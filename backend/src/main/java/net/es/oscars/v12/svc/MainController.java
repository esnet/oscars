package net.es.oscars.v12.svc;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.v12.api.IntentException;
import net.es.oscars.v12.api.IntentVerifyResult;
import net.es.oscars.v12.api.L2VPNIntentRequest;
import net.es.oscars.v12.model.L2VPN;
import net.es.oscars.v12.model.TimeInterval;
import net.es.oscars.v12.model.intent.L2VPNIntent;
import net.es.oscars.v12.model.repo.L2VPNRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;

@Service
@Slf4j
public class Main Controller {
    @Autowired
    private L2VPNRepository repo;

    // TODO: implement this
    public IntentVerifyResult verifyIntent(L2VPNIntent intent) {
        return null;
    }

    /**
     * This method adds a new intent to the L2VPN.
     * The latest intent entry is automatically evaluated
     * for pathfinding, resource reservation, etc via a periodic
     * task.
     * In this task the  L2VPNIntent is checked whether it is satisfiable.
     * In that case it will be matched with  a L2VPNResource that satisfies it.
     *
     * That L2VPNResource will be deployed to replace any previous L2VPNResource
     * that was already deployed for this L2VPN
     *
     * @param request the L2VPNIntentRequest object
     * @return the saved L2VPN
     */
    @Transactional
    public L2VPN newIntent(L2VPNIntentRequest request) throws IntentException  {
        Optional<L2VPN> maybeL2vpn = repo.findByConnectionId(request.getConnectionId());
        if (maybeL2vpn.isEmpty()) {
            throw new IntentException("not found");
        }
        L2VPN l2VPN = maybeL2vpn.get();
        // we allow adding an intent to previously released L2VPNs
        // in this case they are marked as un-released (and not marked
        // for release either)
        l2VPN.setReleased(false);
        l2VPN.setMarkedForRelease(false);

        if (l2VPN.getIntents() == null) {
            l2VPN.setIntents(new ArrayList<>());
        }
        L2VPNIntent newIntent = request.getIntent();
        // overwrite or
        newIntent.setSatisfied(false);
        newIntent.setSatisfiedBy(null);
        if (newIntent.getValidity() == null) {
            newIntent.setValidity(TimeInterval.builder()
                            .indefinite(true)
                            .start(Instant.now())
                            .end(null)
                    .build());
        }

        // if there are any existing intents, modify the last one's validity
        // to end one millisec before the new one starts
        l2VPN.invalidateLastIntent(newIntent.getValidity().getStart()
                .minus(1, ChronoUnit.MILLIS));
        l2VPN.getIntents().add(request.getIntent());

        return repo.save(l2VPN);
    }

    /**
     * This operation marks a L2VPN for release (and undeployment)
     *
     * The actual work will be performed by our periodic task
     *
     * @param connectionId the connection id
     * @return the updated L2VPN instance
     * @throws IntentException
     */
    @Transactional
    public L2VPN release(String connectionId) throws IntentException {
        Optional<L2VPN> maybeL2vpn = repo.findByConnectionId(connectionId);
        if (maybeL2vpn.isEmpty()) {
            throw new IntentException("not found");
        }
        L2VPN l2VPN = maybeL2vpn.get();
        l2VPN.setMarkedForRelease(true);

        return repo.save(l2VPN);
    }



}
