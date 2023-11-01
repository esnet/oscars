package net.es.oscars.topo.pop;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.TopoProperties;
import net.es.oscars.dto.topo.DeviceModel;
import net.es.oscars.topo.beans.IntRange;
import net.es.oscars.topo.beans.TopoException;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.db.AdjcyRepository;
import net.es.oscars.topo.db.DeviceRepository;
import net.es.oscars.topo.db.PortRepository;
import net.es.oscars.topo.ent.*;
import net.es.oscars.topo.enums.DeviceType;
import net.es.oscars.topo.enums.Layer;
import net.es.oscars.topo.svc.ConsistencyService;
import net.es.oscars.topo.svc.TopoService;
import net.es.topo.common.model.oscars1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;


@Slf4j
@Service
public class TopoPopulator {
    private TopoProperties topoProperties;
    private TopoService topoService;

    private DeviceRepository deviceRepo;
    private PortRepository portRepo;
    private AdjcyRepository adjcyRepo;

    private ConsistencyService consistencySvc;


    @Autowired
    public TopoPopulator(TopoService topoService,
                         DeviceRepository deviceRepo,
                         PortRepository portRepo,
                         AdjcyRepository adjcyRepo,
                         ConsistencyService consistencySvc,
                         TopoProperties topoProperties) {
        this.topoProperties = topoProperties;
        this.deviceRepo = deviceRepo;
        this.portRepo = portRepo;
        this.adjcyRepo = adjcyRepo;
        this.topoService = topoService;
        this.consistencySvc = consistencySvc;
    }

    public boolean fileLoadNeeded(Version version) {
        return version.getUpdated().isBefore(fileLastModified());

    }

    public Instant fileLastModified() {
        String devicesFilename = "./config/topo/" + topoProperties.getPrefix() + "-devices.json";
        File devFile = new File(devicesFilename);
        Instant devLastMod = Instant.ofEpochMilli(devFile.lastModified());

        String adjciesFilename = "./config/topo/" + topoProperties.getPrefix() + "-adjcies.json";
        File adjFile = new File(adjciesFilename);
        Instant adjLastMod = Instant.ofEpochMilli(adjFile.lastModified());

        Instant latest = devLastMod;
        if (adjLastMod.isAfter(devLastMod)) {
            latest = adjLastMod;
        }
        return latest;
    }

    public Topology loadFromDefaultFiles() throws ConsistencyException, IOException {
        log.info("loading topology DB from files");
        if (topoProperties == null) {
            throw new ConsistencyException("Could not load topology properties!");
        }
        String devicesFilename = "./config/topo/" + topoProperties.getPrefix() + "-devices.json";
        String adjciesFilename = "./config/topo/" + topoProperties.getPrefix() + "-adjcies.json";

        Topology current = topoService.currentTopology();
        log.debug("Existing topology: dev: " + current.getDevices().size() + " adj: " + current.getAdjcies().size());
        return this.loadTopology(devicesFilename, adjciesFilename);
    }

    public void replaceDbTopology(Topology incoming) {
        deviceRepo.deleteAll();
        portRepo.deleteAll();
        adjcyRepo.deleteAll();
        deviceRepo.saveAll(incoming.getDevices().values());
        portRepo.saveAll(incoming.getPorts().values());
        adjcyRepo.saveAll(incoming.getAdjcies());

    }

    public Topology loadTopology(String devicesFilename, String adjciesFilename) throws IOException {

        List<Device> devices = loadDevicesFromFile(devicesFilename);
        Map<String, Port> portMap = new HashMap<>();
        Map<String, Device> deviceMap = new HashMap<>();
        log.debug("Loaded topology from " + devicesFilename + " , " + adjciesFilename);
        devices.forEach(d -> {
            deviceMap.put(d.getUrn(), d);
            // log.info("  d: "+d.getUrn());
            d.getPorts().forEach(p -> {
                // log.info("  +- "+p.getUrn());
                portMap.put(p.getUrn(), p);
            });
        });

        List<Adjcy> adjcies = loadAdjciesFromFile(adjciesFilename, portMap);

        return Topology.builder()
                .adjcies(adjcies)
                .devices(deviceMap)
                .ports(portMap)
                .build();

    }


