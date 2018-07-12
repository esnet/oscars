package net.es.oscars.resv.svc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PCEException;
import net.es.oscars.app.exc.PSSException;
import net.es.oscars.ext.SlackConnector;
import net.es.oscars.pss.svc.PSSAdapter;
import net.es.oscars.pss.svc.PssResourceService;
import net.es.oscars.resv.db.*;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.BuildMode;
import net.es.oscars.resv.enums.EventType;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.enums.State;
import net.es.oscars.topo.beans.IntRange;
import net.es.oscars.topo.beans.PortBwVlan;
import net.es.oscars.web.beans.ConnectionFilter;
import net.es.oscars.web.beans.Interval;
import net.es.oscars.web.simple.Fixture;
import net.es.oscars.web.simple.Pipe;
import net.es.oscars.web.simple.SimpleConnection;
import net.es.oscars.web.simple.Validity;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.Multigraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.IntStream;

@Service
@Slf4j
@Data
@Transactional
public class ConnService {

    @Autowired
    private ConnectionRepository connRepo;

    @Autowired
    private SlackConnector slack;

    @Autowired
    private LogService logService;

    @Autowired
    private ResvService resvService;

    @Autowired
    private PssResourceService pssResourceService;

    @Autowired
    private HeldRepository heldRepo;

    @Autowired
    private ScheduleRepository schRepo;

    @Autowired
    private ArchivedRepository archivedRepo;

    @Autowired
    private ReservedRepository reservedRepo;

    @Autowired
    private PSSAdapter pssAdapter;

    public String generateConnectionId() {
        boolean found = false;
        String result = "";
        while (!found) {
            String candidate = this.connectionIdGenerator();
            Optional<Connection> d = connRepo.findByConnectionId(candidate);
            if (!d.isPresent()) {
                found = true;
                result = candidate;
            }
        }
        return result;
    }


    public String connectionIdGenerator() {
        char[] FIRST_LETTER = "CDEFGHJKMNPRTWXYZ".toCharArray();
        char[] SAFE_ALPHABET = "234679CDFGHJKMNPRTWXYZ".toCharArray();

        Random random = new Random();

        StringBuilder b = new StringBuilder();
        Integer firstIdx = random.nextInt(FIRST_LETTER.length);
        char firstLetter = FIRST_LETTER[firstIdx];
        b.append(firstLetter);

        int max = SAFE_ALPHABET.length;
        int totalNumber = 3;
        IntStream stream = random.ints(totalNumber, 0, max);

        stream.forEach(i -> {
            b.append(SAFE_ALPHABET[i]);
        });
        return b.toString();

    }
    public List<Connection> filter(ConnectionFilter filter) {

        List<Connection> candidates = new ArrayList<>();
        // first we don't take into account anything that doesn't have any archived
        // i.e. we discount any temporarily held
        connRepo.findAll().forEach(c -> {
            if (c.getArchived() != null) {
                candidates.add(c);
            }
        });
        List<Connection> descFiltered = new ArrayList<>();
        if (filter.getDescription() != null) {
            for (Connection c: candidates) {
                if (c.getDescription().matches(filter.getDescription())) {
                    descFiltered.add(c);
                }
            }
        } else {
            descFiltered.addAll(candidates);
        }
        List<Connection> phaseFiltered = new ArrayList<>();
        if (filter.getPhase() != null) {
            for (Connection c: descFiltered) {
                if (c.getPhase().equals(filter.getPhase())) {
                    phaseFiltered.add(c);
                }
            }
        } else {
            phaseFiltered.addAll(descFiltered);
        }

        return phaseFiltered;

    }


