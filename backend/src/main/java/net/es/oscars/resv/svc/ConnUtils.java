package net.es.oscars.resv.svc;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.enums.State;
import net.es.oscars.sb.nso.db.NsoVcIdDAO;
import net.es.oscars.web.simple.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.IntStream;


@Component
@Slf4j
public class ConnUtils {
    @Autowired
    private ConnectionRepository connRepo;

    @Autowired
    private NsoVcIdDAO nsoVcIdDAO;

    public String genUniqueConnectionId() {
        boolean found = false;
        String result = "";
        while (!found) {
            String candidate = connectionIdGenerator();
            Optional<Connection> d = connRepo.findByConnectionId(candidate);
            if (d.isEmpty()) {
                found = true;
                result = candidate;
            }
        }
        return result;
    }

    public static String connectionIdGenerator() {
        char[] FIRST_LETTER = "CDEFGHJKMNPRTWXYZ".toCharArray();
        char[] SAFE_ALPHABET = "234679CDFGHJKMNPRTWXYZ".toCharArray();

        Random random = new Random();

        StringBuilder b = new StringBuilder();
        int firstIdx = random.nextInt(FIRST_LETTER.length);
        char firstLetter = FIRST_LETTER[firstIdx];
        b.append(firstLetter);

        int max = SAFE_ALPHABET.length;
        int totalNumber = 3;
        IntStream stream = random.ints(totalNumber, 0, max);

        stream.forEach(i -> b.append(SAFE_ALPHABET[i]));
        return b.toString();

    }


    public static void reservedFromHeld(Connection c) {

        Components cmp = c.getHeld().getCmp();
        Schedule resvSch = copySchedule(c.getHeld().getSchedule());
        resvSch.setPhase(Phase.RESERVED);


        Components resvCmp = copyComponents(cmp, resvSch);
        Reserved reserved = Reserved.builder()
                .cmp(resvCmp)
                .connectionId(c.getConnectionId())
                .schedule(resvSch)
                .build();
        c.setReserved(reserved);
    }



    public static void archiveFromReserved(Connection c) {
        Components cmp = c.getReserved().getCmp();
        Schedule sch = copySchedule(c.getReserved().getSchedule());
        sch.setPhase(Phase.ARCHIVED);

        Components archCmp = copyComponents(cmp, sch);
        Archived archived = Archived.builder()
                .cmp(archCmp)
                .connectionId(c.getConnectionId())
                .schedule(sch)
                .build();
        c.setArchived(archived);
    }


    public static Schedule copySchedule(Schedule sch) {
        return Schedule.builder()
                .beginning(sch.getBeginning())
                .ending(sch.getEnding())
                .connectionId(sch.getConnectionId())
                .refId(sch.getRefId())
                .phase(sch.getPhase())
                .build();
    }

