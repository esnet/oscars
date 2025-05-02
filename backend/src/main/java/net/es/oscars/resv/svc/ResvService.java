package net.es.oscars.resv.svc;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.resv.beans.PeriodBandwidth;
import net.es.oscars.resv.db.*;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.BwDirection;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.topo.beans.*;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.oscars.model.Interval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;

import java.util.*;

import static net.es.oscars.app.util.PrettyPrinter.prettyLog;

@Service
@Slf4j
@Data
public class ResvService {
    @Autowired
    private Jackson2ObjectMapperBuilder builder;

    @Autowired
    private ScheduleRepository scheduleRepo;

    @Autowired
    private VlanRepository vlanRepo;

    @Autowired
    private FixtureRepository fixtureRepo;

    @Autowired
    private PipeRepository pipeRepo;

    @Autowired
    private JunctionRepository jnctRepo;

    @Autowired
    private TopologyStore topologyStore;


    public Map<String, List<PeriodBandwidth>> reservedIngBws(Interval interval, Map<String, Connection> held, String connectionId) {
        Set<Schedule> scheds = reservedSchedules(interval, connectionId);

        Map<String, List<PeriodBandwidth>> reservedIngBws = new HashMap<>();
        Map<String, List<VlanFixture>> fixturesByConnId = new HashMap<>();
        Map<String, List<VlanPipe>> pipesByConnId = new HashMap<>();
        Map<String, Schedule> scheduleMap = new HashMap<>();
        for (Schedule sch : scheds) {
            if (sch.getPhase().equals(Phase.RESERVED)) {
                fixturesByConnId.put(sch.getConnectionId(), fixtureRepo.findBySchedule(sch));
                pipesByConnId.put(sch.getConnectionId(), pipeRepo.findBySchedule(sch));
                scheduleMap.put(sch.getConnectionId(), sch);
            }
        }
        for (Connection c : held.values()) {
            fixturesByConnId.put(c.getConnectionId(), c.getHeld().getCmp().getFixtures());
            pipesByConnId.put(c.getConnectionId(), c.getHeld().getCmp().getPipes());
            scheduleMap.put(c.getConnectionId(), c.getHeld().getSchedule());
        }

        for (String connId : scheduleMap.keySet()) {
            List<VlanFixture> fixtures = fixturesByConnId.get(connId);
            List<VlanPipe> pipes = pipesByConnId.get(connId);
            Schedule sch = scheduleMap.get(connId);

            for (VlanFixture f : fixtures) {
                String urn = f.getPortUrn();
                PeriodBandwidth iPbw = PeriodBandwidth.builder()
                        .bandwidth(f.getIngressBandwidth())
                        .beginning(sch.getBeginning())
                        .ending(sch.getEnding())
                        .build();
                addTo(reservedIngBws, urn, iPbw);
            }

            for (VlanPipe pipe : pipes) {
                // hops go:
                // device, outPort, inPort, device, outPort, inPort, device
                // hops will always be empty, or 1 modulo 3
                // bandwidth gets applied per direction i.e.
                // az as egress on outPort, as ingress on inPort
                if (pipe.getAzERO() != null) {

                    for (int i = 0; i < pipe.getAzERO().size(); i++) {
                        EroHop hop = pipe.getAzERO().get(i);
                        String urn = hop.getUrn();
                        PeriodBandwidth pbw = PeriodBandwidth.builder()
                                .bandwidth(pipe.getAzBandwidth())
                                .beginning(sch.getBeginning())
                                .ending(sch.getEnding())
                                .build();

                        if (i % 3 == 2) {
                            addTo(reservedIngBws, urn, pbw);
                        }
                    }
                }

                if (pipe.getZaERO() != null ) {

                    for (int i = 0; i < pipe.getZaERO().size(); i++) {
                        EroHop hop = pipe.getZaERO().get(i);
                        String urn = hop.getUrn();

                        PeriodBandwidth pbw = PeriodBandwidth.builder()
                                .bandwidth(pipe.getZaBandwidth())
                                .beginning(sch.getBeginning())
                                .ending(sch.getEnding())
                                .build();

                        if (i % 3 == 2) {
                            addTo(reservedIngBws, urn, pbw);
                        }
                    }
                }
            }
        }
        return reservedIngBws;
    }

