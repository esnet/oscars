package net.es.oscars.web.rest.v2;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.svc.ResvService;
import net.es.oscars.topo.beans.Device;
import net.es.oscars.topo.beans.PortBwVlan;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.beans.v2.BackbonePort;
import net.es.oscars.topo.beans.v2.Bandwidth;
import net.es.oscars.topo.beans.v2.EdgePort;
import net.es.oscars.topo.beans.v2.VlanAvailability;
import net.es.oscars.topo.pop.ConsistencyException;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.oscars.web.beans.Interval;
import net.es.oscars.web.beans.v2.PortSearchRequest;
import net.es.oscars.web.beans.v2.ConnectionEdgePortRequest;
import net.es.topo.common.devel.DevelUtils;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@Slf4j
public class TopoSearchController {
    private final TopologyStore topologyStore;
    private final Startup startup;
    private final ResvService resvService;
    private final ConnectionRepository connRepo;

    public TopoSearchController(TopologyStore topologyStore, Startup startup, ResvService resvService, ConnectionRepository connRepo) {
        this.topologyStore = topologyStore;
        this.startup = startup;
        this.resvService = resvService;
        this.connRepo = connRepo;
    }

    @ExceptionHandler(ConsistencyException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleException(ConsistencyException ex) {
        log.warn("consistency error " + ex.getMessage());
    }

    @ExceptionHandler(StartupException.class)
    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    public void handleException() {
        log.warn("Still in startup");
    }

    @ExceptionHandler(SearchException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public String handleException(SearchException ex) {
        return ex.getMessage();
    }

    public static class SearchException extends Exception {
        public SearchException(String msg) {
            super(msg);
        }
    }

    @RequestMapping(value = "/api/topo/edge-port-search", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public List<EdgePort> edgePortSearch(@RequestBody PortSearchRequest psr)
            throws ConsistencyException, StartupException, SearchException {
        startup.startupCheck();
        Topology topology = topologyStore.getCurrentTopology();

        boolean blankTerm = psr.getTerm() == null || psr.getTerm().isBlank();
        boolean blankDevice = psr.getDevice() == null || psr.getDevice().isBlank();

        if (!blankTerm) {
            if (psr.getTerm().length() < 4) {
                throw new SearchException("search term too short");
            }
        }

        if (blankTerm && blankDevice) {
            throw new SearchException("must include device id or a text search term");
        }

        if (psr.getInterval() == null) {
            throw new SearchException("null interval");
        } else if (psr.getInterval().getBeginning() == null) {
            throw new SearchException("null interval beginning");
        } else if (psr.getInterval().getEnding() == null) {
            throw new SearchException("null interval ending");
        }


        Map<String, PortBwVlan> available = resvService.available(psr.getInterval(), psr.getConnectionId());
        Map<String, Map<Integer, Set<String>>> vlanUsageMap = resvService.vlanUsage(psr.getInterval(), psr.getConnectionId());

        String term = null;
        if (!blankTerm) {
            term = psr.getTerm().toUpperCase();
        }

        List<EdgePort> results = new ArrayList<>();

        for (Device d : topology.getDevices().values()) {
            if (!blankDevice && !psr.getDevice().equals(d.getUrn())) {
                // user specified a device and this is not the specified device, so skip
                continue;
            }

            for (net.es.oscars.topo.beans.Port p : d.getPorts()) {
                if (!p.isEdge()) {
                    continue;
                }
                boolean isResult = false;
                if (blankTerm) {
                    isResult = true;
                } else {
                    if (p.getUrn().toUpperCase().contains(term)) {
                        isResult = true;
                    } else {
                        for (String tag : p.getTags()) {
                            if (tag.toUpperCase().contains(term)) {
                                isResult = true;
                                break;
                            }
                        }
                    }
                }

                if (isResult) {
                    results.add(mapEdgePort(p, available, vlanUsageMap));
                }
            }
        }

        return results;
    }


    @RequestMapping(value = "/api/topo/device", method = RequestMethod.GET)
    @ResponseBody
    @Transactional
    public Set<String> devices() throws ConsistencyException, StartupException {
        startup.startupCheck();
        Topology topology = topologyStore.getCurrentTopology();
        return topology.getDevices().keySet();
    }


    @RequestMapping(value = "/api/topo/backbone-port/{device:.+}", method = RequestMethod.GET)
    @ResponseBody
    @Transactional
    public List<BackbonePort> backbonePorts(@PathVariable String device)
            throws ConsistencyException, StartupException, SearchException {
        startup.startupCheck();
        Topology topology = topologyStore.getCurrentTopology();

        if (!topology.getDevices().containsKey(device)) {
            throw new SearchException("device not found");
        }

        List<BackbonePort> results = new ArrayList<>();

        for (net.es.oscars.topo.beans.Port p : topology.getDevices().get(device).getPorts()) {
            if (p.isEdge()) {
                continue;
            }
            log.info(p.getUrn());
            results.add(mapBackbonePort(p));
        }

        return results;
    }

    @RequestMapping(value = "/api/topo/connection/edge-port", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public List<EdgePort> connectionEdgePorts(@RequestBody ConnectionEdgePortRequest cepr) throws SearchException, StartupException, ConsistencyException {
        startup.startupCheck();
        Topology topology = topologyStore.getCurrentTopology();

        boolean blankConnectionId = cepr.getConnectionId() == null || cepr.getConnectionId().isBlank();
        if (blankConnectionId) {
            throw new SearchException("must include connection id");

        }

        Set<String> connectionEdgePorts = new HashSet<>();
        Interval interval = cepr.getInterval();
        Optional<Connection> maybeC = connRepo.findByConnectionId(cepr.getConnectionId());

        if (maybeC.isPresent()) {
            Connection c = maybeC.get();
            if (c.getArchived() != null) {
                c.getArchived().getCmp().getFixtures().forEach(f -> {
                    connectionEdgePorts.add(f.getPortUrn());
                });
                if (interval == null) {
                    interval = Interval.builder()
                            .beginning(c.getArchived().getSchedule().getBeginning())
                            .ending(c.getArchived().getSchedule().getEnding())
                            .build();
                }
            }
        }
        if (interval == null) {
            throw new SearchException("unable to determine interval from input or connection id");
        }


        Map<String, PortBwVlan> available = resvService.available(interval, cepr.getConnectionId());
        Map<String, Map<Integer, Set<String>>> vlanUsageMap = resvService.vlanUsage(interval, cepr.getConnectionId());
        List<EdgePort> results = new ArrayList<>();
        for (Device d : topology.getDevices().values()) {
            for (net.es.oscars.topo.beans.Port p : d.getPorts()) {
                if (connectionEdgePorts.contains(p.getUrn())) {
                    results.add(mapEdgePort(p, available, vlanUsageMap));
                }
            }
        }
        return results;

    }


    @RequestMapping(value = "/api/reports/utilization", method = RequestMethod.GET)
    @ResponseBody
    @Transactional
    public List<EdgePort> utilizationReport()
            throws ConsistencyException, StartupException {
        startup.startupCheck();
        List<EdgePort> results = new ArrayList<>();

        Topology topology = topologyStore.getTopology();
        if (topology.getVersion() == null) {
            throw new ConsistencyException("null current topology");
        }
        Interval interval = Interval.builder()
                        .beginning(Instant.now())
                        .ending(Instant.now().plus(24, ChronoUnit.HOURS))
                        .build();
        Map<String, PortBwVlan> available = resvService.available(interval, null);
        Map<String, Map<Integer, Set<String>>> vlanUsageMap = resvService.vlanUsage(interval, null);


        for (Device d : topology.getDevices().values()) {
            for (net.es.oscars.topo.beans.Port p : d.getPorts()) {
                results.add(mapEdgePort(p, available, vlanUsageMap));
            }
        }
        return results;
    }


    private BackbonePort mapBackbonePort(net.es.oscars.topo.beans.Port p) throws ConsistencyException {

        String[] parts = p.getUrn().split(":");
        if (parts.length != 2) {
            throw new ConsistencyException("Invalid port URN format");
        }

        // get the least of ingress / egress available
        int bwPhysical = p.getReservableIngressBw();
        if (p.getReservableEgressBw() < bwPhysical) {
            bwPhysical = p.getReservableEgressBw();
        }

        Bandwidth bw = Bandwidth.builder()
                .unit(Bandwidth.Unit.MBPS)
                .physical(bwPhysical)
                .build();

        return BackbonePort.builder()
                .device(parts[0])
                .name(parts[1])
                .bandwidth(bw)
                .description(p.getTags())
                .esdbEquipmentInterfaceId(p.getEsdbEquipmentInterfaceId())
                .build();
    }

    private EdgePort mapEdgePort(net.es.oscars.topo.beans.Port p,
                                 Map<String, PortBwVlan> available,
                                 Map<String, Map<Integer, Set<String>>> vlanUsageMap) throws ConsistencyException {

        String[] parts = p.getUrn().split(":");
        if (parts.length != 2) {
            throw new ConsistencyException("Invalid port URN format");
        }

        if (!available.containsKey(p.getUrn())) {
            throw new ConsistencyException("cannot get available bw and vlans for " + p.getUrn());
        }
        PortBwVlan pbw = available.get(p.getUrn());

        VlanAvailability vlanAvailability = VlanAvailability.builder()
                .ranges(pbw.getVlanRanges())
                .build();

        // get the least of ingress / egress available
        int bwPhysical = p.getReservableIngressBw();
        if (p.getReservableEgressBw() < bwPhysical) {
            bwPhysical = p.getReservableEgressBw();
        }
        int bwAvailable = pbw.getIngressBandwidth();
        if (pbw.getEgressBandwidth() < bwAvailable) {
            bwAvailable = pbw.getEgressBandwidth();
        }

        Bandwidth bw = Bandwidth.builder()
                .unit(Bandwidth.Unit.MBPS)
                .available(bwAvailable)
                .physical(bwPhysical)
                .build();

        Map<Integer, Set<String>> vlanUsage = new HashMap<>();
        if (vlanUsageMap.containsKey(p.getUrn())) {
            vlanUsage = vlanUsageMap.get(p.getUrn());
        }

        return EdgePort.builder()
                .device(parts[0])
                .name(parts[1])
                .bandwidth(bw)
                .availability(EdgePort.Availability.builder().vlan(vlanAvailability).build())
                .description(p.getTags())
                .esdbEquipmentInterfaceId(p.getEsdbEquipmentInterfaceId())
                .usage(EdgePort.Usage.builder().vlan(vlanUsage).build())
                .build();
    }

}