    public Phase commit(Connection c) throws PSSException, PCEException {

        Held h = c.getHeld();

        if (!c.getPhase().equals(Phase.HELD)) {
            throw new PCEException("Connection not in HELD phase "+c.getConnectionId());

        }
        if (h == null) {
            throw new PCEException("Null held "+c.getConnectionId());
        }
        Boolean valid = true;

        slack.sendMessage("User "+c.getUsername()+" committed reservation "+c.getConnectionId());

        String error = "";
        Multigraph<String, DefaultEdge> graph = new Multigraph<>(DefaultEdge.class);
        for (VlanJunction j: h.getCmp().getJunctions()) {
            graph.addVertex(j.getDeviceUrn());
        }
        for (VlanPipe pipe: h.getCmp().getPipes()) {
            boolean pipeValid = true;
            if (!graph.containsVertex(pipe.getA().getDeviceUrn())) {
                pipeValid = false;
                error += "invalid pipe A entry: "+pipe.getA().getDeviceUrn()+"\n";
            }
            if (!graph.containsVertex(pipe.getZ().getDeviceUrn())) {
                pipeValid = false;
                valid = false;
                error += "invalid pipe Z entry: "+pipe.getZ().getDeviceUrn()+"\n";
            }
            if (pipeValid) {
                graph.addEdge(pipe.getA().getDeviceUrn(), pipe.getZ().getDeviceUrn());
            }
        }

        for (VlanFixture f: h.getCmp().getFixtures()) {
            if (!graph.containsVertex(f.getJunction().getDeviceUrn())) {
                valid = false;
                error += "invalid fixture junction entry: "+f.getJunction().getDeviceUrn()+"\n";
            } else {
                graph.addVertex(f.getPortUrn());
                graph.addEdge(f.getJunction().getDeviceUrn(), f.getPortUrn());
            }
        }

        ConnectivityInspector<String, DefaultEdge> inspector = new ConnectivityInspector<>(graph);
        if (!inspector.isConnected()) {
            error += "fixture / junction / pipe graph is unconnected\n";
            valid = false;
        }
        if (!valid) {
            throw new PCEException(error);
        }


        c.setPhase(Phase.RESERVED);

        this.reservedFromHeld(c);
        this.pssResourceService.reserve(c);

        this.archiveFromReserved(c);

        c.setHeld(null);
        connRepo.save(c);

        // TODO: set the user
        Event ev = Event.builder()
                .connectionId(c.getConnectionId())
                .description("committed")
                .type(EventType.COMMITTED)
                .at(Instant.now())
                .username("")
                .build();
        logService.logEvent(c.getConnectionId(), ev);
        return Phase.RESERVED;
    }





    public Phase uncommit(Connection c) {

        Held h = this.heldFromReserved(c);
        c.setReserved(null);
        c.setHeld(h);
        connRepo.save(c);
        return Phase.HELD;

    }

    public Phase cancel(Connection c) {
        // if it is HELD or DESIGN, delete it
        if (c.getPhase().equals(Phase.HELD) ||  c.getPhase().equals(Phase.DESIGN)) {
            connRepo.delete(c);
            return Phase.HELD;
        }
        // if it is ARCHIVED , nothing to do
        if (c.getPhase().equals(Phase.ARCHIVED)) {
            return c.getPhase();
        }
        if (c.getPhase().equals(Phase.RESERVED)) {
            if (c.getReserved().getSchedule().getBeginning().isAfter(Instant.now())) {
                // we haven't started yet; can delete without consequence
                connRepo.delete(c);
                return Phase.HELD;
            }
            if (c.getState().equals(State.ACTIVE)) {
                slack.sendMessage("Cancelling active reservation: " + c.getConnectionId());

                // need to dismantle first, that part relies on Reserved components
                try {
                    State s = pssAdapter.dismantle(c);
                    if (!s.equals(State.FAILED)) {
                        c.setState(State.FINISHED);
                    }
                } catch (PSSException ex) {
                    c.setState(State.FAILED);
                    log.error(ex.getMessage(), ex);
                }

            } else {
                slack.sendMessage("Cancelling non-active reservation: " + c.getConnectionId());
            }
        }

        // then, archive it
        c.setPhase(Phase.ARCHIVED);
        c.setHeld(null);
        c.setReserved(null);

        // TODO: somehow set the user that cancelled
        Event ev = Event.builder()
                .connectionId(c.getConnectionId())
                .description("cancelled")
                .type(EventType.CANCELLED)
                .at(Instant.now())
                .username("")
                .build();
        logService.logEvent(c.getConnectionId(), ev);


        connRepo.save(c);
        return Phase.ARCHIVED;
    }

    public void reservedFromHeld(Connection c) {

        Components cmp = c.getHeld().getCmp();
        Schedule resvSch = this.copySchedule(c.getHeld().getSchedule());
        resvSch.setPhase(Phase.RESERVED);


        Components resvCmp = this.copyComponents(cmp, resvSch);
        Reserved reserved = Reserved.builder()
                .cmp(resvCmp)
                .connectionId(c.getConnectionId())
                .schedule(resvSch)
                .build();
        c.setReserved(reserved);
    }

    public Held heldFromReserved(Connection c) {

        Components cmp = c.getReserved().getCmp();
        Schedule sch = this.copySchedule(c.getReserved().getSchedule());
        sch.setPhase(Phase.HELD);

        Instant exp = Instant.now().plus(15L, ChronoUnit.MINUTES);

        Components heldCmp = this.copyComponents(cmp, sch);
        return Held.builder()
                .cmp(heldCmp)
                .connectionId(c.getConnectionId())
                .schedule(sch)
                .expiration(exp)
                .build();
    }

