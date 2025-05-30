package net.es.oscars.resv.svc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PCEException;
import net.es.oscars.app.util.DbAccess;
import net.es.oscars.app.util.PrettyPrinter;
import net.es.oscars.model.Interval;
import net.es.oscars.resv.svc.comparisons.ConnServiceBWtoAvailableCompare;
import net.es.oscars.resv.svc.comparisons.ConnServiceVlanToAvailableCompare;
import net.es.oscars.resv.svc.populators.ConnServiceFixtureRequestPopulate;
import net.es.oscars.resv.svc.populators.ConnServiceFixtureValidityPopulate;
import net.es.oscars.resv.svc.populators.ConnServicePipeAndEroValidityPopulate;
import net.es.oscars.resv.svc.populators.ConnServicePipeRequestPopulate;
import net.es.oscars.resv.svc.validators.ConnServiceGlobalConnectionValidate;
import net.es.oscars.resv.svc.validators.ConnServiceScheduleValidate;
import net.es.oscars.sb.db.RouterCommandsRepository;
import net.es.oscars.sb.ent.RouterCommands;
import net.es.oscars.sb.nso.resv.NsoResourceService;
import net.es.oscars.sb.nso.resv.NsoResvException;
import net.es.oscars.resv.db.*;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.*;
import net.es.oscars.topo.beans.PortBwVlan;
import net.es.oscars.web.beans.*;
import net.es.oscars.web.simple.*;
import net.es.topo.common.devel.DevelUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.Multigraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.*;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.es.oscars.app.util.PrettyPrinter.prettyLog;
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

    @Value("${resv.timeout}")
    private Integer resvTimeout;


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

    private Map<String, Connection> held = new HashMap<>();

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
            connRepo.save(c);
        }

        log.info("modify bandwidth completed");
    }

    public int findAvailableMaxBandwidth(Connection c) {
        Interval interval = Interval.builder()
                .beginning(c.getReserved().getSchedule().getBeginning())
                .ending(c.getReserved().getSchedule().getEnding())
                .build();

        Map<String, PortBwVlan> availBwVlanMap = resvService.available(interval, held, c.getConnectionId());

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
            log.info("removing from held " + c.getConnectionId());
            held.remove(c.getConnectionId());
            // log.debug("got connection lock ");
            c.setPhase(Phase.RESERVED);
            c.setArchived(null);
            DeploymentState deploymentState = DeploymentState.UNDEPLOYED;
            DeploymentIntent deploymentIntent = DeploymentIntent.SHOULD_BE_UNDEPLOYED;

            Optional<Connection> existing = connRepo.findByConnectionId(c.getConnectionId());
            boolean isModify = false;
            Long oldScheduleId = null;

            // previous fixture ids
            Map<String, Long> prevFixtureIds = new HashMap<>();

            if (existing.isPresent()) {
                isModify = true;
                log.info("deleting from db previous " + existing.get().getConnectionId());

                for (VlanFixture vf : existing.get().getReserved().getCmp().getFixtures()) {
                    prevFixtureIds.put(vf.urn(), vf.getId());
                    log.info("previous fixture id for " + vf.urn() + " : " + vf.getId());

                }
                if (existing.get().getDeploymentState().equals(DeploymentState.DEPLOYED)) {
                    deploymentState = DeploymentState.DEPLOYED;
                    deploymentIntent = DeploymentIntent.SHOULD_BE_REDEPLOYED;
                }

                PrettyPrinter.prettyLog(prevFixtureIds);

                oldScheduleId = existing.get().getReserved().getSchedule().getId();
                connRepo.delete(existing.get());
            }

            reservedFromHeld(c);
            archiveFromReserved(c);
            c.setHeld(null);
            c.setDeploymentState(deploymentState);
            c.setDeploymentIntent(deploymentIntent);
            c.setLast_modified((int) Instant.now().getEpochSecond());

            log.info("saving updated connection to db " + c.getConnectionId());
            connRepo.saveAndFlush(c);
            PrettyPrinter.prettyLog(c);

            if (!isModify) {
                nsoResourceService.reserve(c);
            } else {
                Map<Long, Long> fixtureIdMap = new HashMap<>();
                for (VlanFixture vf : c.getReserved().getCmp().getFixtures()) {

                    Long prevFixtureId = prevFixtureIds.get(vf.urn());
                    Long newFixtureId = vf.getId();
                    fixtureIdMap.put(prevFixtureId, newFixtureId);
                    log.info("new fixture id for " + vf.urn() + " : " + vf.getId());
                }
                log.info("migrating NSO resources for " + c.getConnectionId());

                PrettyPrinter.prettyLog(fixtureIdMap);

                Long newScheduleId = c.getReserved().getSchedule().getId();
                nsoResourceService.migrate(newScheduleId, oldScheduleId, fixtureIdMap);
            }


            log.info("committed " + c.getConnectionId());


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

    public ConnChangeResult unhold(String connectionId) {
        if (held.containsKey(connectionId)) {
            log.info("unholding " + connectionId);
        }
        this.held.remove(connectionId);
        return ConnChangeResult.builder()
                .what(ConnChange.DELETED)
                .when(Instant.now())
                .build();

    }

    @Transactional
    public ConnChangeResult release(Connection c) {
        // if it is ARCHIVED , nothing to do
        if (c.getPhase().equals(Phase.ARCHIVED)) {
            return ConnChangeResult.builder()
                    .what(ConnChange.ARCHIVED)
                    .when(Instant.now())
                    .build();
        } else if (c.getPhase().equals(Phase.HELD)) {

            // un-hold if it is held..
            log.info("un-holding " + c.getConnectionId());
            this.unhold(c.getConnectionId());

            // it might have a RESERVED component; in that case we re-assign c and fall through to the next section
            if (connRepo.findByConnectionId(c.getConnectionId()).isPresent()) {
                c = connRepo.findByConnectionId(c.getConnectionId()).get();
            } else {
                return ConnChangeResult.builder()
                        .what(ConnChange.DELETED)
                        .when(Instant.now())
                        .build();
            }
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

    /**
     * Validates a SimpleConnection object against global connection parameters.
     * Validation routine will validate:
     *  - Global connection parameters
     *  - Scheduled start and end times.
     *  - Resource availability, assuming the schedule is valid.
     * @param inConn SimpleConnection object.
     * @param mode ConnectionMode enumeration value.
     * @return net.es.oscars.web.simple.Validity object.
     * @throws ConnException May throw an exception of type ConnException
     */
    public Validity validate(SimpleConnection inConn, ConnectionMode mode)
            throws ConnException {

        DevelUtils.dumpDebug("validate conn", inConn);

        StringBuilder error = new StringBuilder();
        boolean valid = false;

        if (inConn == null) {
            throw new ConnException("null connection");
        }

        Errors errorsConnection = new BeanPropertyBindingResult(inConn, "inConn");
        Errors errorsSchedule = new BeanPropertyBindingResult(inConn, "inConn");
        // validate global connection params BEGIN
        ConnServiceGlobalConnectionValidate validateGlobalConnection = new ConnServiceGlobalConnectionValidate(
            minMtu,
            maxMtu
        );
        validateGlobalConnection.validate(inConn, errorsConnection);
        boolean validGlobalConnection = validateGlobalConnection.valid() && !errorsConnection.hasErrors();
        if (errorsConnection.hasErrors()) {
            errorsConnection.getAllErrors().forEach((ObjectError oe) -> {
                log.error(oe.getDefaultMessage());
            });
        }
        // validate global connection params END


        // validate schedule BEGIN
        boolean validInterval = false;
        ConnServiceScheduleValidate validateSchedule = new ConnServiceScheduleValidate(
            mode,
            this.minDuration
        );

        if (validGlobalConnection) {
            validateSchedule.validate(inConn, errorsSchedule);
            validInterval = validateSchedule.valid() && !errorsSchedule.hasErrors();

            if (errorsSchedule.hasErrors()) {
                errorsSchedule.getAllErrors().forEach((ObjectError oe) -> {
                    log.error(oe.getDefaultMessage());
                });
            }
        }
        // validate schedule END

        // we can only check resource availability if the schedule makes sense.
        if (validInterval) {
            Instant begin = validateSchedule.getCheckedBeginTime();
            Instant end = validateSchedule.getCheckedEndTime();

            // Use the begin and end time to create an interval object.
            Interval interval = Interval.builder()
                    .beginning(begin)
                    .ending(end)
                    .build();

            // Assert we at least have an empty list of fixtures, pipes, and junctions
            if (inConn.getFixtures() == null)   inConn.setFixtures(new ArrayList<>());
            if (inConn.getPipes() == null)      inConn.setPipes(new ArrayList<>());
            if (inConn.getJunctions() == null)  inConn.setJunctions(new ArrayList<>());

            // Get the available BW VLAN map
            Map<String, PortBwVlan> availBwVlanMap = resvService.available(
                interval,
                held,
                inConn.getConnectionId()
            );

            // make maps: urn -> total of what we are requesting to reserve for VLANs and BW
            Map<String, ImmutablePair<Integer, Integer>> inBwMap = new HashMap<>();
            Map<String, Set<Integer>> inVlanMap = new HashMap<>();


            // populate the maps with what we request through fixtures BEGIN
            ConnServiceFixtureRequestPopulate fixtures = new ConnServiceFixtureRequestPopulate(
                inConn.getFixtures(),
                inBwMap,
                inVlanMap
            );
            // ... Run the populate() method.
            fixtures.populate();

            // ... A feature may or may not have been valid, populate our maps with whatever is valid.
            inBwMap = fixtures.getInBwMap();
            inVlanMap = fixtures.getInVlanMap();

            // populate the maps with what we request through fixtures END

            // populate the maps with what we request through pipes (bw only) BEGIN
            ConnServicePipeRequestPopulate pipes = new ConnServicePipeRequestPopulate(
                inConn.getPipes(),
                inBwMap
            );
            // ... Run the populate() method.
            pipes.populate();

            // ... Our BW map should have been updated
            inBwMap = fixtures.getInBwMap();
            // populate the maps with what we request through pipes (bw only) END

            // compare VLAN maps to what is available BEGIN
            ConnServiceVlanToAvailableCompare compareVlanToAvailable = new ConnServiceVlanToAvailableCompare(
                inConn.getFixtures(),
                availBwVlanMap,
                inVlanMap
            );
            // ... Run the compare() method.
            compareVlanToAvailable.compare();

            inConn.setFixtures(compareVlanToAvailable.getSourceList());
            // compare VLAN maps to what is available END

            Map<String, Validity> urnInBwValid = new HashMap<>();
            Map<String, Validity> urnEgBwValid = new HashMap<>();

            // compare map to what is available for BW BEGIN
            ConnServiceBWtoAvailableCompare compareBWtoAvailable = new ConnServiceBWtoAvailableCompare(
                inBwMap,
                availBwVlanMap,
                urnInBwValid,
                urnEgBwValid
            );
            // ... Run the compare() method.
            compareBWtoAvailable.compare();

            urnInBwValid = compareBWtoAvailable.getUrnInBwValid();
            urnEgBwValid = compareBWtoAvailable.getUrnEgBwValid();
            // compare map to what is available for BW END

            // populate Validity for fixtures BEGIN
            ConnServiceFixtureValidityPopulate fixturesValidPopulate = new ConnServiceFixtureValidityPopulate(
                inConn.getFixtures(),
                urnInBwValid,
                urnEgBwValid
            );
            fixturesValidPopulate.populate();
            inConn.setFixtures(fixturesValidPopulate.getSourceList());
            // populate Validity for fixtures END

            // populate Validity for pipes & EROs BEGIN
            ConnServicePipeAndEroValidityPopulate connServicePipeAndEroValidityPopulate = new ConnServicePipeAndEroValidityPopulate(
              inConn.getPipes(),
              urnInBwValid
            );

            connServicePipeAndEroValidityPopulate.populate();
            inConn.setPipes(connServicePipeAndEroValidityPopulate.getSourceList());
            // populate Validity for pipes & EROs END

            // Check overall validity BEGIN
            boolean isFixturesValid = fixtures.isValid();
            boolean isPipesValid = pipes.isValid();
            boolean isCompareVlanToAvailableValid = compareVlanToAvailable.isValid();
            boolean isCompareBWtoAvailableValid = compareBWtoAvailable.isValid();
            boolean isFixturesValidPopulateValid = fixturesValidPopulate.isValid();
            boolean isConnServicePipeAndEroValidityPopulateValid = connServicePipeAndEroValidityPopulate.isValid();

            valid = isFixturesValid
                && isPipesValid
                && isCompareVlanToAvailableValid
                && isCompareBWtoAvailableValid
                && isFixturesValidPopulateValid
                && isConnServicePipeAndEroValidityPopulateValid;
            // Check overall validity END

            // Build our error string BEGIN
            // ... fixtures
            if (fixtures.hasErrors()) {
                Map<String, Errors> allErrors = fixtures.getAllErrors();
                error = ammendErrorStringBuilder(error, allErrors);
            }
            // ... pipes
            if (pipes.hasErrors()) {
                Map<String, Errors> allErrors = pipes.getAllErrors();
                error = ammendErrorStringBuilder(error, allErrors);
            }
            // ... compare VLANs to available
            if (compareVlanToAvailable.hasErrors()) {
                Map<String, Errors> allErrors = compareVlanToAvailable.getAllErrors();
                error = ammendErrorStringBuilder(error, allErrors);
            }
            // ... compare BW to available
            if (compareBWtoAvailable.hasErrors()) {
                Map<String, Errors> allErrors = compareBWtoAvailable.getAllErrors();
                error = ammendErrorStringBuilder(error, allErrors);
            }
            // ... populate with valid fixtures
            if (fixturesValidPopulate.hasErrors()) {
                Map<String, Errors> allErrors = fixturesValidPopulate.getAllErrors();
                error = ammendErrorStringBuilder(error, allErrors);
            }
            // ... populate with valid pipe and ero
            if (connServicePipeAndEroValidityPopulate.hasErrors()) {
                Map<String, Errors> allErrors = connServicePipeAndEroValidityPopulate.getAllErrors();
                error = ammendErrorStringBuilder(error, allErrors);
            }
            // Build our error string END
        } else {
            error.append("invalid interval! VLANs and bandwidths not checked\n");
        }

        return Validity.builder()
                .message(error.toString())
                .valid(valid)
                .build();
    }

    /**
     * Internal helper method to build an error message string for the Validity message property.
     * @param error StringBuilder error message string.
     * @param allErrors A map of Errors objects to append.
     * @return Returns the updated error StringBuilder string.
     */
    private StringBuilder ammendErrorStringBuilder(StringBuilder error, Map<String, Errors> allErrors) {
        for (String key : allErrors.keySet()) {
            Errors errors = allErrors.get(key);
            for (ObjectError objError : errors.getAllErrors()) {
                error.append(objError.getDefaultMessage() + "\n");
            }
        }
        return error;
    }


    public Optional<Connection> findConnection(String connectionId) {
        if (connectionId == null || connectionId.isEmpty()) {
            return Optional.empty();
        }

        if (held.containsKey(connectionId)) {
            return Optional.of(held.get(connectionId));
        }

//        log.info("looking for connectionId "+ connectionId);
        Optional<Connection> cOpt = connRepo.findByConnectionId(connectionId);
        if (cOpt.isPresent()) {

            Connection c = cOpt.get();
            c.setSouthbound(this.southbound(c.getConnectionId()));
            return Optional.of(c);
        } else {
            return Optional.empty();
        }
    }

    public Instant extendHold(String connectionId) throws NoSuchElementException {
        Instant expiration = Instant.now().plus(resvTimeout, ChronoUnit.SECONDS);
        if (held.containsKey(connectionId)) {
            held.get(connectionId).getHeld().setExpiration(expiration);
            return expiration;
        } else {
            throw new NoSuchElementException("connection not found for id " + connectionId);
        }
    }

    public void releaseHold(String connectionId) {
        if (held.containsKey(connectionId)) {
            log.info("releasing an existing hold for connection " + connectionId);
        }
        held.remove(connectionId);
    }

    public Pair<SimpleConnection, Connection> holdConnection(SimpleConnection in) throws ConnException {

        ReentrantLock connLock = dbAccess.getConnLock();
        if (connLock.isLocked()) {
            log.debug("connection lock already locked; returning from hold");
            return Pair.of(in, null);
        }
        connLock.lock();

        // maybe don't throw exception; populate all the Validity entries instead
        Validity v = this.validate(in, ConnectionMode.NEW);
        in.setValidity(v);

        if (!v.isValid()) {
            log.info("could not hold connection " + in.getConnectionId());
            log.info("reason: " + v.getMessage());
            connLock.unlock();
            return Pair.of(in, null);
        }

        // we can hold it, so we do
        Instant exp = Instant.now().plus(resvTimeout, ChronoUnit.SECONDS);
        long secs = exp.toEpochMilli() / 1000L;
        in.setHeldUntil((int) secs);


        String connectionId = in.getConnectionId();
        Connection c = simpleToHeldConnection(in);
        prettyLog(c);

        this.held.put(connectionId, c);

        connLock.unlock();
        return Pair.of(in, c);

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
