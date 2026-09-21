package net.es.oscars.task;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.enums.DeploymentIntent;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.sb.nso.NsoAdapter;
import net.es.oscars.sb.nso.exc.NsoGenException;
import net.es.oscars.sb.nso.exc.NsoReadException;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.NsoVPLS;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Component
@Slf4j
public class NsoStaleReport {
    private final ConnectionRepository connRepo;
    private final Startup startup;
    private final NsoAdapter nsoAdapter;

    public NsoStaleReport(ConnectionRepository connRepo, Startup startup, NsoAdapter nsoAdapter) {
        this.connRepo = connRepo;
        this.startup = startup;
        this.nsoAdapter = nsoAdapter;
    }


    @Scheduled(fixedDelayString = "${nso.stale-report-interval}")
    @Transactional
    public void generateReport() {
        if (startup.isInStartup() || startup.isInShutdown()) {
            log.info("application in startup or shutdown; skipping state transitions");
            return;
        }
        Instant now = Instant.now();
        List<Connection> reservedConns = connRepo.findByPhase(Phase.RESERVED);

        // these are the connections that are supposed to be deployed right now
        Map<String, Connection> shouldBeDeployed = new HashMap<>();
        for (Connection conn : reservedConns) {
            if (conn.getReserved().getSchedule().getBeginning().isBefore(now) &&
                conn.getReserved().getSchedule().getEnding().isAfter(now)) {
                if (!conn.getDeploymentIntent().equals(DeploymentIntent.SHOULD_BE_UNDEPLOYED)) {
                    shouldBeDeployed.put(conn.getConnectionId(), conn);
                }
            }
        }

        try {
            NsoAdapter.OscarsNsoState nsoState = nsoAdapter.fetchNsoState(true);

            // see what is actually deployed - if it is not supposed to, it is "stale"
            Map<String, Pair<NsoVPLS, List<NsoLSP>>> staleConnections = new HashMap<>();
            for (String connectionId : nsoState.getServiceMap().keySet()) {
                if (!shouldBeDeployed.containsKey(connectionId)) {
                    staleConnections.put(connectionId, nsoState.getServiceMap().get(connectionId));
                    log.info("Stale on NSO: {}", connectionId);
                }
            }

            // generate NSO commands for cleaning stale connections
            for (String connectionId : staleConnections.keySet()) {
                Optional<NsoAdapter.NsoOscarsDismantle> maybeDismantle = nsoAdapter.nsoOscarsDismantle(connectionId);
                if (maybeDismantle.isPresent()) {
                    NsoAdapter.NsoOscarsDismantle dismantle = maybeDismantle.get();
                    String commands = dismantle.asCliCommands();
                    log.info("    DISMANTLE {}+ :\n{}", connectionId, commands);
                }
            }

        } catch (NsoReadException | NsoGenException e) {
            log.error("Error creating NSO staleness report", e);
        }
    }
}
