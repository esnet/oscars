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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processingLoop() {
        if (startup.isInStartup() || startup.isInShutdown()) {
//            log.info("application in startup or shutdown; skipping state transitions");
            return;
        }
        ReentrantLock connLock = dbAccess.getConnLock();
        boolean gotLock = connLock.tryLock();
        if (gotLock) {
            try {

                List<Connection> heldConns = new ArrayList<>(connService.getHeld().values());
                List<Connection> reservedConns = connRepo.findByPhase(Phase.RESERVED);

                List<Connection> unholdThese = new ArrayList<>();
                List<Connection> archiveThese = new ArrayList<>();

                List<NsiMapping> pastEndTime = new ArrayList<>();
                List<NsiMapping> timedOut = new ArrayList<>();

                for (Connection c : heldConns) {

                    if (c.getHeld().getExpiration().isBefore(Instant.now())) {
                        log.info("will un-hold a held connection that expired: " + c.getConnectionId());
                        Optional<NsiMapping> maybeMapping =  nsiMappingService.getMappingForOscarsId(c.getConnectionId());
                        maybeMapping.ifPresent(timedOut::add);
                        unholdThese.add(c);
                    }
                }

                for (Connection c : reservedConns) {
                    if (c.getReserved().getSchedule().getEnding().isBefore(Instant.now())) {
                        log.info("will archive (and dismantle if needed) a reserved connection that reached its end time: " + c.getConnectionId());
                        Optional<NsiMapping> maybeMapping =  nsiMappingService.getMappingForOscarsId(c.getConnectionId());
                        maybeMapping.ifPresent(pastEndTime::add);

                        if (c.getState().equals(State.ACTIVE)) {
                            log.info(c.getConnectionId() + " : active; will need to dismantle before archiving");
                        } else {
                            archiveThese.add(c);
                        }
                    }
                }

                List<NsiRequest> expiredRequests = nsiRequestManager.timedOut();
                for (NsiRequest req : expiredRequests) {
                    nsiRequestManager.remove(req.getNsiConnectionId());
                    try {
                        NsiMapping mapping = nsiMappingService.getMapping(req.getNsiConnectionId());
                        nsiService.timeoutRequest(mapping);
                    } catch (NsiMappingException ex) {
                        log.error("unable to roll back expired request for "+req.getNsiConnectionId(), ex);
                    }
                }


                for (NsiMapping mapping : pastEndTime) {
                    nsiService.pastEndTime(mapping);
                }

                for (NsiMapping mapping : timedOut) {
                    nsiService.resvTimedOut(mapping);
                }

                if (unholdThese.isEmpty() && archiveThese.isEmpty()) {
                    return;
                }

                unholdThese.forEach(c -> {
                    log.debug("Un-holding "+c.getConnectionId());
                    connService.unhold(c.getConnectionId());
                });

                archiveThese.forEach(c -> {
                    log.debug("Archiving "+c.getConnectionId());
                    c.setPhase(Phase.ARCHIVED);
                    c.setReserved(null);
                    connRepo.saveAndFlush(c);
                });


            } finally {
                // log.debug("unlocking connections");
                connLock.unlock();
            }
        } else {
            log.debug("unable to lock; waiting for next run ");
        }


    }

}