    private List<Device> loadDevicesFromFile(String filename) throws IOException {
        File jsonFile = new File(filename);
        ObjectMapper mapper = new ObjectMapper();
        List<Device> devices = Arrays.asList(mapper.readValue(jsonFile, Device[].class));
        for (Device d : devices) {
            for (Port p : d.getPorts()) {
                p.setDevice(d);
            }
        }
        return devices;
    }

    private List<Adjcy> loadAdjciesFromFile(String filename, Map<String, Port> portMap) throws IOException {
        File jsonFile = new File(filename);
        ObjectMapper mapper = new ObjectMapper();
        List<Adjcy> fromFile = Arrays.asList(mapper.readValue(jsonFile, Adjcy[].class));

        List<Adjcy> filtered = new ArrayList<>();

        fromFile.forEach(t -> {
            String aPortUrn = t.getA().getPortUrn();
            String zPortUrn = t.getZ().getPortUrn();
            boolean add = true;
            if (!portMap.containsKey(aPortUrn)) {
                log.error("  " + aPortUrn + " not in topology");
                add = false;
            }
            if (!portMap.containsKey(zPortUrn)) {
                log.error("  " + zPortUrn + " not in topology");
                add = false;
            }

            if (add) {
                filtered.add(t);

            } else {
                log.error("Could not load an adjacency: " + t.getUrn());
            }
        });
        return filtered;

    }


    @Transactional
    public void refresh(boolean onlyLoadWhenFileNewer) throws ConsistencyException, TopoException, IOException {
        Topology incoming = null;
        boolean updated = false;
        if (topoProperties.getUrl() != null) {
            try {
                incoming = this.loadFromDiscovery();
                updated = true;

            } catch (ResourceAccessException ex) {
                // typically an unknown host error
                log.info(ex.getMessage());

            } catch (TopoException ex) {
                log.error(ex.getMessage());

            }
        } else {
            Optional<Version> maybeV = topoService.latestVersion();
            boolean fileLoadNeeded = true;

            if (maybeV.isPresent()) {
                if (onlyLoadWhenFileNewer) {
                    fileLoadNeeded = fileLoadNeeded(maybeV.get());
                }
            } else {
                log.info("no topology valid version present; first load?");

            }

            if (fileLoadNeeded) {
                log.info("Need to load new topology files");
                // load to DB from disk
                incoming = loadFromDefaultFiles();
                updated = true;
            }
        }

        if (updated) {
            replaceDbTopology(incoming);
            topoService.bumpVersion();
            // load to memory from DB
            topoService.updateInMemoryTopo();
            // check consistency
            consistencySvc.checkConsistency();
        }
    }

