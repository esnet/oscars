package net.es.oscars.task;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.NsiMappingException;
import net.es.oscars.app.util.DbAccess;
import net.es.oscars.nsi.beans.NsiRequest;
import net.es.oscars.nsi.ent.NsiMapping;
import net.es.oscars.nsi.svc.NsiMappingService;
import net.es.oscars.nsi.svc.NsiRequestManager;
import net.es.oscars.nsi.svc.NsiService;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.enums.State;
import net.es.oscars.resv.svc.ConnService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;


@Slf4j
@Component
public class TransitionStates {

    private final ConnectionRepository connRepo;
    private final Startup startup;
    private final NsiService nsiService;
    private final NsiMappingService nsiMappingService;

    private final DbAccess dbAccess;
    private final NsiRequestManager nsiRequestManager;
    private final ConnService connService;

    public TransitionStates(ConnectionRepository connRepo, Startup startup,
                            NsiService nsiService, NsiMappingService nsiMappingService, DbAccess dbAccess, NsiRequestManager nsiRequestManager, ConnService connService) {
        this.connRepo = connRepo;
        this.startup = startup;
        this.nsiService = nsiService;
        this.nsiMappingService = nsiMappingService;
        this.dbAccess = dbAccess;
        this.nsiRequestManager = nsiRequestManager;
        this.connService = connService;
    }

    @Scheduled(fixedDelayString = "${resv.state-delay}")
    @Transactional
    public void processingLoop() {
        if (startup.isInStartup() || startup.isInShutdown()) {
//            log.info("application in startup or shutdown; skipping state transitions");
            return;
        }
        ReentrantLock connLock = dbAccess.getConnLock();
        boolean gotLock = connLock.tryLock();
        if (gotLock) {
//            log.info("TransitionStates got connLock");
            try {
                List<Connection> heldConns = new ArrayList<>(connService.getHeld().values());
                List<Connection> reservedConns = connRepo.findByPhase(Phase.RESERVED);

                List<Connection> unholdThese = new ArrayList<>();
                List<Connection> releaseThese = new ArrayList<>();

                Set<NsiMapping> pastEndTime = new HashSet<>();
                Set<NsiMapping> timedOut = new HashSet<>();

//                log.info("processing held connections");
                for (Connection c : heldConns) {

                    if (c.getHeld() == null || c.getHeld().getExpiration().isBefore(Instant.now())) {
                        log.info("will un-hold a held connection that expired: {}", c.getConnectionId());
                        Optional<NsiMapping> maybeMapping =  nsiMappingService.getMappingForOscarsId(c.getConnectionId());
                        maybeMapping.ifPresent(m -> {
                            log.info("timing out associated NSI mapping: " + m.getNsiConnectionId());
                            timedOut.add(m);
                        });
                        maybeMapping.ifPresent(timedOut::add);
                        unholdThese.add(c);
                    }
                }

//                log.info("processing reserved connections");
                for (Connection c : reservedConns) {
                    if (c.getReserved().getSchedule().getEnding().isBefore(Instant.now())) {
                        log.info("will archive (and dismantle if needed) a reserved connection that reached its end time: {}", c.getConnectionId());
                        Optional<NsiMapping> maybeMapping =  nsiMappingService.getMappingForOscarsId(c.getConnectionId());
                        maybeMapping.ifPresent(pastEndTime::add);

                        if (c.getState().equals(State.ACTIVE)) {
                            log.info(c.getConnectionId() + " : active; will need to dismantle before archiving");
                        } else {
                            releaseThese.add(c);
                        }
                    }
                }

//                log.info("processing timed out NSI connections");
                List<NsiRequest> expiredRequests = nsiRequestManager.timedOut();
                for (NsiRequest req : expiredRequests) {
                    nsiRequestManager.remove(req.getNsiConnectionId());
                    try {
                        NsiMapping mapping = nsiMappingService.getMapping(req.getNsiConnectionId());
                        timedOut.add(mapping);
                        log.info("an NSI request timed out {}", req.getNsiConnectionId());
                    } catch (NsiMappingException ex) {
                        log.error("mapping problem: {}", req.getNsiConnectionId(), ex);
                    }
                }


                for (NsiMapping mapping : pastEndTime) {
                    nsiService.pastEndTime(mapping);
                }

                for (NsiMapping mapping : timedOut) {
                    nsiService.resvTimedOut(mapping);
                }

                unholdThese.forEach(c -> {
                    log.debug("Un-holding "+c.getConnectionId());
                    connService.unhold(c.getConnectionId());
                });

                releaseThese.forEach(c -> {
                    log.debug("Releasing "+c.getConnectionId());
                    connService.release(c);
                });


            } finally {
//                log.info("TransitionStates releasing connLock");
                connLock.unlock();
            }
        } else {
            log.debug("unable to lock; waiting for next run ");
        }
    }



}