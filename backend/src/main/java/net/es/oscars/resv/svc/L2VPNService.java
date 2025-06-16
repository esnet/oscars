package net.es.oscars.resv.svc;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.model.Bundle;
import net.es.oscars.model.Endpoint;
import net.es.oscars.model.L2VPN;
import net.es.oscars.model.LSP;
import net.es.oscars.model.enums.QosExcessAction;
import net.es.oscars.model.enums.Role;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.enums.State;
import net.es.oscars.web.beans.BandwidthAvailabilityResponse;
import net.es.oscars.web.beans.ConnException;
import net.es.oscars.web.beans.PceMode;
import net.es.oscars.web.simple.Fixture;
import net.es.oscars.web.simple.Junction;
import net.es.oscars.web.simple.Pipe;
import net.es.oscars.web.simple.SimpleConnection;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class L2VPNService {
    private final ConnService connSvc;

    public L2VPNService(ConnService connSvc) {
        this.connSvc = connSvc;
    }

    public BandwidthAvailabilityResponse bwAvailability(L2VPN l2VPNRequest) throws ConnException {
        Integer begin  = Long.valueOf(l2VPNRequest.getSchedule().getBeginning().getEpochSecond()).intValue();
        Integer end  = Long.valueOf(l2VPNRequest.getSchedule().getEnding().getEpochSecond()).intValue();
        Integer heldUntil = Long.valueOf(Instant.now().getEpochSecond() + 60).intValue();

        List<Fixture> fixtures = new ArrayList<>();
        List<Pipe> pipes = new ArrayList<>();
        Set<Junction> junctions = new HashSet<>();

        for (Endpoint endpoint : l2VPNRequest.getEndpoints()) {
            junctions.add(Junction.builder().device(endpoint.getDevice()).build());
            fixtures.add(Fixture.builder()
                    .junction(endpoint.getDevice())
                    .inMbps(l2VPNRequest.getQos().getBandwidth())
                    .outMbps(l2VPNRequest.getQos().getBandwidth())
                    .mbps(l2VPNRequest.getQos().getBandwidth())
                    .port(endpoint.getPort())
                    .vlan(endpoint.getVlan())
                    .strict(l2VPNRequest.getQos().getExcessAction().equals(QosExcessAction.DROP))
                    .build());
        }
        for (Bundle bundle : l2VPNRequest.getBundles()) {
            LSP primary = null;
            for (LSP lsp : bundle.getLsps()) {
                if (lsp.getRole().equals(Role.PRIMARY)) {
                    primary = lsp;
                }
            }
            if (primary != null) {
                List<String> ero = new ArrayList<>();
                if (primary.getPath() != null && !primary.getPath().isEmpty()) {
                    ero = primary.getPath();
                } else if (bundle.getConstraints().getInclude() != null && !bundle.getConstraints().getInclude().isEmpty()) {
                    ero = bundle.getConstraints().getInclude();
                }

                pipes.add(Pipe.builder()
                        .azMbps(l2VPNRequest.getQos().getBandwidth())
                        .zaMbps(l2VPNRequest.getQos().getBandwidth())
                        .mbps(l2VPNRequest.getQos().getBandwidth())
                        .a(bundle.getA())
                        .z(bundle.getZ())
                        .ero(ero)
                        .exclude(new ArrayList<>(bundle.getConstraints().getExclude()))
                        .pceMode(PceMode.BEST)
                        .build());

            }
        }


        SimpleConnection simpleConnection = SimpleConnection.builder()
                .connection_mtu(l2VPNRequest.getTech().getMtu())
                .connectionId(l2VPNRequest.getName())
                .begin(begin)
                .end(end)
                .description(l2VPNRequest.getMeta().getDescription())
                .phase(Phase.HELD)
                .state(State.WAITING)
                .heldUntil(heldUntil)
                .serviceId(l2VPNRequest.getMeta().getTrackingId())
                .fixtures(fixtures)
                .junctions(new ArrayList<>(junctions))
                .pipes(pipes)
                .build();

        return connSvc.bwAvailability(simpleConnection);
    }

}
