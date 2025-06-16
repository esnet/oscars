package net.es.oscars.resv.svc;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PCEException;
import net.es.oscars.model.*;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.svc.conversions.L2VPNConversions;
import net.es.oscars.sb.nso.resv.NsoResvException;
import net.es.oscars.web.beans.BandwidthAvailabilityResponse;
import net.es.oscars.web.beans.ConnException;
import net.es.oscars.web.simple.SimpleConnection;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class L2VPNService {
    private final ConnService connSvc;

    public L2VPNService(ConnService connSvc) {
        this.connSvc = connSvc;
    }

    public BandwidthAvailabilityResponse bwAvailability(L2VPN l2VPN) throws ConnException {
        l2VPN.getStatus().setPhase(Phase.HELD);
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
