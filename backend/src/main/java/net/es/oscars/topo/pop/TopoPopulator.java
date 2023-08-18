package net.es.oscars.topo.pop;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.TopoProperties;
import net.es.oscars.topo.beans.TopoException;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.db.AdjcyRepository;
import net.es.oscars.topo.db.DeviceRepository;
import net.es.oscars.topo.db.PortRepository;
import net.es.oscars.topo.ent.Device;
import net.es.oscars.topo.ent.Port;
import net.es.oscars.topo.ent.Adjcy;
import net.es.oscars.topo.ent.Version;
import net.es.oscars.topo.svc.ConsistencyService;
import net.es.oscars.topo.svc.TopoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
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

    @Transactional
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


    public void refresh(boolean onlyLoadWhenFileNewer) throws ConsistencyException, TopoException, IOException {
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
            Topology incoming = loadFromDefaultFiles();
            replaceDbTopology(incoming);
            topoService.bumpVersion();
            // load to memory from DB
            topoService.updateInMemoryTopo();
            // check consistency
            consistencySvc.checkConsistency();
        }

    }


}