    public Map<String, List<PeriodBandwidth>> reservedEgBws(Interval interval, Map<String, Connection> held, String connectionId) {
        Set<Schedule> scheds = reservedSchedules(interval, connectionId);
        Map<String, List<PeriodBandwidth>> reservedEgBws = new HashMap<>();
        Map<String, List<VlanFixture>> fixturesByConnId = new HashMap<>();
        Map<String, List<VlanPipe>> pipesByConnId = new HashMap<>();
        Map<String, Schedule> scheduleMap = new HashMap<>();
        for (Schedule sch : scheds) {
            if (sch.getPhase().equals(Phase.RESERVED)) {
                fixturesByConnId.put(sch.getConnectionId(), fixtureRepo.findBySchedule(sch));
                pipesByConnId.put(sch.getConnectionId(), pipeRepo.findBySchedule(sch));
                scheduleMap.put(sch.getConnectionId(), sch);
            }
        }
        for (Connection c : held.values()) {
            fixturesByConnId.put(c.getConnectionId(), c.getHeld().getCmp().getFixtures());
            pipesByConnId.put(c.getConnectionId(), c.getHeld().getCmp().getPipes());
            scheduleMap.put(c.getConnectionId(), c.getHeld().getSchedule());
        }


        for (String connId : scheduleMap.keySet()) {
            List<VlanFixture> fixtures = fixturesByConnId.get(connId);
            List<VlanPipe> pipes = pipesByConnId.get(connId);
            Schedule sch = scheduleMap.get(connId);

            for (VlanFixture f : fixtures) {
                String urn = f.getPortUrn();
                PeriodBandwidth ePbw = PeriodBandwidth.builder()
                        .bandwidth(f.getEgressBandwidth())
                        .beginning(sch.getBeginning())
                        .ending(sch.getEnding())
                        .build();

                addTo(reservedEgBws, urn, ePbw);
            }

            for (VlanPipe pipe : pipes) {
                // hops go:
                // device, outPort, inPort, device, outPort, inPort, device
                // hops will always be empty, or 1 modulo 3
                // bandwidth gets applied per direction i.e.
                // az as egress on outPort, as ingress on inPort
                if (pipe.getAzERO() != null) {

                    for (int i = 0; i < pipe.getAzERO().size(); i++) {

                        EroHop hop = pipe.getAzERO().get(i);
                        String urn = hop.getUrn();
                        PeriodBandwidth pbw = PeriodBandwidth.builder()
                                .bandwidth(pipe.getAzBandwidth())
                                .beginning(sch.getBeginning())
                                .ending(sch.getEnding())
                                .build();

                        if (i % 3 == 1) {
                            addTo(reservedEgBws, urn, pbw);
                        }
                    }
                }
                if (pipe.getZaERO() != null) {
                    for (int i = 0; i < pipe.getZaERO().size(); i++) {
                        EroHop hop = pipe.getZaERO().get(i);
                        String urn = hop.getUrn();

                        PeriodBandwidth pbw = PeriodBandwidth.builder()
                                .bandwidth(pipe.getZaBandwidth())
                                .beginning(sch.getBeginning())
                                .ending(sch.getEnding())
                                .build();

                        if (i % 3 == 1) {

                            addTo(reservedEgBws, urn, pbw);
                        }
                    }
                }
            }
        }
        return reservedEgBws;
    }

    public Map<String, Map<Integer, Set<String>>> vlanUsage(Interval interval, Map<String, Connection> held, String connectionId) {
        Collection<Vlan> reservedVlans = this.reservedOrHeldVlans(interval, held, connectionId);
        Map<String, Map<Integer, Set<String>>> results = new HashMap<>();

        for (Vlan v : reservedVlans) {
            String portUrn = v.getUrn();
            if (!results.containsKey(portUrn)) {
                results.put(portUrn, new HashMap<>());
            }
            if (!results.get(portUrn).containsKey(v.getVlanId())) {
                results.get(portUrn).put(v.getVlanId(), new HashSet<>());
            }
            results.get(portUrn).get(v.getVlanId()).add(v.getConnectionId());

        }
        return results;
    }