    public void archiveFromReserved(Connection c) {
        Components cmp = c.getReserved().getCmp();
        Schedule sch = this.copySchedule(c.getReserved().getSchedule());
        sch.setPhase(Phase.ARCHIVED);

        Components archCmp = this.copyComponents(cmp, sch);
        Archived archived = Archived.builder()
                .cmp(archCmp)
                .connectionId(c.getConnectionId())
                .schedule(sch)
                .build();
        c.setArchived(archived);
    }



    private Schedule copySchedule(Schedule sch) {
        return Schedule.builder()
                .beginning(sch.getBeginning())
                .ending(sch.getEnding())
                .connectionId(sch.getConnectionId())
                .refId(sch.getRefId())
                .phase(sch.getPhase())
                .build();
    }

    private Components copyComponents(Components cmp, Schedule sch) {
        List<VlanJunction> junctions = new ArrayList<>();
        Map<String, VlanJunction> jmap = new HashMap<>();
        for (VlanJunction j : cmp.getJunctions()) {
            VlanJunction jc = VlanJunction.builder()
                    .commandParams(copyCommandParams(j.getCommandParams(), sch))
                    .deviceUrn(j.getDeviceUrn())
                    .vlan(copyVlan(j.getVlan(), sch))
                    .schedule(sch)
                    .refId(j.getRefId())
                    .connectionId(j.getConnectionId())
                    .deviceUrn(j.getDeviceUrn())
                    .build();
            jmap.put(j.getDeviceUrn(), jc);
            junctions.add(jc);
        }

        List<VlanFixture> fixtures = new ArrayList<>();
        for (VlanFixture f: cmp.getFixtures()) {
            VlanFixture fc = VlanFixture.builder()
                    .connectionId(f.getConnectionId())
                    .ingressBandwidth(f.getIngressBandwidth())
                    .egressBandwidth(f.getEgressBandwidth())
                    .schedule(sch)
                    .junction(jmap.get(f.getJunction().getDeviceUrn()))
                    .portUrn(f.getPortUrn())
                    .vlan(copyVlan(f.getVlan(), sch))
                    .commandParams(copyCommandParams(f.getCommandParams(), sch))
                    .build();
            fixtures.add(fc);
        }
        List<VlanPipe> pipes = new ArrayList<>();
        for (VlanPipe p : cmp.getPipes()) {
            VlanPipe pc = VlanPipe.builder()
                    .a(jmap.get(p.getA().getDeviceUrn()))
                    .z(jmap.get(p.getZ().getDeviceUrn()))
                    .azBandwidth(p.getAzBandwidth())
                    .zaBandwidth(p.getZaBandwidth())
                    .connectionId(p.getConnectionId())
                    .schedule(sch)
                    .azERO(copyEro(p.getAzERO()))
                    .zaERO(copyEro(p.getZaERO()))
                    .build();
            pipes.add(pc);
        }


        return Components.builder()
                .fixtures(fixtures)
                .junctions(junctions)
                .pipes(pipes)
                .build();
    }
    private List<EroHop> copyEro(List<EroHop> ero) {
        List<EroHop> res = new ArrayList<>();
        for (EroHop h : ero) {
            EroHop hc = EroHop.builder()
                    .urn(h.getUrn())
                    .build();
            res.add(hc);
        }

        return res;
    }

    private Set<CommandParam> copyCommandParams(Set<CommandParam> cps, Schedule sch) {
        Set<CommandParam> res = new HashSet<>();
        for (CommandParam cp: cps) {
            res.add(CommandParam.builder()
                    .connectionId(cp.getConnectionId())
                    .paramType(cp.getParamType())
                    .schedule(sch)
                    .resource(cp.getResource())
                    .refId(cp.getRefId())
                    .urn(cp.getUrn())
                    .build());
        }
        return res;
    }

    private Vlan copyVlan(Vlan v, Schedule sch) {
        if (v == null) {
            return null;
        }
        return Vlan.builder()
                .connectionId(v.getConnectionId())
                .schedule(sch)
                .urn(v.getUrn())
                .vlanId(v.getVlanId())
                .build();

    }