    public Topology loadFromDiscovery() throws TopoException, ResourceAccessException {
        RestTemplate restTemplate = new RestTemplate();
        OscarsOneTopo discTopo = restTemplate.getForObject(topoProperties.getUrl(), OscarsOneTopo.class);
        if (discTopo == null) {
            log.warn("null discovery topology");
            return Topology.builder()
                    .adjcies(new ArrayList<>())
                    .ports(new HashMap<>())
                    .devices(new HashMap<>())
                    .build();

        } else if (discTopo.getAdjcies() == null) {
            throw new TopoException("null discovery topology adjacencies");
        } else if (discTopo.getDevices() == null) {
            throw new TopoException("null discovery topology devices");
        }
        List<Adjcy> adjcies = new ArrayList<>();
        for (OscarsOneAdjcy discAdjcy : discTopo.getAdjcies()) {
            Point a = Point.builder()
                    .port(discAdjcy.getA().getPort())
                    .device(discAdjcy.getA().getDevice())
                    .ifce(discAdjcy.getA().getIfce())
                    .addr(discAdjcy.getA().getAddr())
                    .build();
            Point z = Point.builder()
                    .port(discAdjcy.getZ().getPort())
                    .device(discAdjcy.getZ().getDevice())
                    .ifce(discAdjcy.getZ().getIfce())
                    .addr(discAdjcy.getZ().getAddr())
                    .build();
            Map<Layer, Long> metrics = new HashMap<>();
            for (String key : discAdjcy.getMetrics().keySet()) {
                metrics.put(Layer.valueOf(key), discAdjcy.getMetrics().get(key).longValue());
            }
            adjcies.add(Adjcy.builder()
                    .a(a)
                    .z(z)
                    .metrics(metrics)
                    .build());
        }
        Map<String, Device> devices = new HashMap<>();
        Map<String, Port> ports = new HashMap<>();

        for (OscarsOneDevice discDevice : discTopo.getDevices()) {
            Set<Layer> deviceCaps = new HashSet<>();
            for (String capString : discDevice.getCapabilities()) {
                deviceCaps.add(Layer.valueOf(capString));
            }
            Set<IntRange> devResVlans = new HashSet<>();
            for (OscarsOneVlan vlan : discDevice.getReservableVlans()) {
                devResVlans.add(IntRange.builder()
                                .ceiling(vlan.getCeiling())
                                .floor(vlan.getFloor())
                                .build());
            }
            Device d = Device.builder()
                    .ports(new HashSet<>())
                    .capabilities(new HashSet<>())
                    .ipv4Address(discDevice.getIpv4Address())
                    .longitude(Double.valueOf(discDevice.getLongitude()))
                    .latitude(Double.valueOf(discDevice.getLatitude()))
                    .location(discDevice.getLocation())
                    .model(DeviceModel.valueOf(discDevice.getModel().toString()))
                    .locationId(discDevice.getLocationId())
                    .type(DeviceType.valueOf(discDevice.getType()))
                    .urn(discDevice.getUrn())
                    .capabilities(deviceCaps)
                    .esdbEquipmentId(discDevice.getEsdbEquipmentId())
                    .reservableVlans(devResVlans)
                    .build();
            devices.put(d.getUrn(), d);
            for (OscarsOnePort discPort : discDevice.getPorts()) {
                Set<Layer> portCaps = new HashSet<>();
                for (String capString : discDevice.getCapabilities()) {
                    portCaps.add(Layer.valueOf(capString));
                }
                Set<IntRange> portResVlans = new HashSet<>();
                for (OscarsOneVlan vlan : discPort.getReservableVlans()) {
                    portResVlans.add(IntRange.builder()
                            .ceiling(vlan.getCeiling())
                            .floor(vlan.getFloor())
                            .build());
                }
                Set<Layer3Ifce> ifces = new HashSet<>();
                for (OscarsOnePort.OscarsOnePortIfce discIfce : discPort.getIfces()) {
                    ifces.add(Layer3Ifce.builder()
                            .port(discIfce.getPort())
                            .ipv4Address(discIfce.getIpv4Address())
                            .urn(discIfce.getUrn())
                            .build());
                }
                Port port = Port.builder()
                        .capabilities(portCaps)
                        .device(d)
                        .esdbEquipmentInterfaceId(discPort.getEsdbEquipmentInterfaceId())
                        .reservableEgressBw(discPort.getReservableEgressBw())
                        .reservableIngressBw(discPort.getReservableIngressBw())
                        .tags(new ArrayList<>(discPort.getTags()))
                        .ifces(ifces)
                        .urn(discPort.getUrn())
                        .reservableVlans(portResVlans)
                        .build();
                d.getPorts().add(port);
                ports.put(port.getUrn(), port);
            }
        }

        return Topology.builder()
                .adjcies(adjcies)
                .ports(ports)
                .devices(devices)
                .build();

    }


}
