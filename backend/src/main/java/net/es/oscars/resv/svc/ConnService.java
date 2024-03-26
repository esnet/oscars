package net.es.oscars.resv.svc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PCEException;
import net.es.oscars.app.util.DbAccess;
import net.es.oscars.sb.db.RouterCommandsRepository;
import net.es.oscars.sb.ent.RouterCommands;
import net.es.oscars.sb.nso.resv.NsoResourceService;
import net.es.oscars.sb.nso.resv.NsoResvException;
import net.es.oscars.resv.db.*;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.*;
import net.es.oscars.topo.beans.IntRange;
import net.es.oscars.topo.beans.PortBwVlan;
import net.es.oscars.web.beans.*;
import net.es.oscars.web.simple.*;
import net.es.topo.common.devel.DevelUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.Multigraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.es.oscars.resv.svc.ConnUtils.*;
import static net.es.oscars.sb.nso.NsoAdapter.NSO_TEMPLATE_VERSION;

@Service
@Slf4j
@Data
public class ConnService {

    @Autowired
    private ConnectionRepository connRepo;

    @Autowired
    private LogService logService;

    @Autowired
    private ResvService resvService;

    @Autowired
    private NsoResourceService nsoResourceService;

    @Autowired
    private RouterCommandsRepository rcRepo;

    @Autowired
    private HeldRepository heldRepo;

    @Autowired
    private ScheduleRepository schRepo;

    @Autowired
    private ArchivedRepository archivedRepo;

    @Autowired
    private ReservedRepository reservedRepo;


    @Autowired
    private FixtureRepository fixRepo;

    @Autowired
    private PipeRepository pipeRepo;

    @Autowired
    private ObjectMapper jacksonObjectMapper;

    @Autowired
    private DbAccess dbAccess;

    @Autowired
    private ConnUtils connUtils;

    @Value("${pss.default-mtu:9000}")
    private Integer defaultMtu;

    @Value("${pss.min-mtu:1500}")
    private Integer minMtu;

    @Value("${pss.max-mtu:9000}")
    private Integer maxMtu;

    @Value("${resv.minimum-duration:15}")
    private Integer minDuration;

