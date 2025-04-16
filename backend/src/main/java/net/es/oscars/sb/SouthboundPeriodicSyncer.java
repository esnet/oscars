package net.es.oscars.sb;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.app.util.DbAccess;
import net.es.oscars.model.Interval;
import net.es.topo.common.dto.nso.enums.NsoService;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.enums.*;
import net.es.oscars.sb.nso.NsoAdapter;
import net.es.oscars.sb.nso.NsoStateSyncer;
import net.es.oscars.sb.nso.NsoVplsStateSyncer;
import net.es.oscars.sb.nso.dto.NsoStateWrapper;
import net.es.oscars.sb.nso.exc.NsoGenException;
import net.es.oscars.sb.nso.rest.NsoServicesWrapper;
import net.es.oscars.web.beans.ConnectionFilter;
import net.es.oscars.web.rest.ConnController;
import net.es.topo.common.dto.nso.NsoVPLS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;


@Slf4j
@Component
public class SouthboundPeriodicSyncer {

    @Value("${nso.sync}")
    private Boolean sync;

    private final Startup startup;
    private final DbAccess dbAccess;
    private final NsoVplsStateSyncer nsoVplsStateSyncer;
    private final ConnController connController;
    private final NsoAdapter nsoAdapter;

    public SouthboundPeriodicSyncer(Startup startup, DbAccess dbAccess, NsoVplsStateSyncer nsoVplsStateSyncer, ConnController connController, NsoAdapter nsoAdapter) {
        this.startup = startup;
        this.dbAccess = dbAccess;
        this.nsoVplsStateSyncer = nsoVplsStateSyncer;
        this.connController = connController;
        this.nsoAdapter = nsoAdapter;
    }

    @Scheduled(fixedDelayString = "${nso.sync-interval-millisec}")
    @Transactional
    public void scheduledSync() {
        if (!sync) return;
        this.periodicSync();
    }

    public void periodicSync() {
        if (startup.isInStartup() || startup.isInShutdown()) {
            // log.info("application in startup or shutdown; skipping state transitions");
            return;
        }

        ReentrantLock connLock = dbAccess.getConnLock();
        boolean gotLock = connLock.tryLock();
        if (gotLock) {
            try {
                String vplsPath = nsoVplsStateSyncer.getNsoProxy().getNsoServiceConfigRestPath(NsoService.VPLS);
                Dictionary<Integer, NsoStateWrapper<NsoVPLS>> desiredState = this.desiredState();
                nsoVplsStateSyncer.load(vplsPath);
                nsoVplsStateSyncer.setLocalState(desiredState);
                if (nsoVplsStateSyncer.sync(vplsPath)) {

                    nsoVplsStateSyncer.load(vplsPath);
                    Enumeration<NsoStateWrapper<NsoVPLS>> enumeration = nsoVplsStateSyncer.getLocalState().elements();
                    while (enumeration.hasMoreElements()) {
                        NsoStateWrapper<NsoVPLS> wrappedNsoVPLS = enumeration.nextElement();
                        // This should automatically mark this VPLS as "noop", "add", "delete", or "redeploy"
                        nsoVplsStateSyncer.evaluate(wrappedNsoVPLS.getInstance().getVcId());
                    }
                } else {
                    // sync did not succeed
                }

                // reload state from NSO after sync
                // FIXME: modify local connection state according to what NSO gives back

            } catch (Exception e) {
                log.error("error while syncing vpls state", e);
                //FIXME: handle exceptions more gracefully
                throw new RuntimeException(e);
            }
            connLock.unlock();

        }
    }
    public Dictionary<Integer, NsoStateWrapper<NsoVPLS>> desiredState() throws StartupException {

        Interval interval = Interval.builder()
                .beginning(Instant.now())
                .ending(Instant.now())
                .build();
        ConnectionFilter f = ConnectionFilter.builder()
                .phase(Phase.RESERVED.toString())
                .sizePerPage(Integer.MAX_VALUE)
                .interval(interval)
                .page(1)
                .build();
        // this is a list of all the connections that are currently active
        List<Connection> connections = connController.list(f).getConnections();

        Dictionary<Integer, NsoStateWrapper<NsoVPLS>> desiredState = new Hashtable<>();
        for (Connection c : connections) {
            try {
                NsoServicesWrapper wrapped = nsoAdapter.nsoOscarsServices(c);
                for (NsoVPLS vpls : wrapped.getVplsInstances() ){
                    NsoStateWrapper<NsoVPLS> stateWrapper = new NsoStateWrapper<>(NsoStateSyncer.State.NOOP, vpls);

                    desiredState.put(vpls.getVcId(), stateWrapper);
                }
            } catch (NsoGenException ex) {
                log.error(ex.getMessage(),ex);
            }
        }
        return desiredState;
    }

}