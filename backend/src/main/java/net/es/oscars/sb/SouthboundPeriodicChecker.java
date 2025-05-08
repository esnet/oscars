package net.es.oscars.sb;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.util.DbAccess;
import net.es.oscars.dto.pss.cmd.CommandType;
import net.es.oscars.sb.beans.QueueName;
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
            Set<Connection> shouldBeMadeUndeployed = new HashSet<>();
            Set<String> shouldBeFinished = new HashSet<>();

            Set<Connection> shouldBeDeployed = new HashSet<>();


            // look at all RESERVED connections;
            // expired ones need to be made UNDEPLOYED
            // ones that just entered their activation schedule need to be made DEPLOYED
            List<Connection> reservedConns = connRepo.findByPhase(Phase.RESERVED);
            for (Connection c : reservedConns) {
                Schedule s = c.getReserved().getSchedule();
                if (s.getEnding().isBefore(Instant.now())) {
                    shouldBeMadeUndeployed.add(c);
                    shouldBeFinished.add(c.getConnectionId());
                } else if (s.getBeginning().isBefore(Instant.now())) {
                    if (c.getMode().equals(BuildMode.AUTOMATIC)) {
                        shouldBeDeployed.add(c);
                    }
                }
            }

            // modify intents on connections
            for (Connection c: shouldBeDeployed)  {
                if (c.getDeploymentIntent().equals(DeploymentIntent.SHOULD_BE_UNDEPLOYED)) {
                    log.info("now set to should-be-deployed "+c.getConnectionId());
                    c.setDeploymentIntent(DeploymentIntent.SHOULD_BE_DEPLOYED);
                    connRepo.save(c);
                }
            }

            for (Connection c: shouldBeMadeUndeployed)  {
                if (c.getDeploymentIntent().equals(DeploymentIntent.SHOULD_BE_DEPLOYED)) {
                    log.info("now set to should-be-undeployed "+c.getConnectionId());
                    c.setDeploymentIntent(DeploymentIntent.SHOULD_BE_UNDEPLOYED);
                    connRepo.save(c);
                }
            }

            southboundQueuer.clear(QueueName.DONE);

            // these catch any connections that don't have the correct state
            List<Connection> deployThese = connRepo
                    .findByDeploymentIntentAndDeploymentState(DeploymentIntent.SHOULD_BE_DEPLOYED, DeploymentState.UNDEPLOYED);
            List<Connection> undeployThese = connRepo
                    .findByDeploymentIntentAndDeploymentState(DeploymentIntent.SHOULD_BE_UNDEPLOYED, DeploymentState.DEPLOYED);

            List<Connection> redeployThese = connRepo.findByDeploymentIntent(DeploymentIntent.SHOULD_BE_REDEPLOYED);



            // we only allow RESERVED connections that have a start time in the past to ever get deployed
            for (Connection c : deployThese) {
                if (c.getPhase().equals(Phase.RESERVED) &&
                        c.getReserved() != null &&
                        c.getReserved().getSchedule().getBeginning().isBefore(Instant.now())) {
                    log.info("it is not-deployed, and should-be - adding a queued job to BUILD: "+c.getConnectionId());
                    c.setDeploymentState(DeploymentState.WAITING_TO_BE_DEPLOYED);
                    connRepo.save(c);
                    southboundQueuer.add(CommandType.BUILD, c.getConnectionId(), State.ACTIVE);
                }
            }

            for (Connection c : undeployThese) {
                log.info("it is deployed, and should-not-be - adding a queued job to DISMANTLE: "+c.getConnectionId());
                // we allow connections in any phase to get undeployed
                c.setDeploymentState(DeploymentState.WAITING_TO_BE_UNDEPLOYED);
                connRepo.save(c);
                if (shouldBeFinished.contains(c.getConnectionId())) {
                    southboundQueuer.add(CommandType.DISMANTLE, c.getConnectionId(), State.WAITING);
                } else {
                    southboundQueuer.add(CommandType.DISMANTLE, c.getConnectionId(), State.FINISHED);
                }
            }

            for (Connection c : redeployThese) {
                log.info("needs to be redeployed - adding a queued job to REDEPLOY: "+c.getConnectionId());
                c.setDeploymentState(DeploymentState.WAITING_TO_BE_REDEPLOYED);
                connRepo.save(c);
                southboundQueuer.add(CommandType.REDEPLOY, c.getConnectionId(), State.ACTIVE);
            }

            // run the PSS queue
            southboundQueuer.process();
            connLock.unlock();

        }
    }


}