    public ConnectionList filter(ConnectionFilter filter) {

        List<Connection> reservedAndArchived = new ArrayList<>();
        List<Phase> phases = new ArrayList<>();
        phases.add(Phase.ARCHIVED);
        phases.add(Phase.RESERVED);

        // first we don't take into account anything that doesn't have any archived
        // i.e. we discount any temporarily held
        for (Connection c : connRepo.findByPhaseIn(phases)) {
            try {
                if (c.getArchived() != null) {
                    reservedAndArchived.add(c);
                } else {
                    log.error("no archived components for " + c.getConnectionId());

                }

            } catch (EntityNotFoundException ex) {
                log.error("missed a connection somehow " + c.getConnectionId());

            }
        }

        List<Connection> connIdFiltered = reservedAndArchived;

        if (filter.getConnectionId() != null) {
            connIdFiltered = new ArrayList<>();
            for (Connection c : reservedAndArchived) {
                if (c.getConnectionId().toLowerCase().contains(filter.getConnectionId().toLowerCase())) {
                    connIdFiltered.add(c);
                }
            }
        }

        List<Connection> descFiltered = connIdFiltered;
        if (filter.getDescription() != null) {

            descFiltered = new ArrayList<>();
            for (Connection c : connIdFiltered) {
                boolean found = c.getDescription().toLowerCase().contains(filter.getDescription().toLowerCase());
                for (Tag tag : c.getTags()) {
                    if (tag.getContents().toLowerCase().contains(filter.getDescription().toLowerCase())) {
                        found = true;
                    }
                    if (tag.getCategory().toLowerCase().contains(filter.getDescription().toLowerCase())) {
                        found = true;
                    }
                }
                if (found) {
                    descFiltered.add(c);
                }
            }
        }

        List<Connection> phaseFiltered = descFiltered;
        if (filter.getPhase() != null && !filter.getPhase().equals("ANY")) {
            phaseFiltered = new ArrayList<>();
            for (Connection c : descFiltered) {
                if (c.getPhase().toString().equals(filter.getPhase())) {
                    phaseFiltered.add(c);
                }
            }
        }

        List<Connection> userFiltered = phaseFiltered;
        if (filter.getUsername() != null) {
            Pattern pattern = Pattern.compile(filter.getUsername(), Pattern.CASE_INSENSITIVE);
            userFiltered = new ArrayList<>();
            for (Connection c : phaseFiltered) {
                Matcher matcher = pattern.matcher(c.getUsername());
                if (matcher.find()) {
                    userFiltered.add(c);
                }
            }
        }

        List<Connection> portFiltered = userFiltered;
        if (filter.getPorts() != null && !filter.getPorts().isEmpty()) {
            List<Pattern> patterns = new ArrayList<>();
            for (String portFilter : filter.getPorts()) {
                Pattern pattern = Pattern.compile(portFilter, Pattern.CASE_INSENSITIVE);
                patterns.add(pattern);
            }

            portFiltered = new ArrayList<>();
            for (Connection c : userFiltered) {
                boolean add = false;
                for (VlanFixture f : c.getArchived().getCmp().getFixtures()) {
                    for (Pattern pattern : patterns) {
                        Matcher matcher = pattern.matcher(f.getPortUrn());
                        if (matcher.find()) {
                            add = true;
                        }
                    }
                }
                if (add) {
                    portFiltered.add(c);
                }
            }
        }

        List<Connection> vlanFiltered = portFiltered;
        if (filter.getVlans() != null && !filter.getVlans().isEmpty()) {
            vlanFiltered = new ArrayList<>();
            for (Connection c : portFiltered) {
                boolean add = false;
                for (VlanFixture f : c.getArchived().getCmp().getFixtures()) {
                    String fixtureVlanStr = f.getVlan().getVlanId().toString();
                    for (Integer vlan : filter.getVlans()) {
                        String vlanStr = vlan.toString();
                        if (fixtureVlanStr.contains(vlanStr)) {
                            add = true;
                            break;
                        }
                    }
                }
                if (add) {
                    vlanFiltered.add(c);
                }
            }
        }
        List<Connection> intervalFiltered = vlanFiltered;
        if (filter.getInterval() != null) {
            Instant fBeginning = filter.getInterval().getBeginning();
            Instant fEnding = filter.getInterval().getEnding();
            intervalFiltered = new ArrayList<>();
            for (Connection c : vlanFiltered) {
                boolean add = true;
                Schedule s;
                if (c.getPhase().equals(Phase.ARCHIVED)) {
                    s = c.getArchived().getSchedule();
                } else if (c.getPhase().equals(Phase.RESERVED)) {
                    s = c.getReserved().getSchedule();
                } else {
                    // shouldn't happen!
                    log.error("invalid phase for " + c.getConnectionId());
                    continue;
                }

                if (s.getEnding().isBefore(fBeginning)) {
                    add = false;
                    // log.info("not adding, schedule is before interval "+c.getConnectionId());
                }
                if (s.getBeginning().isAfter(fEnding)) {
                    add = false;
                    // log.info("not adding, schedule is after interval "+c.getConnectionId());
                }
                if (add) {
                    intervalFiltered.add(c);
                }
            }
        }

        List<Connection> southboundFiltered = intervalFiltered;
        if (filter.getSouthbound() != null && !filter.getSouthbound().equals("any")) {
            southboundFiltered = new ArrayList<>();
            for (Connection c : intervalFiltered) {
                if (this.southbound(c.getConnectionId()).toString().equals(filter.getSouthbound())) {
                    southboundFiltered.add(c);
                }

            }
        }

        List<Connection> finalFiltered = southboundFiltered;
        List<Connection> paged = new ArrayList<>();
        int totalSize = finalFiltered.size();

        if (filter.getSizePerPage() < 0) {
            //
            paged = finalFiltered;
        } else {
            // pages start at 1
            int firstIdx = (filter.getPage() - 1) * filter.getSizePerPage();
            // log.info("first idx: "+firstIdx);
            // if past the end, would return empty list
            if (firstIdx < totalSize) {

                int lastIdx = firstIdx + filter.getSizePerPage();
                if (lastIdx > totalSize) {
                    lastIdx = totalSize;
                }
                for (int idx = firstIdx; idx < lastIdx; idx++) {
                    // log.info(idx+" - adding to list: "+finalFiltered.get(idx).getConnectionId());
                    paged.add(finalFiltered.get(idx));
                }
            }

        }

        return ConnectionList.builder()
                .page(filter.getPage())
                .sizePerPage(filter.getSizePerPage())
                .totalSize(totalSize)
                .connections(paged)
                .build();


    }

    public ConnectionSouthbound southbound(String connectionId) {
        List<RouterCommands> routerCommandsList = rcRepo.findByConnectionId(connectionId);
        for (RouterCommands rc : routerCommandsList) {
            if (rc.getTemplateVersion().startsWith(NSO_TEMPLATE_VERSION) ||
                    rc.getTemplateVersion().startsWith("NSO")) {
                return ConnectionSouthbound.NSO;
            }
        }
        return ConnectionSouthbound.RANCID;
    }