    // this grabs all schedule entries from RESERVED connections or ones currently HELD in memory in connService
    // if there is a schedule for a connectionId that is found RESERVED and HELD simultaneously,
    // we use the HELD one
    public Set<Schedule> reservedSchedules(Interval interval, String connectionId) {
        Map<String, Schedule> scheduleMap = new HashMap<>();
        List<Schedule> scheds = scheduleRepo.findOverlapping(interval.getBeginning(), interval.getEnding());
        for (Schedule sch : scheds) {
            // skip archived schedules
            if (sch.getPhase().equals(Phase.RESERVED)) {
                scheduleMap.put(sch.getConnectionId(), sch);
            }
        }
        // we want to ignore the schedule of the connectionId we got passed
        scheduleMap.remove(connectionId);

        return new HashSet<>(scheduleMap.values());
    }


    public Collection<Vlan> reservedOrHeldVlans(Interval interval, Map<String, Connection> held, String connectionId) {

        Set<Schedule> scheds = reservedSchedules(interval, connectionId);
        Map<String, Set<Vlan>> vlansByConnectionId = new HashMap<>();

        HashSet<Vlan> reservedVlans = new HashSet<>();
        for (Schedule sch : scheds) {
            if (sch.getPhase().equals(Phase.RESERVED)) {
                List<Vlan> vlans = vlanRepo.findBySchedule(sch);
                vlansByConnectionId.put(sch.getConnectionId(), new HashSet<>(vlans));
            }
        }
        for (Connection c : held.values()) {
            prettyLog(c);
            Set<Vlan> vlans = new HashSet<>();
            for (VlanFixture f : c.getHeld().getCmp().getFixtures()) {

                vlans.add(f.getVlan());
            }
            vlansByConnectionId.put(c.getConnectionId(), new HashSet<>(vlans));

        }
        vlansByConnectionId.remove(connectionId);
        for (String connId : vlansByConnectionId.keySet()) {
            reservedVlans.addAll(vlansByConnectionId.get(connId));
        }
        return reservedVlans;
    }

    public Map<String, Integer> availableIngBws(Interval interval) {
        Map<String, List<PeriodBandwidth>> reservedIngBws = reservedIngBws(interval, new HashMap<>(), null);

        Map<String, TopoUrn> baseline = topologyStore.getTopoUrnMap();
        return ResvLibrary.availableBandwidthMap(BwDirection.INGRESS, baseline, reservedIngBws);

    }


    public Map<String, Integer> availableEgBws(Interval interval) {
        Map<String, List<PeriodBandwidth>> reservedEgBws = reservedEgBws(interval, new HashMap<>(), null);
        /*
        try {
            ObjectMapper mapper = builder.build();
            log.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reservedEgBws));
        } catch (JsonProcessingException ex) {
            log.error(ex.getMessage());
        }
        */

        Map<String, TopoUrn> baseline = topologyStore.getTopoUrnMap();
        return ResvLibrary.availableBandwidthMap(BwDirection.EGRESS, baseline, reservedEgBws);
    }

    public Map<String, PortBwVlan> available(Interval interval, Map<String, Connection> held, String connectionId) {
        Collection<Vlan> reservedVlans = reservedOrHeldVlans(interval, held, connectionId);
        Map<String, List<PeriodBandwidth>> reservedEgBws = reservedEgBws(interval, held, connectionId);
        Map<String, List<PeriodBandwidth>> reservedIngBws = reservedIngBws(interval, held, connectionId);

        return ResvLibrary.portBwVlans(topologyStore.getTopoUrnMap(), reservedVlans, reservedIngBws, reservedEgBws);
    }

    static private void addTo(Map<String, List<PeriodBandwidth>> bwMap, String urn, PeriodBandwidth pbw) {
        if (!bwMap.containsKey(urn)) {
            bwMap.put(urn, new ArrayList<>());
        }
        bwMap.get(urn).add(pbw);
    }


}
