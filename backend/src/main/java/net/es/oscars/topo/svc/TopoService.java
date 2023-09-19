package net.es.oscars.topo.svc;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.PssProperties;
import net.es.oscars.resv.svc.ResvLibrary;
import net.es.oscars.topo.beans.*;
import net.es.oscars.topo.db.DeviceRepository;
import net.es.oscars.topo.db.AdjcyRepository;
import net.es.oscars.topo.db.PortRepository;
import net.es.oscars.topo.db.VersionRepository;
import net.es.oscars.topo.ent.Device;
import net.es.oscars.topo.ent.Port;
import net.es.oscars.topo.ent.Adjcy;
import net.es.oscars.topo.ent.Version;
import net.es.oscars.topo.enums.Layer;
import net.es.oscars.topo.enums.UrnType;
import net.es.oscars.topo.pop.ConsistencyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@Data
public class TopoService {
    private Map<String, TopoUrn> topoUrnMap;
    private List<TopoAdjcy> topoAdjcies;
    private Version current = null;


    @Autowired
    private DeviceRepository deviceRepo;
    @Autowired
    private PortRepository portRepo;
    @Autowired
    private AdjcyRepository adjcyRepo;
    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private PssProperties pssProperties;

    // dumb cache
    private Map<String, PortBwVlan> baseline = new HashMap<>();

    @Transactional
    public void updateInMemoryTopo() throws TopoException {
        List<Device> devices = deviceRepo.findAll();
        List<Adjcy> adjcies = adjcyRepo.findAll();

        // first add all devices (and ports) to the urn map
        this.topoUrnMap = this.urnsFromDevices(devices);

        // now process all adjacencies
        this.topoAdjcies = topoAdjciesFromDevices(devices);
        this.topoAdjcies.addAll(topoAdjciesFromDbAdjcies(adjcies));
        this.baseline = new HashMap<>();

        log.info("topo version " + this.current.getId() + " updated " +
                "(" + devices.size() + " devices, " + this.topoAdjcies.size() + " adjcies)");

    }


    public Optional<Version> latestVersion() throws ConsistencyException {
        List<Version> maybeCurrent = versionRepo.findByValid(true);

        if (maybeCurrent.size() == 1) {
            return Optional.of(maybeCurrent.get(0));
        } else if (maybeCurrent.isEmpty()) {
            return Optional.empty();
        } else {
            throw new ConsistencyException("Multiple valid versions found in db!");
        }

    }

    @Transactional
    public Version bumpVersion() {
        List<Version> maybeCurrent = versionRepo.findByValid(true);

        if (!maybeCurrent.isEmpty()) {
            for (Version noLongerValid : maybeCurrent) {
                noLongerValid.setValid(false);
                log.debug("Setting previous version to invalid: " + noLongerValid.getId());
                versionRepo.save(noLongerValid);
            }
        }

        Version newVersion = Version.builder().updated(Instant.now()).valid(true).build();
        this.current = newVersion;
        versionRepo.save(newVersion);

        log.debug("New version id is: " + newVersion.getId());
        return newVersion;
    }


    public Topology currentTopology() {
        Map<String, Device> deviceMap = new HashMap<>();
        Map<String, Port> portMap = new HashMap<>();

        List<Adjcy> adjcies = new ArrayList<>();
        Topology t = Topology.builder()
                .adjcies(adjcies)
                .devices(deviceMap)
                .ports(portMap)
                .version(current)
                .build();

        List<Device> devices = deviceRepo.findAll();
        devices.forEach(d -> {
            deviceMap.put(d.getUrn(), d);
        });

        adjcies = adjcyRepo.findAll();
//        log.info("found " + devices.size() + " devices ");
//        log.info("found " + adjcies.size() + " adjcies ");
        t.setDevices(deviceMap);
        t.setAdjcies(adjcies);

//            log.info(" current topo:");
        devices.forEach(d -> {
//                log.info(" d: "+d.getUrn());
            for (Port p : d.getPorts()) {
//                        log.info(" +- "+p.getUrn());
                portMap.put(p.getUrn(), p);
            }
        });
        return t;
    }


    public Map<String, PortBwVlan> baseline() {
        if (this.baseline.size() == 0) {
            baseline = ResvLibrary.portBwVlans(this.getTopoUrnMap(), new HashSet<>(), new HashMap<>(), new HashMap<>());
        }
        return this.baseline;
    }