    @Transactional
    public void modifySchedule(Connection c, Instant beginning, Instant ending) throws ModifyException {
        if (!c.getPhase().equals(Phase.RESERVED)) {
            throw new ModifyException("May only change schedule when RESERVED");
        }

        c.getReserved().getSchedule().setEnding(ending);
        c.getReserved().getSchedule().setBeginning(beginning);
        Validity v = verifyModification(c);
        if (v.isValid()) {
            c.getArchived().getSchedule().setEnding(ending);
            c.getArchived().getSchedule().setBeginning(ending);
            connRepo.save(c);
        }

    }

    @Transactional
    public void modifyBandwidth(Connection c, Integer bandwidth) throws ModifyException {
        if (!c.getPhase().equals(Phase.RESERVED)) {
            throw new ModifyException("May only change schedule when RESERVED");
        }
        int max = findAvailableMaxBandwidth(c);
        if (bandwidth > max) {
            throw new ModifyException("New bandwidth above available");
        }

        for (VlanFixture f : c.getReserved().getCmp().getFixtures()) {
            f.setIngressBandwidth(bandwidth);
            f.setEgressBandwidth(bandwidth);
            fixRepo.save(f);
        }

        for (VlanFixture f : c.getArchived().getCmp().getFixtures()) {
            f.setIngressBandwidth(bandwidth);
            f.setEgressBandwidth(bandwidth);
            fixRepo.save(f);
        }
        for (VlanPipe p : c.getReserved().getCmp().getPipes()) {
            p.setAzBandwidth(bandwidth);
            p.setZaBandwidth(bandwidth);
            pipeRepo.save(p);
        }
        for (VlanPipe p : c.getArchived().getCmp().getPipes()) {
            p.setAzBandwidth(bandwidth);
            p.setZaBandwidth(bandwidth);
            pipeRepo.save(p);
        }
        if (c.getState().equals(State.ACTIVE) && c.getDeploymentState().equals(DeploymentState.DEPLOYED)) {
            c.setDeploymentIntent(DeploymentIntent.SHOULD_BE_REDEPLOYED);
        }
    }

    public Validity modifyNsi(Connection c, Integer bandwidth, Instant beginning, Instant ending) throws ModifyException {
        if (beginning != null) {
            c.getReserved().getSchedule().setBeginning(beginning);
        }
        if (ending != null) {
            c.getReserved().getSchedule().setEnding(ending);
        }

        if (bandwidth != null) {
            for (VlanFixture f : c.getReserved().getCmp().getFixtures()) {
                f.setIngressBandwidth(bandwidth);
                f.setEgressBandwidth(bandwidth);
            }

            for (VlanPipe p : c.getReserved().getCmp().getPipes()) {
                p.setAzBandwidth(bandwidth);
                p.setZaBandwidth(bandwidth);
            }
        }

        Validity v = verifyModification(c);
        if (v.isValid()) {
            this.modifySchedule(c, beginning, ending);
            this.modifyBandwidth(c, bandwidth);
        }
        return v;
    }


    public int findAvailableMaxBandwidth(Connection c) {
        Interval interval = Interval.builder()
                .beginning(c.getReserved().getSchedule().getBeginning())
                .ending(c.getReserved().getSchedule().getEnding())
                .build();

        Map<String, PortBwVlan> availBwVlanMap = resvService.available(interval, c.getConnectionId());

        Set<String> portUrns = new HashSet<>();
        for (VlanFixture f : c.getReserved().getCmp().getFixtures()) {
            portUrns.add(f.getPortUrn());
        }

        for (VlanPipe p : c.getReserved().getCmp().getPipes()) {
            int i = 0;
            for (EroHop hop : p.getAzERO()) {
                if (i % 3 != 0) {
                    portUrns.add(hop.getUrn());
                }
                i++;
            }
        }

        int available = Integer.MAX_VALUE;
        for (String portUrn : portUrns) {
            if (availBwVlanMap.containsKey(portUrn)) {
                PortBwVlan availableForPort = availBwVlanMap.get(portUrn);
                if (availableForPort.getEgressBandwidth() < available) {
                    available = availableForPort.getEgressBandwidth();
                }
                if (availableForPort.getIngressBandwidth() < available) {
                    available = availableForPort.getIngressBandwidth();
                }
            }
        }
        return available;

    }

    public Validity verifyModification(Connection c) {
        try {
            return this.validate(connUtils.fromConnection(c, false), ConnectionMode.MODIFY);
        } catch (ConnException e) {
            throw new RuntimeException(e);
        }

    }


