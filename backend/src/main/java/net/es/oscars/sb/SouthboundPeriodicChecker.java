package net.es.oscars.sb;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.util.DbAccess;
import net.es.oscars.dto.pss.cmd.CommandType;
import net.es.oscars.pss.beans.QueueName;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.ent.Schedule;
import net.es.oscars.resv.enums.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;


@Slf4j
@Component
public class SouthboundPeriodicChecker {
    @Autowired
    private SouthboundQueuer southboundQueuer;

    @Autowired
    private ConnectionRepository connRepo;


    @Autowired
    private Startup startup;

    @Autowired
    private DbAccess dbAccess;

    private Map<String, Integer> attempts = new HashMap<>();

    /**
     * this function runs every 3 seconds and checks whether:
     * - A RESERVED connection is
     *   - past its starting time and needs to be built (for AUTOMATIC build mode)
     *   - past its end time and needs to be dismantled
     * - Any connection's deployment intent does not match its deployment state
     *
     */
    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void buildOrDismantle() {
        if (startup.isInStartup() || startup.isInShutdown()) {
            // log.info("application in startup or shutdown; skipping state transitions");
            return;
        }

        ReentrantLock connLock = dbAccess.getConnLock();
        boolean gotLock = connLock.tryLock();
        if (gotLock) {
            Set<Connection> shouldBeUndeployed = new HashSet<>();
            Set<String> shouldBeFinished = new HashSet<>();

            Set<Connection> shouldBeDeployed = new HashSet<>();

            //
            List<Connection> reservedConns = connRepo.findByPhase(Phase.RESERVED);
            for (Connection c : reservedConns) {
                Schedule s = c.getReserved().getSchedule();
                if (s.getEnding().isBefore(Instant.now())) {
                    shouldBeUndeployed.add(c);
                    shouldBeFinished.add(c.getConnectionId());

                } else if (s.getBeginning().isBefore(Instant.now())) {
                    boolean shouldDeploy = false;
                    if (c.getMode().equals(BuildMode.AUTOMATIC)) {
                        shouldDeploy = true;
                    }
                    if (shouldDeploy) {
                        shouldBeDeployed.add(c);
                    }
                }
            }
            // modify the intent
            for (Connection c: shouldBeDeployed)  {
                if (c.getDeploymentIntent().equals(DeploymentIntent.SHOULD_BE_UNDEPLOYED)) {
                    c.setDeploymentIntent(DeploymentIntent.SHOULD_BE_DEPLOYED);
                    connRepo.save(c);
                }
            }
            for (Connection c: shouldBeUndeployed)  {
                if (c.getDeploymentIntent().equals(DeploymentIntent.SHOULD_BE_DEPLOYED)) {
                    c.setDeploymentIntent(DeploymentIntent.SHOULD_BE_UNDEPLOYED);
                    connRepo.save(c);
                }
            }

            southboundQueuer.clear(QueueName.DONE);
            List<Connection> deployThese = connRepo
                    .findByDeploymentIntentAndDeploymentState(DeploymentIntent.SHOULD_BE_DEPLOYED, DeploymentState.UNDEPLOYED);
            List<Connection> undeployThese = connRepo
                    .findByDeploymentIntentAndDeploymentState(DeploymentIntent.SHOULD_BE_UNDEPLOYED, DeploymentState.DEPLOYED);

            for (Connection c : deployThese) {
                // we only allow RESERVED connections to ever get deployed
                if (c.getPhase().equals(Phase.RESERVED)) {
                    c.setDeploymentState(DeploymentState.WAITING_TO_BE_DEPLOYED);
                    connRepo.save(c);
                    southboundQueuer.add(CommandType.BUILD, c.getConnectionId(), State.ACTIVE);
                }
            }

            for (Connection c : undeployThese) {
                // .. but we allow connections in any phase to get undeployed
                c.setDeploymentState(DeploymentState.WAITING_TO_BE_UNDEPLOYED);
                connRepo.save(c);
                if (shouldBeFinished.contains(c.getConnectionId())) {
                    southboundQueuer.add(CommandType.DISMANTLE, c.getConnectionId(), State.WAITING);
                } else {
                    southboundQueuer.add(CommandType.DISMANTLE, c.getConnectionId(), State.FINISHED);
                }
            }

            // run the PSS queue
            southboundQueuer.process();
            connLock.unlock();

        }
    }


}