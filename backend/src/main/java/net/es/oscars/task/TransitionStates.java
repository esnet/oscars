package net.es.oscars.task;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.NsiException;
import net.es.oscars.app.util.DbAccess;
import net.es.oscars.nsi.ent.NsiMapping;
import net.es.oscars.nsi.svc.NsiService;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.enums.State;
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

    private final DbAccess dbAccess;
    public TransitionStates(ConnectionRepository connRepo, Startup startup, NsiService nsiService, DbAccess dbAccess) {
        this.connRepo = connRepo;
        this.startup = startup;
        this.nsiService = nsiService;
        this.dbAccess = dbAccess;
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
                // log.info("got connection lock");

                List<Connection> heldConns = connRepo.findByPhase(Phase.HELD);
                List<Connection> reservedConns = connRepo.findByPhase(Phase.RESERVED);
                List<Connection> deleteThese = new ArrayList<>();
                List<Connection> archiveThese = new ArrayList<>();

                List<NsiMapping> pastEndTime = new ArrayList<>();
                List<NsiMapping> timedOut = new ArrayList<>();

                for (Connection c : heldConns) {

                    if (c.getHeld().getExpiration().isBefore(Instant.now())) {
                        log.info("will delete expired held connection: " + c.getConnectionId());
                        try {
                            Optional<NsiMapping> maybeMapping = nsiService.getMappingForOscarsId(c.getConnectionId());
                            maybeMapping.ifPresent(timedOut::add);
                        } catch (NsiException ex) {
                            log.error(ex.getMessage(), ex);
                        }
                        deleteThese.add(c);
                    }
                }
                for (Connection c : reservedConns) {
                    if (c.getReserved().getSchedule().getEnding().isBefore(Instant.now())) {
                        log.info("will archive (and dismantle if needed) connection: " + c.getConnectionId());
                        try {
                            Optional<NsiMapping> maybeMapping = nsiService.getMappingForOscarsId(c.getConnectionId());
                            maybeMapping.ifPresent(pastEndTime::add);
                        } catch (NsiException ex) {
                            log.error(ex.getMessage(), ex);
                        }

                        if (c.getState().equals(State.ACTIVE)) {
                            log.info(c.getConnectionId() + " : active; waiting for dismantle before archiving");
                        } else {
                            archiveThese.add(c);
                        }
                    }
                }

                for (NsiMapping mapping : pastEndTime) {
                    nsiService.pastEndTime(mapping);
                }
                for (NsiMapping mapping : timedOut) {
                    nsiService.resvTimedOut(mapping);
                }

                if (deleteThese.isEmpty() && archiveThese.isEmpty()) {
                    return;
                }

                deleteThese.forEach(c -> log.debug("Deleting "+c.getConnectionId()));
                connRepo.deleteAll(deleteThese);

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