    @Transactional
    public ConnChangeResult commit(Connection c) throws NsoResvException, PCEException, ConnException {
        log.info("committing " + c.getConnectionId());
        ReentrantLock connLock = dbAccess.getConnLock();
        if (connLock.isLocked()) {
            log.debug("connection lock already locked; will need to wait to complete commit");
        }
        connLock.lock();

        Held h = c.getHeld();

        if (!c.getPhase().equals(Phase.HELD)) {
            connLock.unlock();
            throw new PCEException("Connection not in HELD phase " + c.getConnectionId());

        }
        if (h == null) {
            connLock.unlock();
            throw new PCEException("Null held " + c.getConnectionId());
        }


        Validity v = this.validateCommit(c);
        if (!v.isValid()) {
            connLock.unlock();
            throw new ConnException("Invalid connection for commit; errors follow: \n" + v.getMessage());
        }


        try {
            // log.debug("got connection lock ");
            c.setPhase(Phase.RESERVED);
            c.setArchived(null);

            Connection afterArchiveDeleted = connRepo.save(c);

            // try and delete any previous archived stuff that might exist
            archivedRepo.findByConnectionId(afterArchiveDeleted.getConnectionId()).ifPresent(archivedRepo::delete);

            reservedFromHeld(afterArchiveDeleted);
            archiveFromReserved(afterArchiveDeleted);

            afterArchiveDeleted.setHeld(null);

            // try and delete any held components that might still be around
            heldRepo.findByConnectionId(afterArchiveDeleted.getConnectionId()).ifPresent(heldRepo::delete);

            afterArchiveDeleted.setDeploymentState(DeploymentState.UNDEPLOYED);
            afterArchiveDeleted.setDeploymentIntent(DeploymentIntent.SHOULD_BE_UNDEPLOYED);
            Instant instant = Instant.now();
            afterArchiveDeleted.setLast_modified((int) instant.getEpochSecond());

            dumpDebug("afterArchiveDeleted", afterArchiveDeleted);

            Connection beforeNsoReserve = connRepo.saveAndFlush(afterArchiveDeleted);
            nsoResourceService.reserve(beforeNsoReserve);


        } finally {
            // log.debug("unlocked connections");
            connLock.unlock();
        }

        // TODO: set the user
        Event ev = Event.builder()
                .connectionId(c.getConnectionId())
                .description("committed")
                .type(EventType.COMMITTED)
                .occurrence(Instant.now())
                .username("")
                .build();
        logService.logEvent(c.getConnectionId(), ev);
        return ConnChangeResult.builder()
                .what(ConnChange.COMMITTED)
                .phase(Phase.RESERVED)
                .when(Instant.now())
                .build();
    }


    @Transactional
    public ConnChangeResult release(Connection c) {
        // if it is HELD or DESIGN, just delete it
        if (c.getPhase().equals(Phase.HELD) || c.getPhase().equals(Phase.DESIGN)) {
            log.info("deleting a HELD / DESIGN connection during release " + c.getConnectionId());
            connRepo.delete(c);
            connRepo.flush();
            return ConnChangeResult.builder()
                    .what(ConnChange.DELETED)
                    .when(Instant.now())
                    .build();
        }
        // if it is ARCHIVED , nothing to do
        if (c.getPhase().equals(Phase.ARCHIVED)) {
            return ConnChangeResult.builder()
                    .what(ConnChange.ARCHIVED)
                    .when(Instant.now())
                    .build();
        }

        if (c.getPhase().equals(Phase.RESERVED)) {
            if (c.getReserved().getSchedule().getBeginning().isAfter(Instant.now())) {
                // we haven't started yet; can delete without consequence
                log.info("deleting unstarted connection during release" + c.getConnectionId());
                connRepo.delete(c);
                Event ev = Event.builder()
                        .connectionId(c.getConnectionId())
                        .description("released (unstarted)")
                        .type(EventType.RELEASED)
                        .occurrence(Instant.now())
                        .username("system")
                        .build();
                logService.logEvent(c.getConnectionId(), ev);

                return ConnChangeResult.builder()
                        .what(ConnChange.DELETED)
                        .when(Instant.now())
                        .build();
            }
            if (c.getState().equals(State.ACTIVE) || c.getDeploymentState().equals(DeploymentState.DEPLOYED)) {

                log.info("Releasing active connection: " + c.getConnectionId());

                Event ev = Event.builder()
                        .connectionId(c.getConnectionId())
                        .description("released (active)")
                        .type(EventType.RELEASED)
                        .occurrence(Instant.now())
                        .username("system")
                        .build();
                logService.logEvent(c.getConnectionId(), ev);


            } else {
                log.info("Releasing a non-active connection: " + c.getConnectionId());
                Event ev = Event.builder()
                        .connectionId(c.getConnectionId())
                        .description("released (inactive)")
                        .type(EventType.RELEASED)
                        .occurrence(Instant.now())
                        .username("system")
                        .build();
                logService.logEvent(c.getConnectionId(), ev);

            }
        }


        // TODO: somehow set the user that cancelled
        Event ev = Event.builder()
                .connectionId(c.getConnectionId())
                .description("cancelled")
                .type(EventType.CANCELLED)
                .occurrence(Instant.now())
                .username("")
                .build();
        logService.logEvent(c.getConnectionId(), ev);

        ReentrantLock connLock = dbAccess.getConnLock();
        if (connLock.isLocked()) {
            log.debug("connection lock already locked; will need to wait to complete cancel");
        }
        connLock.lock();
        try {
            // log.debug("got connection lock ");
            // then, archive it
            c.setPhase(Phase.ARCHIVED);
            c.setDeploymentIntent(DeploymentIntent.SHOULD_BE_UNDEPLOYED);
            c.setHeld(null);
            c.setReserved(null);

            Instant instant = Instant.now();
            c.setLast_modified((int) instant.getEpochSecond());

            connRepo.saveAndFlush(c);

        } finally {
            // log.debug("unlocked connections");
            connLock.unlock();
        }


        return ConnChangeResult.builder()
                .what(ConnChange.ARCHIVED)
                .when(Instant.now())
                .build();
    }