    private Map<String, TopoUrn> urnsFromDevices(List<Device> devices) {
        Map<String, TopoUrn> urns = new HashMap<>();

        devices.forEach(d -> {

            // make a copy of the IntRanges otherwise it'd be set by reference
            Set<IntRange> drv = new HashSet<>(IntRange.mergeIntRanges(d.getReservableVlans()));
            Set<Layer> dCaps = new HashSet<>(d.getCapabilities());

            TopoUrn deviceUrn = TopoUrn.builder()
                    .urn(d.getUrn())
                    .urnType(UrnType.DEVICE)
                    .device(d)
                    .reservableVlans(drv)
                    .capabilities(dCaps)
                    .reservableCommandParams(new HashSet<>())
                    .build();

            urns.put(d.getUrn(), deviceUrn);

            d.getPorts().forEach(p -> {
                // make a copy of the IntRanges otherwise it'd be set by reference
                Set<IntRange> prv = new HashSet<>(IntRange.mergeIntRanges(p.getReservableVlans()));
                Set<Layer> pCaps = new HashSet<>(p.getCapabilities());

                TopoUrn portUrn = TopoUrn.builder()
                        .urn(p.getUrn())
                        .urnType(UrnType.PORT)
                        .capabilities(pCaps)
                        .device(d)
                        .port(p)
                        .reservableIngressBw(p.getReservableIngressBw())
                        .reservableEgressBw(p.getReservableEgressBw())
                        .reservableVlans(prv)
                        .reservableCommandParams(new HashSet<>())
                        .build();


                urns.put(p.getUrn(), portUrn);
            });

        });

        return urns;

    }

    private List<TopoAdjcy> topoAdjciesFromDbAdjcies(List<Adjcy> dbAdjcies) throws TopoException {
        List<TopoAdjcy> adjcies = new ArrayList<>();
        List<Adjcy> filtered = new ArrayList<>();

        for (Adjcy dbAdjcy : dbAdjcies) {
            boolean shouldAdd = true;

            List<String> portUrnsToVerify = new ArrayList<>();
            portUrnsToVerify.add(dbAdjcy.getA().getPortUrn());
            portUrnsToVerify.add(dbAdjcy.getZ().getPortUrn());

            for (String portUrn : portUrnsToVerify) {
                if (!this.topoUrnMap.containsKey(portUrn)) {
                    log.error("port not in topology: " + dbAdjcy.getUrn());
                    shouldAdd = false;
                } else {
                    TopoUrn topoUrn = this.topoUrnMap.get(portUrn);
                    if (!topoUrn.getUrnType().equals(UrnType.PORT)) {
                        log.error("wrong port URN type: " + dbAdjcy.getUrn());
                        shouldAdd = false;
                    }
                }
            }
            if (shouldAdd) {
                filtered.add(dbAdjcy);
            }

        }

        for (Adjcy dbAdjcy : filtered) {
            TopoUrn aUrn = this.topoUrnMap.get(dbAdjcy.getA().getPortUrn());
            TopoUrn zUrn = this.topoUrnMap.get(dbAdjcy.getZ().getPortUrn());
            Map<Layer, Long> metrics = new HashMap<>();

            dbAdjcy.getMetrics().entrySet().forEach(e -> {
                metrics.put(e.getKey(), e.getValue());
            });

            TopoAdjcy adjcy = TopoAdjcy.builder().a(aUrn).z(zUrn).metrics(metrics).build();
            adjcies.add(adjcy);
        }
        return adjcies;

    }

    private List<TopoAdjcy> topoAdjciesFromDevices(List<Device> devices) throws TopoException {
        List<TopoAdjcy> adjcies = new ArrayList<>();
        for (Device d : devices) {
            if (this.topoUrnMap.containsKey(d.getUrn())) {
                TopoUrn deviceUrn = this.topoUrnMap.get(d.getUrn());
                for (Port p : d.getPorts()) {
                    if (this.topoUrnMap.containsKey(p.getUrn())) {
                        TopoUrn portUrn = this.topoUrnMap.get(p.getUrn());
                        TopoAdjcy az = TopoAdjcy.builder()
                                .a(deviceUrn)
                                .z(portUrn)
                                .metrics(new HashMap<>())
                                .build();
                        az.getMetrics().put(Layer.INTERNAL, 1L);
                        TopoAdjcy za = TopoAdjcy.builder()
                                .a(portUrn)
                                .z(deviceUrn)
                                .metrics(new HashMap<>())
                                .build();
                        za.getMetrics().put(Layer.INTERNAL, 1L);
                        adjcies.add(az);
                        adjcies.add(za);
                    } else {
                        throw new TopoException("missing a port urn " + p.getUrn());
                    }
                }
            } else {
                throw new TopoException("missing a device urn " + d.getUrn());
            }
        }

        return adjcies;
    }

    public Integer minimalReservableBandwidth(Adjcy adjcy) {
        Set<Integer> reservableBandwidths = new HashSet<>();
        TopoUrn aPortUrn = topoUrnMap.get(adjcy.getA().getPortUrn());
        TopoUrn zPortUrn = topoUrnMap.get(adjcy.getA().getPortUrn());
        Port aPort = aPortUrn.getPort();
        Port zPort = zPortUrn.getPort();


        reservableBandwidths.add(aPort.getReservableEgressBw());
        reservableBandwidths.add(zPort.getReservableEgressBw());
        reservableBandwidths.add(aPort.getReservableIngressBw());
        reservableBandwidths.add(zPort.getReservableIngressBw());
        // we can get() because the stream is not empty
        return reservableBandwidths.stream().min(Integer::compare).get();
    }
}
