package net.es.oscars.v12.svc;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.v12.api.OscarsException;
import net.es.oscars.v12.api.L2VPNIntentRequest;
import net.es.oscars.v12.model.L2VPN;
import net.es.oscars.v12.model.common.TimeInterval;
import net.es.oscars.v12.model.intent.L2VPNIntent;
import net.es.oscars.v12.model.repo.L2VPNRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
public class MainController {
    @Autowired
    private L2VPNRepository repo;

    /**
     * This method adds a new intent to the L2VPN.
     * The L2VPNIntent is not checked here whether it is satisfiable or not.
     * That intent entry is automatically evaluated
     * for pathfinding, resource reservation, etc via a periodic
     * task.
     *
     *
     *
     * @param request the L2VPNIntentRequest object
     * @return the updated L2VPN instance
     * @throws OscarsException an exception
     * */
    @Transactional
    public L2VPN newIntent(L2VPNIntentRequest request) throws OscarsException {
        Optional<L2VPN> maybeL2vpn = repo.findByConnectionId(request.getConnectionId());
        if (maybeL2vpn.isEmpty()) {
            throw new OscarsException("not found");
        }
        L2VPN l2VPN = maybeL2vpn.get();

        L2VPNIntent newIntent = request.getIntent();

        newIntent.setSatisfied(false);
        newIntent.setSatisfiedBy(null);
        if (newIntent.getValidity() == null) {
            newIntent.setValidity(TimeInterval.builder()
                            .indefinite(true)
                            .beginning(Instant.now())
                            .ending(null)
                    .build());
        }
        l2VPN.setIntent(newIntent);

        return repo.save(l2VPN);
    }

    /**
     * This operation marks a L2VPN for release
     * The actual work of releasing will be performed by our periodic task
     *
     * @param connectionId the connection id
     * @return the updated L2VPN instance
     * @throws OscarsException an exception
     */
    @Transactional
    public L2VPN release(String connectionId) throws OscarsException {
        Optional<L2VPN> maybeL2vpn = repo.findByConnectionId(connectionId);
        if (maybeL2vpn.isEmpty()) {
            throw new OscarsException("not found");
        }
        L2VPN l2VPN = maybeL2vpn.get();
        l2VPN.setShouldBeReleased(true);

        return repo.save(l2VPN);
    }

    /**
     * This operation marks a L2VPN for deployment or undeployment
     *
     * @param connectionId the connection id
     * @return the updated L2VPN instance
     * @throws OscarsException an exception
     */
    @Transactional
    public L2VPN deployOrUndeploy(String connectionId, boolean deploy) throws OscarsException {
        Optional<L2VPN> maybeL2vpn = repo.findByConnectionId(connectionId);
        if (maybeL2vpn.isEmpty()) {
            throw new OscarsException("not found");
        }
        L2VPN l2VPN = maybeL2vpn.get();
        if (l2VPN.getResource() == null) {
            throw new OscarsException("no resource to deploy or undeploy");
        } else {
            l2VPN.getResource().setShouldBeDeployed(deploy);
        }

        return repo.save(l2VPN);
    }

}