    public Validity validateCommit(Connection in) throws ConnException {

        Validity v = this.validate(connUtils.fromConnection(in, false), ConnectionMode.NEW);

        StringBuilder error = new StringBuilder(v.getMessage());
        boolean valid = v.isValid();


        List<VlanFixture> fixtures = in.getHeld().getCmp().getFixtures();
        if (fixtures.size() < 2) {
            valid = false;
            error.append("Fixtures size is ").append(fixtures.size()).append(" ; minimum is 2");
        }

        List<VlanJunction> junctions = in.getHeld().getCmp().getJunctions();
        if (junctions.isEmpty()) {
            valid = false;
            error.append("No junctions; minimum is 1");
        }

        if (valid) {
            boolean graphValid = true;
            Multigraph<String, DefaultEdge> graph = new Multigraph<>(DefaultEdge.class);
            for (VlanJunction j : junctions) {
                graph.addVertex(j.getDeviceUrn());
            }

            List<VlanPipe> pipes = in.getHeld().getCmp().getPipes();

            for (VlanPipe pipe : pipes) {
                boolean pipeValid = true;
                if (!graph.containsVertex(pipe.getA().getDeviceUrn())) {
                    pipeValid = false;
                    error.append("invalid pipe A entry: ").append(pipe.getA().getDeviceUrn()).append("\n");
                }
                if (!graph.containsVertex(pipe.getZ().getDeviceUrn())) {
                    pipeValid = false;
                    graphValid = false;
                    error.append("invalid pipe Z entry: ").append(pipe.getZ().getDeviceUrn()).append("\n");
                }
                if (pipeValid) {
                    graph.addEdge(pipe.getA().getDeviceUrn(), pipe.getZ().getDeviceUrn());
                }
            }

            for (VlanFixture f : fixtures) {
                if (!graph.containsVertex(f.getJunction().getDeviceUrn())) {
                    graphValid = false;
                    error.append("invalid fixture junction entry: ").append(f.getJunction().getDeviceUrn()).append("\n");
                } else {
                    graph.addVertex(f.getPortUrn());
                    graph.addEdge(f.getJunction().getDeviceUrn(), f.getPortUrn());
                }
            }

            ConnectivityInspector<String, DefaultEdge> inspector = new ConnectivityInspector<>(graph);
            if (!inspector.isConnected()) {
                error.append("fixture / junction / pipe graph is unconnected\n");
                graphValid = false;
            }
            valid = graphValid;
        }

        v.setMessage(error.toString());
        v.setValid(valid);

        return v;

    }


