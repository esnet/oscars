package net.es.oscars.pce;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PCEException;
import net.es.oscars.pce.beans.PathConstraint;
import net.es.oscars.pce.beans.PipeSpecification;
import net.es.oscars.resv.beans.PeriodBandwidth;
import net.es.oscars.resv.ent.EroHop;
import net.es.oscars.resv.ent.VlanJunction;
import net.es.oscars.resv.ent.VlanPipe;
import net.es.oscars.resv.enums.BwDirection;
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
    final TopologyStore topologyStore;

    public PceService(ResvService resvService, Engine pceEngine, TopologyStore topologyStore) {
        this.resvService = resvService;
        this.pceEngine = pceEngine;
        this.topologyStore = topologyStore;
    }


    public AllPathsPceResponse calculateAllPaths(AllPathsPceRequest request) throws PCEException {
        // get the baseline topology, then remove all bandwidth that is reserved by other reservations
        // over the specified interval

        Map<String, TopoUrn> baseline = topologyStore.getTopoUrnMap();
        Map<String, List<PeriodBandwidth>> reservedIngBws = resvService.reservedIngBws(request.getInterval(), request.getConnectionId());
        Map<String, List<PeriodBandwidth>> reservedEgBws = resvService.reservedEgBws(request.getInterval(), request.getConnectionId());
        Map<String, Integer> availIngressBw = ResvLibrary.availableBandwidthMap(BwDirection.INGRESS, baseline, reservedIngBws);
        Map<String, Integer> availEgressBw = ResvLibrary.availableBandwidthMap(BwDirection.EGRESS, baseline, reservedEgBws);


        AllPathsPceResponse response = AllPathsPceResponse.builder()
                .interval(request.getInterval())
                .connectionId(request.getConnectionId())
                .bandwidth(request.getBandwidth())
                .pipes(new ArrayList<>())
                .build();

        for (PipeSpecification pipe : request.getPipes()) {

            log.info("calculating paths for pipe: " + pipe.getId());
            if (pipe.getA().equals(pipe.getZ())) {
                throw new PCEException("invalid path request: A is the same as Z "+pipe.getA());
            }
            if (pipe.getConstraints().getInclude() == null) {
                pipe.getConstraints().setInclude(new ArrayList<>());
            }
            if (pipe.getConstraints().getExclude() == null) {
                pipe.getConstraints().setExclude(new HashSet<>());
            }

            VlanJunction aj = VlanJunction.builder()
                    .refId(pipe.getA())
                    .deviceUrn(pipe.getA())
                    .build();
            VlanJunction zj = VlanJunction.builder()
                    .refId(pipe.getZ())
                    .deviceUrn(pipe.getZ())
                    .build();

            VlanPipe bwPipe = VlanPipe.builder()
                    .a(aj)
                    .z(zj)
                    .protect(false)
                    .azBandwidth(request.getBandwidth())
                    .zaBandwidth(request.getBandwidth()).build();
            PathConstraint constraint = PathConstraint.builder()
                    .ero(pipe.getConstraints().getInclude())
                    .exclude(pipe.getConstraints().getExclude())
                    .build();
            validateConstraints(constraint, bwPipe);

            // calculate path for this pipe...
            PceResponse pceResponse = pceEngine.calculatePaths(bwPipe, availIngressBw, availEgressBw, constraint);
            pipe.setLsps(new ArrayList<>());

            List<PipeSpecification.LspHop> primaryPath = new ArrayList<>();

            PcePath path = pceResponse.getShortest();
            for (EroHop hop : path.getAzEro()) {
                // add the hop to our pipe path,
                UrnType ut = baseline.get(hop.getUrn()).getUrnType();
                String type = "ROUTER";
                switch (ut) {
                    case PORT -> { type = "PORT"; }
                    case DEVICE -> { type = "ROUTER"; }
                }
                primaryPath.add(PipeSpecification.LspHop.builder()
                                .urn(hop.getUrn())
                                .type(type)
                                .build());

                // remove the bandwidth from the topology before we evaluate the next pipe
                if (availIngressBw.containsKey(hop.getUrn())) {
                    availIngressBw.put(hop.getUrn(), availIngressBw.get(hop.getUrn()) - request.getBandwidth());
                }
                if (availEgressBw.containsKey(hop.getUrn())) {
                    availEgressBw.put(hop.getUrn(), availEgressBw.get(hop.getUrn()) - request.getBandwidth());
                }
            }
            PipeSpecification.PipeLSP primary = PipeSpecification.PipeLSP.builder()
                    .path(primaryPath)
                    .role(PipeSpecification.Role.WORKING)
                    .priority(PipeSpecification.Priority.PRIMARY)
                    .build();
            pipe.getLsps().add(primary);
            List<PipeSpecification.LspHop> protectPath = new ArrayList<>();
            protectPath.add(PipeSpecification.LspHop.builder().type("DEVICE").urn(pipe.getA()).build());
            protectPath.add(PipeSpecification.LspHop.builder().type("DEVICE").urn(pipe.getZ()).build());

            PipeSpecification.PipeLSP protect = PipeSpecification.PipeLSP.builder()
                    .path(protectPath)
                    .role(PipeSpecification.Role.PROTECT)
                    .priority(PipeSpecification.Priority.SECONDARY)
                    .build();
            pipe.getLsps().add(protect);

            response.getPipes().add(pipe);
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