    public static Components copyComponents(Components cmp, Schedule sch) {
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
        for (VlanFixture f : cmp.getFixtures()) {
            VlanFixture fc = VlanFixture.builder()
                    .connectionId(f.getConnectionId())
                    .ingressBandwidth(f.getIngressBandwidth())
                    .egressBandwidth(f.getEgressBandwidth())
                    .schedule(sch)
                    .junction(jmap.get(f.getJunction().getDeviceUrn()))
                    .portUrn(f.getPortUrn())
                    .vlan(copyVlan(f.getVlan(), sch))
                    .strict(f.getStrict())
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
                    .protect(p.getProtect())
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

    public static List<EroHop> copyEro(List<EroHop> ero) {
        List<EroHop> res = new ArrayList<>();
        for (EroHop h : ero) {
            EroHop hc = EroHop.builder()
                    .urn(h.getUrn())
                    .build();
            res.add(hc);
        }

        return res;
    }

    public static Set<CommandParam> copyCommandParams(Set<CommandParam> cps, Schedule sch) {
        Set<CommandParam> res = new HashSet<>();
        for (CommandParam cp : cps) {
            res.add(CommandParam.builder()
                    .connectionId(cp.getConnectionId())
                    .paramType(cp.getParamType())
                    .schedule(sch)
                    .resource(cp.getResource())
                    .intent(cp.getIntent())
                    .target(cp.getTarget())
                    .refId(cp.getRefId())
                    .urn(cp.getUrn())
                    .build());
        }
        return res;
    }

    public static Vlan copyVlan(Vlan v, Schedule sch) {
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

    public static void updateConnection(SimpleConnection in, Connection c) throws IllegalArgumentException {
        log.debug("updating connection " + c.getConnectionId());
        if (!c.getPhase().equals(Phase.HELD)) {
            throw new IllegalArgumentException(c.getConnectionId() + " not in HELD phase");
        }
        c.setDescription(in.getDescription());
        c.setServiceId(in.getServiceId());
        c.setUsername(in.getUsername());
        c.setMode(in.getMode());

        if (in.getConnection_mtu() != null) {
            c.setConnection_mtu(in.getConnection_mtu());
        } else {
            c.setConnection_mtu(9000);
        }
        c.setState(State.WAITING);
        if (in.getTags() != null && !in.getTags().isEmpty()) {
            if (c.getTags() == null) {
                c.setTags(new ArrayList<>());
            }
            c.getTags().clear();
            in.getTags().forEach(t -> c.getTags().add(Tag.builder()
                    .category(t.getCategory())
                    .contents(t.getContents())
                    .build()));
        }

        Schedule s;
        if (c.getHeld() != null) {
            s = c.getHeld().getSchedule();
            s.setBeginning(Instant.ofEpochSecond(in.getBegin()));
            s.setEnding(Instant.ofEpochSecond(in.getEnd()));
        } else {
            s = Schedule.builder()
                    .connectionId(in.getConnectionId())
                    .refId(in.getConnectionId() + "-sched")
                    .phase(Phase.HELD)
                    .beginning(Instant.ofEpochSecond(in.getBegin()))
                    .ending(Instant.ofEpochSecond(in.getEnd()))
                    .build();
        }

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
                    Boolean strict = false;
                    if (f.getStrict() != null) {
                        strict = f.getStrict();
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
                            .strict(strict)
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
                    for (String hop : pipe.getEro()) {
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
                    Boolean protect = false;
                    if (pipe.getProtect() != null) {
                        protect = pipe.getProtect();
                    }

                    VlanPipe vp = VlanPipe.builder()
                            .a(aj)
                            .z(zj)
                            .protect(protect)
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

        if (c.getHeld() != null) {
            Held oldHeld = c.getHeld();
            oldHeld.setSchedule(s);
            oldHeld.setExpiration(expiration);
            oldHeld.setCmp(cmp);

        } else {
            Held h = Held.builder()
                    .connectionId(in.getConnectionId())
                    .cmp(cmp)
                    .schedule(s)
                    .expiration(expiration)
                    .build();
            c.setHeld(h);
        }
    }

    public SimpleConnection fromConnection(Connection c, Boolean return_svc_ids) {
        Schedule s;
        Components cmp;

        if (c.getPhase().equals(Phase.HELD)) {
            s = c.getHeld().getSchedule();
            cmp = c.getHeld().getCmp();
        } else {
            s = c.getArchived().getSchedule();
            cmp = c.getArchived().getCmp();
        }

        String maxDateStr = "00:00:01 AM 01/01/2038";
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a M/d/uuuu", Locale.US);
        LocalDateTime localDateTime = LocalDateTime.parse(maxDateStr, dateTimeFormatter);
        ZoneId zoneId = ZoneId.of("America/Chicago");
        ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);
        Instant maxDate = zonedDateTime.toInstant();

        Long b = s.getBeginning().getEpochSecond();
        if (s.getBeginning().isAfter(maxDate)) {
            b = maxDate.getEpochSecond();
        }
        Long e = s.getEnding().getEpochSecond();
        if (s.getEnding().isAfter(maxDate)) {
            e = maxDate.getEpochSecond();
        }
        List<SimpleTag> simpleTags = new ArrayList<>();
        for (Tag t : c.getTags()) {
            simpleTags.add(SimpleTag.builder()
                    .category(t.getCategory())
                    .contents(t.getContents())
                    .build());
        }
        List<Fixture> fixtures = new ArrayList<>();
        List<Junction> junctions = new ArrayList<>();
        List<Pipe> pipes = new ArrayList<>();

        Integer vcid;
        if (return_svc_ids) {
            vcid = nsoVcIdDAO.findNsoVcIdByConnectionId(c.getConnectionId()).orElseThrow().getVcId();
        } else {
            vcid = null;
        }

        cmp.getFixtures().forEach(f -> {
            Fixture simpleF = Fixture.builder()
                    .inMbps(f.getIngressBandwidth())
                    .outMbps(f.getEgressBandwidth())
                    .port(f.getPortUrn())
                    .strict(f.getStrict())
                    .junction(f.getJunction().getDeviceUrn())
                    .vlan(f.getVlan().getVlanId())
                    .build();
            if (return_svc_ids) {
                simpleF.setSvcId(vcid);
            }

            fixtures.add(simpleF);
        });
        cmp.getJunctions().forEach(j -> junctions.add(Junction.builder()
                .device(j.getDeviceUrn())
                .build()));
        cmp.getPipes().forEach(p -> {
            List<String> ero = new ArrayList<>();
            p.getAzERO().forEach(h -> ero.add(h.getUrn()));
            pipes.add(Pipe.builder()
                    .azMbps(p.getAzBandwidth())
                    .zaMbps(p.getZaBandwidth())
                    .a(p.getA().getDeviceUrn())
                    .z(p.getZ().getDeviceUrn())
                    .protect(p.getProtect())
                    .ero(ero)
                    .build());
        });

        return (SimpleConnection.builder()
                .begin(b.intValue())
                .end(e.intValue())
                .connectionId(c.getConnectionId())
                .serviceId(c.getServiceId())
                .tags(simpleTags)
                .description(c.getDescription())
                .mode(c.getMode())
                .phase(c.getPhase())
                .username(c.getUsername())
                .state(c.getState())
                .fixtures(fixtures)
                .junctions(junctions)
                .pipes(pipes)
                .build());
    }

}