    public Validity validate(SimpleConnection in, ConnectionMode mode)
            throws ConnException {

        DevelUtils.dumpDebug("validate conn", in);

        StringBuilder error = new StringBuilder();
        boolean valid = true;
        if (in == null) {
            throw new ConnException("null connection");
        }

        // validate global connection params
        if (mode.equals(ConnectionMode.NEW)) {
            // check the connection ID:
            String connectionId = in.getConnectionId();
            if (connectionId == null) {
                error.append("null connection id\n");
                valid = false;
            } else {
                if (!connectionId.matches("^[a-zA-Z][a-zA-Z0-9_\\-]+$")) {
                    error.append("connection id invalid format \n");
                    valid = false;
                }
                if (connectionId.length() > 12) {
                    error.append("connection id too long\n");
                    valid = false;
                } else if (connectionId.length() < 4) {
                    error.append("connection id too short\n");
                    valid = false;
                }
            }
            // check the connection MTU
            if (in.getConnection_mtu() != null) {
                if (in.getConnection_mtu() < minMtu || in.getConnection_mtu() > maxMtu) {
                    error.append("MTU must be between ").append(minMtu).append(" and ").append(maxMtu).append(" (inclusive)\n");
                    valid = false;
                }
            } else {
                in.setConnection_mtu(defaultMtu);
            }

            // description:
            if (in.getDescription() == null) {
                error.append("null description\n");
                valid = false;
            }
        }


        // validate schedule
        Instant begin;
        boolean beginValid;
        // check the schedule, begin time first:
        if (in.getBegin() == null) {
            beginValid = false;
            begin = Instant.MAX;
            error.append("null begin field\n");
        } else {
            begin = Instant.ofEpochSecond(in.getBegin());
            Instant rejectBefore = Instant.now().minus(5, ChronoUnit.MINUTES);
            if (begin.isBefore(rejectBefore) && !mode.equals(ConnectionMode.MODIFY)) {
                beginValid = false;
                error.append("Begin time is more than 5 minutes in the past\n");
            } else {
                // if we are set to start to up to +30 sec from now,
                // we (silently) modify the begin timestamp and we
                // set it to +30 secs from now()
                beginValid = true;
                Instant earliestPossible = Instant.now().plus(30, ChronoUnit.SECONDS);
                if (!begin.isAfter(earliestPossible)) {
                    begin = earliestPossible;
                    in.setBegin(Long.valueOf(begin.getEpochSecond()).intValue());
                }
            }
        }

        Instant end;
        boolean endValid;
        // check the schedule, end time:
        if (in.getEnd() == null) {
            endValid = false;
            end = Instant.MIN;
            error.append("null end field\n");
        } else {
            end = Instant.ofEpochSecond(in.getEnd());
            if (!end.isAfter(Instant.now())) {
                endValid = false;
                error.append("end date is in the past\n");
            } else if (!end.isAfter(begin)) {
                endValid = false;
                error.append("end date not past begin()\n");
            } else {
                if (begin.plus(this.minDuration, ChronoUnit.MINUTES).isAfter(end)) {
                    endValid = false;
                    error.append("duration is too short (less than ").append(this.minDuration).append(" min)\n");
                } else {
                    endValid = true;
                }
            }
        }

        boolean validInterval = beginValid && endValid;
        if (!validInterval) {
            valid = false;
        }

        // we can only check resource availability if the schedule makes sense..
        if (validInterval) {
            Interval interval = Interval.builder()
                    .beginning(begin)
                    .ending(end)
                    .build();

            if (in.getFixtures() == null) {
                in.setFixtures(new ArrayList<>());
            }
            if (in.getPipes() == null) {
                in.setPipes(new ArrayList<>());
            }
            if (in.getJunctions() == null) {
                in.setJunctions(new ArrayList<>());

            }

            Map<String, PortBwVlan> availBwVlanMap = resvService.available(interval, in.getConnectionId());

            // make maps: urn -> total of what we are requesting to reserve for VLANs and BW
            Map<String, ImmutablePair<Integer, Integer>> inBwMap = new HashMap<>();
            Map<String, Set<Integer>> inVlanMap = new HashMap<>();

            // populate the maps with what we request thru fixtures
            for (Fixture f : in.getFixtures()) {
                Integer inMbps = f.getInMbps();
                Integer outMbps = f.getOutMbps();
                if (f.getMbps() != null) {
                    inMbps = f.getMbps();
                    outMbps = f.getMbps();
                }
                if (inBwMap.containsKey(f.getPort())) {
                    ImmutablePair<Integer, Integer> prevBw = inBwMap.get(f.getPort());
                    inMbps += prevBw.getLeft();
                    outMbps += prevBw.getRight();
                    ImmutablePair<Integer, Integer> newBw = new ImmutablePair<>(inMbps, outMbps);
                    inBwMap.put(f.getPort(), newBw);
                } else {
                    inBwMap.put(f.getPort(), new ImmutablePair<>(inMbps, outMbps));
                }
                Set<Integer> vlans = new HashSet<>();
                if (inVlanMap.containsKey(f.getPort())) {
                    vlans = inVlanMap.get(f.getPort());
                }
                if (vlans.contains(f.getVlan())) {
                    error.append("VLAN ").append(f.getVlan()).append(" requested twice on ").append(f.getPort());
                    valid = false;
                } else {
                    vlans.add(f.getVlan());
                }
                inVlanMap.put(f.getPort(), vlans);
            }

            // populate the maps with what we request thru pipes (bw only)
            for (Pipe p : in.getPipes()) {
                Integer azMbps = p.getAzMbps();
                Integer zaMbps = p.getZaMbps();
                if (p.getMbps() != null) {
                    azMbps = p.getMbps();
                    zaMbps = p.getMbps();
                }
                int i = 0;
                for (String urn : p.getEro()) {
                    // egress for a-z, ingress for z-a
                    Integer egr = azMbps;
                    Integer ing = zaMbps;
                    boolean notDevice = false;
                    if (i % 3 == 1) {
                        notDevice = true;
                    } else if (i % 3 == 2) {
                        ing = azMbps;
                        egr = zaMbps;
                        notDevice = true;
                    }
                    if (notDevice) {
                        if (inBwMap.containsKey(urn)) {
                            ImmutablePair<Integer, Integer> prevBw = inBwMap.get(urn);
                            ImmutablePair<Integer, Integer> newBw =
                                    ImmutablePair.of(ing + prevBw.getLeft(), egr + prevBw.getRight());
                            inBwMap.put(urn, newBw);
                        } else {
                            ImmutablePair<Integer, Integer> newBw = ImmutablePair.of(ing, egr);
                            inBwMap.put(urn, newBw);
                        }
                    }
                    i++;
                }

            }

            // compare VLAN maps to what is available
            for (Fixture f : in.getFixtures()) {
                Validity fv = Validity.builder()
                        .valid(true)
                        .message("")
                        .build();

                StringBuilder ferror = new StringBuilder();

                if (availBwVlanMap.containsKey(f.getPort())) {
                    PortBwVlan avail = availBwVlanMap.get(f.getPort());
                    Set<Integer> vlans = inVlanMap.get(f.getPort());
                    if (vlans == null) {
                        vlans = new HashSet<>();
                    }

                    Set<IntRange> availVlanRanges = avail.getVlanRanges();
                    for (Integer vlan : vlans) {
                        boolean atLeastOneContains = false;
                        for (IntRange r : availVlanRanges) {
                            if (r.contains(vlan)) {
                                atLeastOneContains = true;
                            }
                        }
                        if (!atLeastOneContains) {
                            ferror.append(f.getPort()).append(" : vlan ").append(f.getVlan()).append(" not available\n");
                            error.append(ferror);
                            fv.setMessage(ferror.toString());
                            fv.setValid(false);
                            valid = false;
                        }
                        log.debug(f.getPort() + " vlan " + vlan + " contained in " + IntRange.asString(availVlanRanges) + " ? " + atLeastOneContains);
                    }
                } else {
                    fv.setValid(false);
                    fv.setMessage(f.getPort() + " not in topology\n");
                    error.append(fv.getMessage());
                    valid = false;
                }
                f.setValidity(fv);
            }

            Map<String, Validity> urnInBwValid = new HashMap<>();
            Map<String, Validity> urnEgBwValid = new HashMap<>();
            // compare map to what is available for BW

            for (String urn : inBwMap.keySet()) {
                PortBwVlan avail = availBwVlanMap.get(urn);

                if (avail == null) {
                    Validity bwValid = Validity.builder()
                            .valid(false)
                            .message(urn + " is not present anymore")
                            .build();
                    // error.append(err);
                    urnInBwValid.put(urn, bwValid);
                    urnEgBwValid.put(urn, bwValid);
                } else {
                    Validity inBwValid = Validity.builder()
                            .valid(true)
                            .message("")
                            .build();
                    ImmutablePair<Integer, Integer> inBw = inBwMap.get(urn);
                    if (avail.getIngressBandwidth() < inBw.getLeft()) {
                        StringBuilder inErr = new StringBuilder("total port ingress bw exceeds available: ")
                                .append(urn).append(" ").append(inBw.getLeft()).append("(req) / ")
                                .append(avail.getIngressBandwidth()).append(" (avail)\n");

                        valid = false;
                        error.append(inErr);

                        inBwValid.setMessage(inErr.toString());
                        inBwValid.setValid(false);
                    }
                    urnInBwValid.put(urn, inBwValid);

                    Validity egBwValid = Validity.builder()
                            .valid(true)
                            .message("")
                            .build();
                    if (avail.getEgressBandwidth() < inBw.getRight()) {
                        StringBuilder egErr = new StringBuilder("total port egress bw exceeds available: ")
                                .append(urn).append(" ").append(inBw.getLeft()).append("(req) / ")
                                .append(avail.getIngressBandwidth()).append(" (avail)\n");

                        valid = false;
                        error.append(egErr);

                        egBwValid.setMessage(egErr.toString());
                        egBwValid.setValid(false);
                    }
                    urnEgBwValid.put(urn, egBwValid);
                }
            }

            // populate Validity for fixtures
            for (Fixture f : in.getFixtures()) {
                Validity inBwValid = urnInBwValid.get(f.getPort());

                if (!inBwValid.isValid()) {
                    f.getValidity().setMessage(f.getValidity().getMessage() + inBwValid.getMessage());
                    f.getValidity().setValid(false);
                    valid = false;
                }

                Validity egBwValid = urnEgBwValid.get(f.getPort());
                if (!egBwValid.isValid()) {
                    f.getValidity().setValid(false);
                    f.getValidity().setMessage(f.getValidity().getMessage() + egBwValid.getMessage());
                    valid = false;
                }
            }

            // populate Validity for pipes & EROs
            for (Pipe p : in.getPipes()) {
                Validity pv = Validity.builder().valid(true).message("").build();

                Map<String, Validity> eroValidity = new HashMap<>();

                int i = 0;
                for (String urn : p.getEro()) {
                    boolean notDevice = false;
                    if (i % 3 == 1) {
                        notDevice = true;
                    } else if (i % 3 == 2) {
                        notDevice = true;
                    }
                    if (notDevice) {
                        Validity hopV = Validity.builder()
                                .message("")
                                .valid(true)
                                .build();

                        Validity inBwValid = urnInBwValid.get(urn);
                        if (!inBwValid.isValid()) {
                            hopV.setMessage(inBwValid.getMessage());
                            hopV.setValid(false);
                            pv.setMessage(pv.getMessage() + inBwValid.getMessage());
                            pv.setValid(false);
                            valid = false;
                        }

                        Validity egBwValid = urnInBwValid.get(urn);
                        if (!egBwValid.isValid()) {
                            hopV.setMessage(hopV.getMessage() + inBwValid.getMessage());
                            hopV.setValid(false);
                            pv.setMessage(pv.getMessage() + inBwValid.getMessage());
                            pv.setValid(false);
                            valid = false;
                        }
                        eroValidity.put(urn, hopV);
                    }
                    i++;
                }
                p.setValidity(pv);
                p.setEroValidity(eroValidity);
            }
        } else {
            error.append("invalid interval! VLANs and bandwidths not checked\n");
        }

        return Validity.builder()
                .message(error.toString())
                .valid(valid)
                .build();

        /*
        try {
            String pretty = jacksonObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(v);
            // log.info(pretty);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
         */


    }


