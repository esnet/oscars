package net.es.oscars.sb;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.util.DbAccess;
import net.es.oscars.model.Interval;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.sb.beans.SyncFeature;
import net.es.oscars.sb.nso.NsoLspStateSyncer;
import net.es.topo.common.dto.nso.NsoLSP;
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
import net.es.topo.common.dto.nso.NsoVPLS;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

import static net.es.oscars.sb.nso.NsoLspStateSyncer.hashKey;


@Slf4j
@Component
public class SouthboundPeriodicSyncer {

    @Value("${nso.sync}")
    private SyncFeature syncFeatureFlag;

    private final Startup startup;
    private final NsoVplsStateSyncer nsoVplsStateSyncer;
    private final NsoLspStateSyncer nsoLspStateSyncer;
    private final ConnService connSvc;
    private final NsoAdapter nsoAdapter;
    private final ConnectionRepository connRepo;

    private final Map<String, NsoServicesWrapper> nsoServicesByConnectionId = new HashMap<>();

    public SouthboundPeriodicSyncer(Startup startup, DbAccess dbAccess,
                                    NsoVplsStateSyncer nsoVplsStateSyncer,
                                    NsoLspStateSyncer nsoLspStateSyncer,
                                    ConnService connSvc, NsoAdapter nsoAdapter,
                                    ConnectionRepository connRepo) {
        this.startup = startup;
        this.connRepo = connRepo;
        this.nsoVplsStateSyncer = nsoVplsStateSyncer;
        this.nsoLspStateSyncer = nsoLspStateSyncer;
        this.connSvc = connSvc;
        this.nsoAdapter = nsoAdapter;
    }

    @Scheduled(fixedDelayString = "${nso.sync-interval-millisec}")
    @Transactional
    public void periodicSyncTask() {
        if (syncFeatureFlag.equals(SyncFeature.DISABLED)) return;
        if (startup.isInStartup() || startup.isInShutdown()) {
            log.info("application in startup or shutdown; skipping NSO sync");
            return;
        }
        this.sync();
    }

    public void sync() {
        boolean dryRynOnly = syncFeatureFlag.equals(SyncFeature.DRY_RUN_ONLY);
        // clear this map
        nsoServicesByConnectionId.clear();

        try {

            String vplsPath = nsoVplsStateSyncer.getNsoProxy().getNsoServiceConfigRestPath(NsoService.VPLS);
            String lspPath = nsoVplsStateSyncer.getNsoProxy().getNsoServiceConfigRestPath(NsoService.LSP);

            DesiredStateResult desiredState = this.desiredState();


            // First sync VPLSs, adding and removing instances. This will also add LSPs to newly added VPLS instances
            log.info("VPLS sync");
            nsoVplsStateSyncer.load(vplsPath);
            nsoVplsStateSyncer.setLocalState(desiredState.getVplsState());
            nsoVplsStateSyncer.sync(vplsPath, dryRynOnly);
            Dictionary<Integer, Triple<String, NsoStateSyncer.State, Boolean>> vplsSyncResults = nsoVplsStateSyncer.getSyncResults();

            // Next sync LSPs; this should generally perform LSP redeploys and deletes.
            //
            // here we will typically not need to add any LSPs (since they should have been added during the VPLS
            // sync step) but it will re-try anyway
            log.info("LSP sync");
            nsoLspStateSyncer.load(lspPath);
            nsoLspStateSyncer.setLocalState(desiredState.getLspState());
            nsoLspStateSyncer.sync(lspPath, dryRynOnly);
            Dictionary<Integer, Triple<String, NsoStateSyncer.State, Boolean>> lspSyncResults = nsoLspStateSyncer.getSyncResults();

            // if this WAS a dry run, we are done - we know no changes happened remotely
            if (dryRynOnly) {
                return;
            }

            // if this wasn't a dry run, we will use the sync results
            // to set the deployment state for the affected connections

            this.updateConnectionStates(vplsSyncResults, lspSyncResults);


        } catch (Exception e) {
            log.error("error while syncing vpls state", e);
            //FIXME: handle exceptions more gracefully
            throw new RuntimeException(e);
        }
    }

