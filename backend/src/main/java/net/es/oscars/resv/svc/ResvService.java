package net.es.oscars.resv.svc;

import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PSSException;
import net.es.oscars.app.props.PssProperties;
import net.es.oscars.resv.beans.PeriodBandwidth;
import net.es.oscars.resv.db.*;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.BwDirection;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.topo.beans.*;
import net.es.oscars.topo.enums.CommandParamType;
import net.es.oscars.topo.svc.TopoService;
import net.es.oscars.web.beans.Interval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;

import java.util.*;

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
    private CommandParamRepository cpRepo;
    @Autowired
    private PssProperties pssProperties;


    @Autowired
    private TopoService topoService;

    public Map<String, List<PeriodBandwidth>> reservedIngBws(Interval interval, String connectionId) {
        List<Schedule> scheds = scheduleRepo.findOverlapping(interval.getBeginning(), interval.getEnding());
        Map<String, List<PeriodBandwidth>> reservedIngBws = new HashMap<>();


        for (Schedule sch : scheds) {
            if (connectionId != null ) {
                if (sch.getConnectionId().equals(connectionId)) {
                    continue;
                }
            }

            if (sch.getPhase().equals(Phase.HELD) || sch.getPhase().equals(Phase.RESERVED)) {
                List<VlanFixture> fixtures = fixtureRepo.findBySchedule(sch);
                List<VlanPipe> pipes = pipeRepo.findBySchedule(sch);
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
        }
        return reservedIngBws;
    }

    public Map<String, List<PeriodBandwidth>> reservedEgBws(Interval interval, String connectionId) {
        List<Schedule> scheds = scheduleRepo.findOverlapping(interval.getBeginning(), interval.getEnding());
        Map<String, List<PeriodBandwidth>> reservedEgBws = new HashMap<>();

        for (Schedule sch : scheds) {
            if (connectionId != null) {
                if (sch.getConnectionId().equals(connectionId)) {
                    continue;
                }
            }
            if (sch.getPhase().equals(Phase.HELD) || sch.getPhase().equals(Phase.RESERVED)) {
                List<VlanFixture> fixtures = fixtureRepo.findBySchedule(sch);
                List<VlanPipe> pipes = pipeRepo.findBySchedule(sch);

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
        }
        return reservedEgBws;
    }

    public Collection<Vlan> reservedVlans(Interval interval, String connectionId) {
        List<Schedule> scheds = scheduleRepo.findOverlapping(interval.getBeginning(), interval.getEnding());
        HashSet<Vlan> reservedVlans = new HashSet<>();

        for (Schedule sch : scheds) {
            if (connectionId != null ) {
                if (sch.getConnectionId().equals(connectionId)) {
                    continue;
                }
            }
            if (sch.getPhase().equals(Phase.HELD) || sch.getPhase().equals(Phase.RESERVED)) {
                List<Vlan> vlans = vlanRepo.findBySchedule(sch);
                reservedVlans.addAll(vlans);
            }
        }
        return reservedVlans;
    }

    public Map<String, Integer> availableIngBws(Interval interval) {
        Map<String, List<PeriodBandwidth>> reservedIngBws = reservedIngBws(interval, null);

        /*
        try {
            ObjectMapper mapper = builder.build();
            log.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reservedIngBws));
        } catch (JsonProcessingException ex) {
            log.error(ex.getMessage());
        }
        */




        Map<String, TopoUrn> baseline = topoService.getTopoUrnMap();
        return ResvLibrary.availableBandwidthMap(BwDirection.INGRESS, baseline, reservedIngBws);

    }

    public Map<String, Set<ReservableCommandParam>> availableParams(Interval interval) {
        List<Schedule> scheds = scheduleRepo.findOverlapping(interval.getBeginning(), interval.getEnding());
        Map<String, Set<CommandParam>> reservedParams = this.reservedCommandParams(scheds);
        /*
        try {
            log.info("reserved:");
            String pretty = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(reservedParams);
            log.debug(pretty);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        */

        Map<String, TopoUrn> baseline = topoService.getTopoUrnMap();
        return ResvLibrary.availableCommandParams(baseline, reservedParams);

    }

    public Set<Integer> availableLoopbacks(Interval interval) throws PSSException {
        Set<Integer> available = new HashSet<>();
        List<Schedule> scheds = scheduleRepo.findOverlapping(interval.getBeginning(), interval.getEnding());
        List<Schedule> heldOrReservedScheds = new ArrayList<>();
        for (Schedule sch : scheds) {
            if (sch.getPhase().equals(Phase.HELD) || sch.getPhase().equals(Phase.RESERVED)) {
                heldOrReservedScheds.add(sch);
            }
        }
        Set<CommandParam> reservedLoopbacks = this.reservedLoopbacks(heldOrReservedScheds);


        String range = pssProperties.getLoopbackRange();
        // range format is ipv4address-ipv4address
        String[] parts = range.split("-");
        if (parts.length != 2) {
            throw new PSSException("invalid vpls loopback range "+range);
        }

        try {
            IPAddress bottom = new IPAddressString(parts[0]).toAddress();
            IPAddress top = new IPAddressString(parts[1]).toAddress();
            Integer min = bottom.toIPv4().intValue();
            Integer max = top.toIPv4().intValue();
//            log.info("vpls loopback range "+min+" -- "+max);
            if (max <= min ) {
                throw new PSSException("invalid VPLS loopback range");
            } else if (max - min > 10000) {
                throw new PSSException("VPLS loopback range too big");
            }
            for (Integer i = min; i <= max; i++) {
                available.add(i);
            }

        } catch (AddressStringException ex ) {
            throw new PSSException("invalid VPLS loopback range");
        }

        for (CommandParam reserved : reservedLoopbacks) {
            available.remove(reserved.getResource());
        }

        return available;
    }

    public Set<CommandParam> reservedLoopbacks(List<Schedule> scheds) {
        Set<CommandParam> result = new HashSet<>();

        for (Schedule sched : scheds) {
            List<CommandParam> cpList = cpRepo.findBySchedule(sched);
            for (CommandParam cp: cpList) {
                if (cp.getParamType().equals(CommandParamType.VPLS_LOOPBACK)) {
                    result.add(cp);
                }
            }
        }
        return result;

    }


    public Map<String, Set<CommandParam>> reservedCommandParams(List<Schedule> scheds) {
        Map<String, Set<CommandParam>> result = new HashMap<>();
        for (Schedule sched: scheds) {
            if (sched.getPhase().equals(Phase.RESERVED)) {
                for (VlanFixture f: fixtureRepo.findBySchedule(sched)) {
                    for (CommandParam cp : f.getCommandParams()) {
                        if (!result.containsKey(cp.getUrn())) {
                            result.put(cp.getUrn(), new HashSet<>());
                        }
                        result.get(cp.getUrn()).add(cp);
                    }
                }
                for (VlanJunction j: jnctRepo.findBySchedule(sched)) {
                    for (CommandParam cp : j.getCommandParams()) {
                        if (!result.containsKey(cp.getUrn())) {
                            result.put(cp.getUrn(), new HashSet<>());
                        }
                        result.get(cp.getUrn()).add(cp);
                    }
                }
            }

        }
        return result;
    }

    public Map<String, Integer> availableEgBws(Interval interval) {
        Map<String, List<PeriodBandwidth>> reservedEgBws = reservedEgBws(interval, null);
        /*
        try {
            ObjectMapper mapper = builder.build();
            log.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reservedEgBws));
        } catch (JsonProcessingException ex) {
            log.error(ex.getMessage());
        }
        */

        Map<String, TopoUrn> baseline = topoService.getTopoUrnMap();
        return ResvLibrary.availableBandwidthMap(BwDirection.EGRESS, baseline, reservedEgBws);
    }

    public Map<String, PortBwVlan> available(Interval interval, String connectionId) {
        Collection<Vlan> reservedVlans = reservedVlans(interval, connectionId);
        Map<String, List<PeriodBandwidth>> reservedEgBws = reservedEgBws(interval, connectionId);
        Map<String, List<PeriodBandwidth>> reservedIngBws = reservedIngBws(interval, connectionId);

        return ResvLibrary.portBwVlans(topoService.getTopoUrnMap(), reservedVlans, reservedIngBws, reservedEgBws);
    }

    static private void addTo(Map<String, List<PeriodBandwidth>> bwMap, String urn, PeriodBandwidth pbw) {
        if (!bwMap.containsKey(urn)) {
            bwMap.put(urn, new ArrayList<>());
        }
        bwMap.get(urn).add(pbw);
    }


}
