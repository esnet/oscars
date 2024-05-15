package net.es.oscars.web.rest.v2;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.resv.svc.ResvService;
import net.es.oscars.topo.beans.Device;
import net.es.oscars.topo.beans.PortBwVlan;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.beans.v2.Bandwidth;
import net.es.oscars.topo.beans.v2.EdgePort;
import net.es.oscars.topo.beans.v2.VlanAvailability;
import net.es.oscars.topo.enums.Layer;
import net.es.oscars.topo.pop.ConsistencyException;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.oscars.web.beans.v2.PortSearchRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Slf4j
public class TopoSearchController {
    private final TopologyStore topologyStore;
    private final Startup startup;
    private final ResvService resvService;

    public TopoSearchController(TopologyStore topologyStore, Startup startup, ResvService resvService) {
        this.topologyStore = topologyStore;
        this.startup = startup;
        this.resvService = resvService;
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

        boolean blankTerm = psr.getTerm() == null || psr.getTerm().isBlank();
        boolean blankDevice = psr.getDevice() == null || psr.getDevice().isBlank();
        if (!blankTerm) {
            if (psr.getTerm().length() < 4) {
                throw new SearchException("search term too short");
            }
        }
        if (blankTerm && blankDevice) {
            throw new SearchException("must include device or search term");
        }

        if (psr.getInterval() == null) {
            throw new SearchException("null interval");
        } else if (psr.getInterval().getBeginning() == null) {
            throw new SearchException("null interval beginning");
        } else if (psr.getInterval().getEnding() == null) {
            throw new SearchException("null interval ending");
        }

        Topology topology = topologyStore.getTopology();
        if (topology.getVersion() == null) {
            throw new ConsistencyException("null current topology");
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
                // not the specified device, skip
                continue;
            }

            for (net.es.oscars.topo.beans.Port p : d.getPorts()) {
                boolean isEdge = true;
                for (Layer l : p.getCapabilities()) {
                    log.info(p.getUrn()+ " " +l.toString());
                }
                if (!p.getCapabilities().contains(Layer.ETHERNET)) {
                    isEdge = false;
                }
                if (!p.getCapabilities().contains(Layer.EDGE)) {
                    isEdge = false;
                }
                if (!isEdge) {
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
                    results.add(fromOldPort(p, available, vlanUsageMap));
                }
            }
        }

        return results;
    }

    private EdgePort fromOldPort(net.es.oscars.topo.beans.Port p,
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
                .vlanAvailability(vlanAvailability)
                .description(p.getTags())
                .esdbEquipmentInterfaceId(p.getEsdbEquipmentInterfaceId())
                .vlanUsage(vlanUsage)
                .build();
    }

}