    @Data
    @Builder
    public static class DesiredStateResult {
        Dictionary<Integer, NsoStateWrapper<NsoVPLS>> vplsState;
        Dictionary<Integer, NsoStateWrapper<NsoLSP>> lspState;

    }

    public void updateConnectionStates(Dictionary<Integer, Triple<String, NsoStateSyncer.State, Boolean>> vplsSyncResults,
                                       Dictionary<Integer, Triple<String, NsoStateSyncer.State, Boolean>> lspSyncResults) {
        log.info("Updating connection states after sync");

        for (Integer vcId : Collections.list(vplsSyncResults.keys())) {
            Triple<String, NsoStateSyncer.State, Boolean> syncResult = vplsSyncResults.get(vcId);
            String connectionId = syncResult.getLeft();
            NsoStateSyncer.State syncState = syncResult.getMiddle();
            Boolean success = syncResult.getRight();
            connSvc.findConnection(connectionId).ifPresent(c -> {
                DeploymentState ds = c.getDeploymentState();
                // this is a bit complicated logic
                switch (syncState) {
                    case DELETE -> {
                        if (success) {
                            ds = DeploymentState.UNDEPLOY_FAILED;
                        } else {
                            ds = DeploymentState.UNDEPLOYED;
                        }
                    }
                    case ADD -> {
                        if (success) {
                            ds = DeploymentState.DEPLOY_FAILED;
                        } else {
                            ds = DeploymentState.DEPLOYED;
                        }
                    }
                    case REDEPLOY -> {
                        if (success) {
                            ds = DeploymentState.REDEPLOY_FAILED;
                        } else {
                            ds = DeploymentState.DEPLOYED;
                        }
                    }
                }
                log.info(connectionId + " : new ds: " + ds);
                c.setDeploymentState(ds);
                connRepo.save(c);
            });
        }
    }


    public DesiredStateResult desiredState() {
        List<Connection> connections = connectionsThatShouldBeDeployed();

        Dictionary<Integer, NsoStateWrapper<NsoVPLS>> desiredVpls = new Hashtable<>();
        Dictionary<Integer, NsoStateWrapper<NsoLSP>> desiredLsp = new Hashtable<>();

        for (Connection c : connections) {

            try {
                NsoServicesWrapper wrapped = nsoAdapter.nsoOscarsServices(c);
                // save this for later
                this.nsoServicesByConnectionId.put(c.getConnectionId(), wrapped);
                for (NsoVPLS vpls : wrapped.getVplsInstances()) {
                    NsoStateWrapper<NsoVPLS> sw = new NsoStateWrapper<>(NsoStateSyncer.State.NOOP, vpls);
                    desiredVpls.put(vpls.getVcId(), sw);
                }
                for (NsoLSP lsp : wrapped.getLspInstances()) {
                    NsoStateWrapper<NsoLSP> sw = new NsoStateWrapper<>(NsoStateSyncer.State.NOOP, lsp);
                    desiredLsp.put(hashKey(lsp), sw);

                }
            } catch (NsoGenException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
        return DesiredStateResult.builder()
                .vplsState(desiredVpls)
                .lspState(desiredLsp)
                .build();
    }

    public List<Connection> connectionsThatShouldBeDeployed() {
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
        List<Connection> currentlyReserved = connSvc.filter(f).getConnections();

        List<Connection> result = new ArrayList<>();
        for (Connection c : currentlyReserved) {
            boolean shouldBeDeployed = true;
            switch (c.getMode()) {
                case AUTOMATIC -> {
                    shouldBeDeployed = true;
                }
                case MANUAL -> {
                    if (c.getDeploymentIntent().equals(DeploymentIntent.SHOULD_BE_UNDEPLOYED)) {
                        // do not add it in the desired state
                        shouldBeDeployed = false;
                    }
                }
            }
            if (shouldBeDeployed) {
                result.add(c);
            }
        }
        return result;
    }

}