    public Connection findConnection(String connectionId) {
        if (connectionId == null || connectionId.isEmpty()) {
            throw new IllegalArgumentException("Null or empty connectionId");
        }
//        log.info("looking for connectionId "+ connectionId);
        Optional<Connection> cOpt = connRepo.findByConnectionId(connectionId);
        if (cOpt.isPresent()) {

            Connection c = cOpt.get();
            c.setSouthbound(this.southbound(c.getConnectionId()));
            return c;
        } else {
            throw new NoSuchElementException("connection not found for id " + connectionId);

        }
    }

    public Connection toNewConnection(SimpleConnection in) {
        Connection c = Connection.builder()
                .mode(BuildMode.AUTOMATIC)
                .deploymentIntent(DeploymentIntent.SHOULD_BE_DEPLOYED)
                .deploymentState(DeploymentState.UNDEPLOYED)
                .phase(Phase.HELD)
                .description("")
                .username("")
                .last_modified((int) Instant.now().getEpochSecond())
                .connectionId(in.getConnectionId())
                .state(State.WAITING)
                .connection_mtu(in.getConnection_mtu())
                .build();
        updateConnection(in, c);

        return c;
    }
    private void dumpDebug(String context, Object o) {
        String pretty = null;

        try {
            pretty = (new ObjectMapper())
                    .registerModule(new JavaTimeModule())
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(o);
        } catch (JsonProcessingException ex) {
            log.error(ex.getMessage());
        }

        log.info(context + "\n" + pretty);
    }

}
