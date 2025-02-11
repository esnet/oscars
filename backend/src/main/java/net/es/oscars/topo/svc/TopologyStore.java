package net.es.oscars.topo.svc;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.resv.svc.ResvLibrary;
import net.es.oscars.topo.beans.*;
import net.es.oscars.topo.enums.Layer;
import net.es.oscars.topo.enums.UrnType;
import net.es.oscars.topo.pop.ConsistencyException;
import net.es.topo.common.model.oscars1.IntRange;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class TopologyStore {

    @Getter
    @Setter
    public Topology topology;

    @Getter
    @Setter
    private Map<String, TopoUrn> topoUrnMap;

    @Getter
    @Setter
    private List<TopoAdjcy> topoAdjcies;

    @Getter
    @Setter
    public Version version;

    @Getter
    @Setter
    private Map<String, PortBwVlan> baseline;

    public Version bumpVersion() {
        version = Version.builder().updated(Instant.now()).valid(true).build();
        log.debug("New version date is: " + version.getUpdated());
        return version;
    }

    public Topology getCurrentTopology() throws ConsistencyException {
        if (topology == null || topology.getVersion() == null) {
            throw new ConsistencyException("no current topology");
        }
        return topology;
    }

    public void replaceTopology(Topology incoming) throws TopoException {
        this.clear();
        this.topology = incoming;
        this.update();
        this.topology.setVersion(this.bumpVersion());
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

    public void clear() {
        this.baseline = new HashMap<>();
        this.topoAdjcies = new ArrayList<>();
        this.topoUrnMap = new HashMap<>();
        this.version = null;
        this.topology = null;
    }

    public Optional<Device> findDeviceByUrn(String urn) {
        if (topology != null && topology.getDevices() != null) {
            if (topology.getDevices().containsKey(urn)) {
                return Optional.of(topology.getDevices().get(urn));
            }
        }
        return Optional.empty();
    }

    public Optional<Port> findPortByUrn(String urn) {
        if (topology != null && topology.getPorts() != null) {
            if (topology.getPorts().containsKey(urn)) {
                return Optional.of(topology.getPorts().get(urn));
            }
        }
        return Optional.empty();
    }

    private void update() throws TopoException {
        List<Device> devices = topology.getDevices().values().stream().toList();
        List<Adjcy> adjcies = topology.getAdjcies().stream().toList();

        // first add all devices (and ports) to the urn map
        this.topoUrnMap = this.urnsFromDevices(devices);

        // now process all adjacencies
        this.topoAdjcies = topoAdjciesFromDevices(devices);
        this.topoAdjcies.addAll(topoAdjciesFromDbAdjcies(adjcies));
        this.baseline = ResvLibrary.portBwVlans(this.getTopoUrnMap(), new HashSet<>(), new HashMap<>(), new HashMap<>());

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


}
