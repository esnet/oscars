package net.es.oscars.resv.svc;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PCEException;
import net.es.oscars.model.*;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.enums.DeploymentIntent;
import net.es.oscars.resv.enums.DeploymentState;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.enums.State;
import net.es.oscars.resv.svc.conversions.L2VPNConversions;
import net.es.oscars.sb.nso.resv.NsoResvException;
import net.es.oscars.web.beans.BandwidthAvailabilityResponse;
import net.es.oscars.web.beans.ConnException;
import net.es.oscars.web.beans.ConnectionFilter;
import net.es.oscars.web.beans.ConnectionList;
import net.es.oscars.web.beans.v2.L2VPNList;
import net.es.oscars.web.simple.SimpleConnection;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
@Slf4j
public class L2VPNService {
    private final ConnService connSvc;

    public L2VPNService(ConnService connSvc) {
        this.connSvc = connSvc;
    }
    public L2VPN get(String connectionId) throws ConnException{
        return L2VPNConversions.fromConnection(connSvc.findConnection(connectionId).orElseThrow());
    }

    public L2VPNList list(ConnectionFilter filter) {
        ConnectionList connList = connSvc.filter(filter);
        List<L2VPN> l2vpns = new ArrayList<>();
        for (Connection conn : connList.getConnections()) {
            l2vpns.add(L2VPNConversions.fromConnection(conn));
        }
        return L2VPNList.builder()
                .page(connList.getPage())
                .sizePerPage(connList.getSizePerPage())
                .l2vpns(l2vpns)
                .build();
    }

    public BandwidthAvailabilityResponse bwAvailability(L2VPN l2VPN) throws ConnException {
        // we ignore whatever status we were sent
        l2VPN.setStatus(L2VPN.Status.builder()
                        .deploymentIntent(DeploymentIntent.SHOULD_BE_UNDEPLOYED)
                        .deploymentState(DeploymentState.UNDEPLOYED)
                        .phase(Phase.HELD)
                        .state(State.WAITING)
                .build());
        SimpleConnection simpleConnection = L2VPNConversions.fromL2VPN(l2VPN);
        return connSvc.bwAvailability(simpleConnection);
    }

    public L2VPN create(L2VPN l2VPN) throws ConnException {
        SimpleConnection in = L2VPNConversions.fromL2VPN(l2VPN);
        Pair<SimpleConnection, Connection> holdResult = connSvc.holdConnection(in);
        try {
            connSvc.commit(holdResult.getRight());
        } catch (NsoResvException | PCEException e) {
            throw new ConnException(e.getMessage());
        }
        return L2VPNConversions.fromConnection(connSvc.findConnection(l2VPN.getName()).orElseThrow(
                () -> new ConnException("No connection found for name: " + l2VPN.getName())
        ));
    }


}