    public Validity validateConnection(SimpleConnection in)
            throws NoSuchElementException, IllegalArgumentException {

        String error = "";
        Boolean valid = true;
        Boolean validInterval = true;
        if (in == null) {
            throw new IllegalArgumentException("null connection");
        }
        Instant begin = Instant.now();
        Instant end;

        String connectionId = in.getConnectionId();
        if (connectionId == null || connectionId.equals("")) {
            error += "empty or null connection id\n";
            valid = false;
        }
        if (in.getBegin() == null) {
            error += "null begin field\n";
            valid = false;
            validInterval = false;
        } else {
            begin = Instant.ofEpochSecond(in.getBegin());
            if (!begin.isAfter(Instant.now())) {
                error += "begin date not past now()\n";
                valid = false;
                validInterval = false;
            }
        }

        if (in.getEnd() == null) {
            error += "null end field\n";
            valid = false;
            validInterval = false;
        } else {
            end = Instant.ofEpochSecond(in.getEnd());
            if (!end.isAfter(Instant.now())) {
                error += "end date not past now()\n";
                valid = false;
                validInterval = false;
            }
            if (!end.isAfter(begin)) {
                error += "end date not past begin()\n";
                valid = false;
                validInterval = false;
            }
        }
        if (validInterval) {
            begin = Instant.ofEpochSecond(in.getBegin());
            end = Instant.ofEpochSecond(in.getEnd());
            if (begin.plus(Duration.ofMinutes(15)).isAfter(end)) {
                valid = false;
                error += "interval is too short (less than 15 min)";
            }

        }
        if (in.getDescription() == null) {
            error += "null description\n";
            valid = false;
        }


        if (validInterval) {
            Interval interval = Interval.builder()
                    .beginning(begin)
                    .ending(Instant.ofEpochSecond(in.getEnd()))
                    .build();

            if (in.getFixtures() == null)  {
                in.setFixtures(new ArrayList<>());
            }
            if (in.getPipes() == null)  {
                in.setPipes(new ArrayList<>());
            }
            if (in.getJunctions() == null) {
                in.setJunctions(new ArrayList<>());

            }

            Map<String, PortBwVlan> availBwVlanMap = resvService.available(interval, in.getConnectionId());

            Map<String, ImmutablePair<Integer, Integer>> inBwMap = new HashMap<>();
            Map<String, Set<Integer>> inVlanMap = new HashMap<>();
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
                    error += "duplicate VLAN for "+f.getPort();
                    valid = false;
                } else {
                    vlans.add(f.getVlan());
                }
                inVlanMap.put(f.getPort(), vlans);
            }


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
                        ing = zaMbps;
                        egr = azMbps;
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

