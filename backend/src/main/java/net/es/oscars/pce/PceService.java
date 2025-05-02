package net.es.oscars.pce;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PCEException;
import net.es.oscars.model.Bundle;
import net.es.oscars.model.LSP;
import net.es.oscars.model.enums.Role;
import net.es.oscars.pce.beans.PathConstraint;
import net.es.oscars.resv.beans.PeriodBandwidth;
import net.es.oscars.resv.ent.EroHop;
import net.es.oscars.resv.ent.VlanJunction;
import net.es.oscars.resv.ent.VlanPipe;
import net.es.oscars.resv.enums.BwDirection;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.resv.svc.ResvLibrary;
import net.es.oscars.resv.svc.ResvService;
import net.es.oscars.topo.beans.TopoUrn;
import net.es.oscars.topo.enums.UrnType;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.oscars.web.beans.PcePath;
import net.es.oscars.web.beans.PceRequest;
import net.es.oscars.web.beans.PceResponse;
import net.es.oscars.web.beans.v2.AllPathsPceRequest;
import net.es.oscars.web.beans.v2.AllPathsPceResponse;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class PceService {
    private final ResvService resvService;
    private final Engine pceEngine;
    private final TopologyStore topologyStore;
    private final ConnService connService;

    public PceService(ResvService resvService, Engine pceEngine, TopologyStore topologyStore, ConnService connService) {
        this.resvService = resvService;
        this.pceEngine = pceEngine;
        this.topologyStore = topologyStore;
        this.connService = connService;
    }


    public AllPathsPceResponse calculateAllPaths(AllPathsPceRequest request) throws PCEException {
        // get the baseline topology, then remove all bandwidth reserved by other reservations
        // over the specified interval

        Map<String, TopoUrn> baseline = topologyStore.getTopoUrnMap();
        Map<String, List<PeriodBandwidth>> reservedIngBws = resvService.reservedIngBws(request.getInterval(), connService.getHeld(), request.getConnectionId());
        Map<String, List<PeriodBandwidth>> reservedEgBws = resvService.reservedEgBws(request.getInterval(), connService.getHeld(), request.getConnectionId());
        Map<String, Integer> availIngressBw = ResvLibrary.availableBandwidthMap(BwDirection.INGRESS, baseline, reservedIngBws);
        Map<String, Integer> availEgressBw = ResvLibrary.availableBandwidthMap(BwDirection.EGRESS, baseline, reservedEgBws);


        AllPathsPceResponse response = AllPathsPceResponse.builder()
                .interval(request.getInterval())
                .connectionId(request.getConnectionId())
                .bandwidth(request.getBandwidth())
                .bundles(new ArrayList<>())
                .build();

        for (Bundle bundle : request.getBundles()) {


            log.info("calculating paths for bundle: {}", bundle.getName());
            if (bundle.getA().equals(bundle.getZ())) {
                throw new PCEException("invalid path request: A is the same as Z "+ bundle.getA());
            }
            if (bundle.getConstraints().getInclude() == null) {
                bundle.getConstraints().setInclude(new ArrayList<>());
            }
            if (bundle.getConstraints().getExclude() == null) {
                bundle.getConstraints().setExclude(new HashSet<>());
            }

            VlanJunction aj = VlanJunction.builder()
                    .refId(bundle.getA())
                    .deviceUrn(bundle.getA())
                    .build();
            VlanJunction zj = VlanJunction.builder()
                    .refId(bundle.getZ())
                    .deviceUrn(bundle.getZ())
                    .build();

            VlanPipe bwPipe = VlanPipe.builder()
                    .a(aj)
                    .z(zj)
                    .protect(false)
                    .azBandwidth(request.getBandwidth())
                    .zaBandwidth(request.getBandwidth()).build();
            PathConstraint constraint = PathConstraint.builder()
                    .ero(bundle.getConstraints().getInclude())
                    .exclude(bundle.getConstraints().getExclude())
                    .build();
            validateConstraints(constraint, bwPipe);

            // calculate path for this pipe...
            PceResponse pceResponse = pceEngine.calculatePaths(bwPipe, availIngressBw, availEgressBw, constraint);

            List<String> primaryPath = new ArrayList<>();

            PcePath path = pceResponse.getShortest();

            if (path == null) {
                throw new PCEException("unable to find path for pipe "+ bundle.getName());
            }

            Bundle responseBundle = Bundle.builder()
                    .name(bundle.getName())
                    .lsps(new ArrayList<>())
                    .a(bundle.getA())
                    .z(bundle.getZ())
                    .constraints(bundle.getConstraints())
                    .build();

            for (EroHop hop : path.getAzEro()) {
                primaryPath.add(hop.getUrn());

                // remove the bandwidth from the topology before we do pathfinding for the next bundle
                if (availIngressBw.containsKey(hop.getUrn())) {
                    availIngressBw.put(hop.getUrn(), availIngressBw.get(hop.getUrn()) - request.getBandwidth());
                }
                if (availEgressBw.containsKey(hop.getUrn())) {
                    availEgressBw.put(hop.getUrn(), availEgressBw.get(hop.getUrn()) - request.getBandwidth());
                }
            }

            LSP primary = LSP.builder()
                    .path(primaryPath)
                    .role(Role.PRIMARY)
                    .build();
            responseBundle.getLsps().add(primary);

            // make a protect LSP
            List<String> protectPath = new ArrayList<>();
            protectPath.add(bundle.getA());
            protectPath.add(bundle.getZ());

            LSP protect = LSP.builder()
                    .path(protectPath)
                    .role(Role.PROTECT)
                    .build();
            responseBundle.getLsps().add(protect);

            response.getBundles().add(responseBundle);
        }


        return response;
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
