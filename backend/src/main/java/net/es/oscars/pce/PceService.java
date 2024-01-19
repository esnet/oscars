package net.es.oscars.pce;

import net.es.oscars.app.exc.PCEException;
import net.es.oscars.pce.beans.PathConstraint;
import net.es.oscars.resv.ent.VlanJunction;
import net.es.oscars.resv.ent.VlanPipe;
import net.es.oscars.resv.svc.ResvService;
import net.es.oscars.topo.enums.UrnType;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.oscars.web.beans.PceRequest;
import net.es.oscars.web.beans.PceResponse;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PceService {
    private final ResvService resvService;
    private final Engine pceEngine;
    final TopologyStore topologyStore;

    public PceService(ResvService resvService, Engine pceEngine, TopologyStore topologyStore) {
        this.resvService = resvService;
        this.pceEngine = pceEngine;
        this.topologyStore = topologyStore;
    }


    public PceResponse calculatePaths(PceRequest request) throws PCEException {
        if (request.getA().equals(request.getZ())) {
            throw new PCEException("invalid path request: A is the same as Z "+request.getA());
        }

        VlanJunction aj = VlanJunction.builder()
                .refId(request.getA())
                .deviceUrn(request.getA())
                .build();
        VlanJunction zj = VlanJunction.builder()
                .refId(request.getZ())
                .deviceUrn(request.getZ())
                .build();

        VlanPipe bwPipe = VlanPipe.builder()
                .a(aj)
                .z(zj)
                .protect(false)
                .azBandwidth(request.getAzBw())
                .zaBandwidth(request.getZaBw()).build();

//        DevelUtils.dumpDebug("baseline", topoService.getBaseline());

        Map<String, Integer> availIngressBw = resvService.availableIngBws(request.getInterval());
        Map<String, Integer> availEgressBw = resvService.availableEgBws(request.getInterval());

        // at this point we have:
        // - the baseline topology
        // - the currently available topology
        // - the available bandwidth over the requested time interval everywhere on the network

        PathConstraint constraint = PathConstraint.builder()
                .ero(request.getInclude())
                .exclude(request.getExclude())
                .build();
//        DevelUtils.dumpDebug("constraint", constraint);

        validateConstraints(constraint, bwPipe);

        return pceEngine.calculatePaths(bwPipe, availIngressBw, availEgressBw, constraint);
    }

    private void validateConstraints(PathConstraint constraint, VlanPipe vlanPipe) throws PCEException {
        Set<String> aAndZ = new HashSet<>();
        aAndZ.add(vlanPipe.getA().getDeviceUrn());
        aAndZ.add(vlanPipe.getZ().getDeviceUrn());
        for (String aOrZ : aAndZ) {
            if (!topologyStore.getTopoUrnMap().containsKey(aOrZ)) {
                throw new PCEException(aOrZ+ " not found in topology");
            } else if (!topologyStore.getTopoUrnMap().get(aOrZ).getUrnType().equals(UrnType.DEVICE)) {
                throw new PCEException(aOrZ+ " is not a device");
            }
        }
        if (constraint.getEro().size() < 2) {
            throw new PCEException("ERO too short; minimum size is 2");
        }

        if (!constraint.getEro().get(0).equals(vlanPipe.getA().getDeviceUrn())) {
            throw new PCEException("ERO first hop must match vlan pipe A");
        }
        if (!constraint.getEro().get(constraint.getEro().size()-1).equals(vlanPipe.getZ().getDeviceUrn())) {
            throw new PCEException("ERO last hop must match vlan pipe Z");
        }
        for (String hop : constraint.getEro()) {
            if (!topologyStore.getTopoUrnMap().containsKey(hop)) {
                throw new PCEException("ERO hop " + hop + " not found in topology");
            } else {
                UrnType urnType = topologyStore.getTopoUrnMap().get(hop).getUrnType();
                if (!urnType.equals(UrnType.DEVICE) && !urnType.equals(UrnType.PORT)) {
                    throw new PCEException("ERO hop " + hop + " must be a device or port");
                }
            }
        }
    }



}