            for (Fixture f : in.getFixtures()) {
                PortBwVlan avail = availBwVlanMap.get(f.getPort());
                Set<Integer> vlans = inVlanMap.get(f.getPort());
                if (avail == null) {
                    avail = PortBwVlan.builder()
                            .egressBandwidth(-1)
                            .ingressBandwidth(-1)
                            .vlanExpression("")
                            .vlanRanges(new HashSet<>())
                            .build();
                }
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
                        error += "vlan not available: " + f.getJunction() + ":" + f.getPort() + "." + f.getVlan() + "\n";
                        valid = false;
                    }
                }

            }
            for (String urn : inBwMap.keySet()) {
                PortBwVlan avail = availBwVlanMap.get(urn);
                ImmutablePair<Integer, Integer> inBw = inBwMap.get(urn);
                if (avail.getIngressBandwidth() < inBw.getLeft()) {
                    error += "total port ingress bw exceeds available: " + urn
                            + " " + inBw.getLeft() + "(req) / " + avail.getIngressBandwidth() + " (avail)\n";
                    valid = false;

                }
                if (avail.getEgressBandwidth() < inBw.getRight()) {
                    error += "total port egress bw exceeds available: " + urn
                            + " " + inBw.getRight() + "(req) / " + avail.getEgressBandwidth() + " (avail)\n";
                    valid = false;
                }
            }


        } else {
            error += "invalid interval, VLANs and bandwidths not checked\n";
            valid = false;
        }

        Validity v = Validity.builder()
                .message(error)
                .valid(valid)
                .build();

        try {
            String pretty = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(v);
            // log.info(pretty);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return v;


    }

    public void updateConnection(SimpleConnection in, Connection c) throws IllegalArgumentException {
        log.debug("updating connection "+c.getConnectionId());
        if (!c.getPhase().equals(Phase.HELD)) {
            throw new IllegalArgumentException(c.getConnectionId() + " not in HELD phase");
        }
        c.setDescription(in.getDescription());
        c.setUsername(in.getUsername());
        c.setMode(in.getMode());
        c.setState(State.WAITING);
        if (in.getTags() != null && !in.getTags().isEmpty()) {
            if (c.getTags() == null) {
                c.setTags(new ArrayList<>());
            }
            c.getTags().clear();
            in.getTags().forEach(t -> {
                c.getTags().add(Tag.builder()
                        .category(t.getCategory())
                        .contents(  t.getContents())
                        .build());
            });
        }
        Schedule s = Schedule.builder()
                .connectionId(in.getConnectionId())
                .refId(in.getConnectionId() + "-sched")
                .phase(Phase.HELD)
                .beginning(Instant.ofEpochSecond(in.getBegin()))
                .ending(Instant.ofEpochSecond(in.getEnd()))
                .build();
        Components cmp = Components.builder()
                .fixtures(new ArrayList<>())
                .junctions(new ArrayList<>())
                .pipes(new ArrayList<>())
                .build();

        Map<String, VlanJunction> junctionMap = new HashMap<>();
        if (in.getJunctions() != null) {
            in.getJunctions().forEach(j -> {
                if (j.getValidity() == null || j.getValidity().isValid()) {
                    VlanJunction vj = VlanJunction.builder()
                            .schedule(s)
                            .refId(j.getDevice())
                            .connectionId(in.getConnectionId())
                            .deviceUrn(j.getDevice())
                            .build();
                    junctionMap.put(vj.getDeviceUrn(), vj);
                    cmp.getJunctions().add(vj);
                }
            });
        }
        if (in.getFixtures() != null) {
            for (Fixture f : in.getFixtures()) {
                if (f.getValidity() == null || f.getValidity().isValid()) {
                    Integer inMbps = f.getInMbps();
                    Integer outMbps = f.getOutMbps();
                    if (f.getMbps() != null) {
                        inMbps = f.getMbps();
                        outMbps = f.getMbps();
                    }
                    Vlan vlan = Vlan.builder()
                            .connectionId(in.getConnectionId())
                            .schedule(s)
                            .urn(f.getPort())
                            .vlanId(f.getVlan())
                            .build();
                    VlanFixture vf = VlanFixture.builder()
                            .junction(junctionMap.get(f.getJunction()))
                            .connectionId(in.getConnectionId())
                            .portUrn(f.getPort())
                            .ingressBandwidth(inMbps)
                            .egressBandwidth(outMbps)
                            .schedule(s)
                            .vlan(vlan)
                            .build();
                    cmp.getFixtures().add(vf);
                }
            }
        }
        if (in.getPipes() != null) {
            for (Pipe pipe : in.getPipes()) {
                if (pipe.getValidity() == null || pipe.getValidity().isValid()) {
                    VlanJunction aj = junctionMap.get(pipe.getA());
                    VlanJunction zj = junctionMap.get(pipe.getZ());
                    List<EroHop> azEro = new ArrayList<>();
                    List<EroHop> zaEro = new ArrayList<>();
                    for (String hop: pipe.getEro()) {
                        azEro.add(EroHop.builder()
                                .urn(hop)
                                .build());
                        zaEro.add(EroHop.builder()
                                .urn(hop)
                                .build());
                    }
                    Collections.reverse(zaEro);
                    Integer azMbps = pipe.getAzMbps();
                    Integer zaMbps = pipe.getZaMbps();
                    if (pipe.getMbps() != null) {
                        azMbps = pipe.getMbps();
                        zaMbps = pipe.getMbps();
                    }

                    VlanPipe vp = VlanPipe.builder()
                            .a(aj)
                            .z(zj)
                            .schedule(s)
                            .azBandwidth(azMbps)
                            .zaBandwidth(zaMbps)
                            .connectionId(in.getConnectionId())
                            .azERO(azEro)
                            .zaERO(zaEro)
                            .build();
                    cmp.getPipes().add(vp);
                }
            }
        }
        Instant expiration = Instant.ofEpochSecond(in.getHeldUntil());
        Held h = Held.builder()
                .connectionId(in.getConnectionId())
                .cmp(cmp)
                .schedule(s)
                .expiration(expiration)
                .build();

        if (c.getHeld() != null) {
            Held oldHeld = c.getHeld();
            oldHeld.setSchedule(s);
            oldHeld.setExpiration(expiration);
            oldHeld.setCmp(cmp);



            Schedule oldSchedule = oldHeld.getSchedule();

            oldHeld.setSchedule(s);
            oldHeld.setExpiration(expiration);
            oldHeld.setCmp(cmp);

            schRepo.delete(oldSchedule);
        } else {
            c.setHeld(h);
        }
    }

    public Connection toNewConnection(SimpleConnection in) {
        Connection c = Connection.builder()
                .mode(BuildMode.AUTOMATIC)
                .phase(Phase.HELD)
                .description("")
                .username("")
                .connectionId(in.getConnectionId())
                .state(State.WAITING)
                .build();
        this.updateConnection(in, c);

        return c;
    }


}
