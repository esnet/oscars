package net.es.oscars.resv.svc;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.resv.beans.PeriodBandwidth;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.BwDirection;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.topo.beans.IntRange;
import net.es.oscars.topo.beans.PortBwVlan;
import net.es.oscars.topo.beans.ReservableCommandParam;
import net.es.oscars.topo.beans.TopoUrn;
import net.es.oscars.topo.enums.CommandParamType;
import net.es.oscars.topo.enums.Layer;
import net.es.oscars.topo.enums.UrnType;

import java.time.Instant;
import java.util.*;

@Slf4j
public class ResvLibrary {




    public static Map<String, PortBwVlan> portBwVlans(Map<String, TopoUrn> urnMap,
                                                      Collection<Vlan> reservedVlans,
                                                      Map<String, List<PeriodBandwidth>> reservedIngBws,
                                                      Map<String, List<PeriodBandwidth>> reservedEgBws) {

        Map<String, Set<IntRange>> availableVlanMap = ResvLibrary.availableVlanMap(urnMap, reservedVlans);
        Map<String, Integer> availableIngressBw = ResvLibrary.availableBandwidthMap(BwDirection.INGRESS, urnMap, reservedIngBws);
        Map<String, Integer> availableEgressBw = ResvLibrary.availableBandwidthMap(BwDirection.EGRESS, urnMap, reservedEgBws);

        Map<String, PortBwVlan> available = new HashMap<>();
        if (urnMap == null) {
            return new HashMap<>();
        }
        urnMap.forEach((urn, topoUrn) -> {
            if (topoUrn.getUrnType().equals(UrnType.PORT)) {
                Integer ingBw = availableIngressBw.get(urn);
                Integer egBw = availableEgressBw.get(urn);

                Set<IntRange> intRanges = new HashSet<>();
                String vlanExpr = "";

                if (topoUrn.getCapabilities().contains(Layer.ETHERNET)) {
                    intRanges = availableVlanMap.get(urn);
                    vlanExpr = IntRange.asString(intRanges);
                }

                PortBwVlan pbw = PortBwVlan.builder()
                        .vlanRanges(intRanges)
                        .ingressBandwidth(ingBw)
                        .egressBandwidth(egBw)
                        .vlanExpression(vlanExpr)
                        .build();
                available.put(urn, pbw);
            }

        });
        return available;
    }

    public static List<Schedule> schedulesOverlapping(List<Schedule> all, Instant b, Instant e) {
        List<Schedule> overlapping = new ArrayList<>();
        for (Schedule s : all) {
            if (s.getPhase().equals(Phase.HELD) || s.getPhase().equals(Phase.RESERVED)) {
                if (s.overlaps(b, e)) {
                    overlapping.add(s);
                }

            }
        }
        return overlapping;
    }

    public static List<PeriodBandwidth> pbwsOverlapping(List<PeriodBandwidth> candidates, Instant b, Instant e) {
        List<PeriodBandwidth> result = new ArrayList<>();
        for (PeriodBandwidth pbw : candidates) {
            log.info("evaluating a pbw: " + pbw.toString());
            boolean add = true;
            if (pbw.getEnding().isBefore(b) || pbw.getBeginning().isAfter(e)) {
                add = false;
            }
            if (add) {
                log.info("adding a pbw: " + pbw.toString());
                result.add(pbw);
            }
        }

        return result;
    }


    public static Map<String, Integer> availableBandwidthMap(BwDirection dir, Map<String, TopoUrn> baseline,
                                                             Map<String, List<PeriodBandwidth>> reservedBandwidths) {
        Map<String, Integer> result = new HashMap<>();
        if (baseline == null) {
            log.info("no baseline available; possibly still starting up.");
            return new HashMap<>();
        }

        for (String urn : baseline.keySet()) {
            if (baseline.get(urn).getUrnType().equals(UrnType.PORT)) {
                Integer reservable = switch (dir) {
                    case INGRESS -> baseline.get(urn).getReservableIngressBw();
                    case EGRESS -> baseline.get(urn).getReservableEgressBw();
                };
                Integer availableBw = overallAvailBandwidth(reservable, reservedBandwidths.get(urn));
                result.put(urn, availableBw);
            }
        }
        return result;

    }
    public static Map<String, Set<ReservableCommandParam>>
            availableCommandParams(Map<String, TopoUrn> baseline, Map<String, Set<CommandParam>> reservedParams) {
        Map<String, Set<ReservableCommandParam>> result = new HashMap<>();
        for (String urn: baseline.keySet()) {
            if (!result.containsKey(urn)) {
                result.put(urn, new HashSet<>());
            }


            Set<ReservableCommandParam> reservable = baseline.get(urn).getReservableCommandParams();
            Set<CommandParam> reservedOnUrn = new HashSet<>();
            if (reservedParams.containsKey(urn)) {
                reservedOnUrn = reservedParams.get(urn);
            }
            Set<CommandParamType> cpts = new HashSet<>();
            reservable.forEach(r -> {
                cpts.add(r.getType());
            });
            for (CommandParamType cpt: cpts) {
                Set<IntRange> reservableOfType = new HashSet<>();
                for (ReservableCommandParam rcp : reservable) {
                    if (rcp.getType().equals(cpt)) {
                        reservableOfType.addAll(rcp.getReservableRanges());
                    }
                }
                Set<Integer> reservedOfType = new HashSet<>();
                for (CommandParam cp : reservedOnUrn) {
                    if (cp.getParamType().equals(cpt)) {
                        reservedOfType.add(cp.getResource());
                    }
                }
                Set<IntRange> availableofType = availableInts(reservableOfType, reservedOfType);
                ReservableCommandParam rcp = ReservableCommandParam.builder()
                        .type(cpt)
                        .reservableRanges(availableofType)
                        .build();
                result.get(urn).add(rcp);
            }
        }



        return result;
    }

    public static Map<String, Set<IntRange>> availableVlanMap(Map<String, TopoUrn> baseline, Collection<Vlan> reservedVlans) {

        Map<String, Set<Integer>> reservedVlanMap = new HashMap<>();
        reservedVlans.forEach(v -> {
            if (!reservedVlanMap.keySet().contains(v.getUrn())) {
                reservedVlanMap.put(v.getUrn(), new HashSet<>());
            }
            reservedVlanMap.get(v.getUrn()).add(v.getVlanId());
        });

        Map<String, Set<IntRange>> availableVlanMap = new HashMap<>();
        if (baseline == null) {
            log.info("no baseline available; possibly still starting up.");
            return new HashMap<>();
        }
        for (String urn : baseline.keySet()) {
            Set<IntRange> reservable = baseline.get(urn).getReservableVlans();
            Set<IntRange> availableVlans = availableInts(reservable, reservedVlanMap.get(urn));
            availableVlanMap.put(urn, availableVlans);
        }
        return availableVlanMap;
    }

    public static Set<IntRange> availableInts(Set<IntRange> reservable, Set<Integer> reserved) {
        Set<Integer> available = new HashSet<>();
        for (IntRange range : reservable) {
            available.addAll(range.asSet());
        }
        if (reserved != null) {
            available.removeAll(reserved);
        }
        return IntRange.fromSet(available);
    }

    public static Map<String, TopoUrn> constructAvailabilityMap(
            Map<String, TopoUrn> urnMap,
            Map<String, Set<IntRange>> availVlanMap,
            Map<String, Integer> availIngressBwMap,
            Map<String, Integer> availEgressBwMap,
            Map<String, Set<ReservableCommandParam>> availCommandParamMap) {

        Map<String, TopoUrn> result = new HashMap<>();

        for (Map.Entry<String, TopoUrn> e : urnMap.entrySet()) {
            TopoUrn u = e.getValue();

            HashSet<Layer> capabilities = new HashSet<>();
            capabilities.addAll(u.getCapabilities());

            TopoUrn copy = TopoUrn.builder()
                    .capabilities(capabilities)
                    .device(u.getDevice())
                    .port(u.getPort())
                    .reservableVlans(availVlanMap.get(u.getUrn()))
                    .reservableEgressBw(availEgressBwMap.get(u.getUrn()))
                    .reservableIngressBw(availIngressBwMap.get(u.getUrn()))
                    .reservableCommandParams(availCommandParamMap.get(u.getUrn()))
                    .build();

            result.put(e.getKey(), copy);
        }
        return result;

    }


    public static Integer availBandwidth(Integer reservableBw,
                                         Collection<PeriodBandwidth> periodBandwidths,
                                         Instant when) {
        Integer result = reservableBw;
        for (PeriodBandwidth pbw : periodBandwidths) {
            if (pbw.getBeginning().isBefore(when) && pbw.getEnding().isAfter(when)) {
                result -= pbw.getBandwidth();
            }
        }

        return result;
    }

    public static Integer overallAvailBandwidth(Integer reservableBw,
                                                Collection<PeriodBandwidth> periodBandwidths) {
        Map<Instant, Set<Integer>> timeline = new HashMap<>();
        if (periodBandwidths == null) {
            periodBandwidths = new ArrayList<>();
        }
        for (PeriodBandwidth pbw : periodBandwidths) {
            if (!timeline.containsKey(pbw.getBeginning())) {
                timeline.put(pbw.getBeginning(), new HashSet<>());
            }
            if (!timeline.containsKey(pbw.getEnding())) {
                timeline.put(pbw.getEnding(), new HashSet<>());
            }
            timeline.get(pbw.getBeginning()).add(pbw.getBandwidth());
            timeline.get(pbw.getEnding()).add(-1 * pbw.getBandwidth());
        }
        List<Instant> instants = new ArrayList<>(timeline.keySet());
        instants.sort(Comparator.comparing(Instant::getEpochSecond));

        Integer maxReserved = 0;

        Integer runningReserved = 0;
        for (Instant inst : instants) {
            Set<Integer> deltas = timeline.get(inst);
            for (Integer d : deltas) {
                runningReserved += d;
            }
            if (runningReserved > maxReserved) {
                maxReserved = runningReserved;
            }
        }
        return reservableBw - maxReserved;
    }

    public static Integer minBwOverPath(List<TopoUrn> topoUrns, Map<String, Integer> availableBws) {
        Integer min = Integer.MAX_VALUE;
        for (TopoUrn topoUrn: topoUrns) {
            Integer thisBw = availableBws.get(topoUrn.getUrn());
            if (thisBw != null && thisBw < min) {
                min = thisBw;
            }
        }
        return min;
    }

    public static Map<String, Integer> decideIdentifier(Map<String, Set<IntRange>> requested,
                                                        Map<String, Set<IntRange>> available) {

        Map<String, Integer> result = new HashMap<>();

        // first, try to find an identifier that is within all of the available / requested ranges
        Map<String, Set<IntRange>> search = new HashMap<>();
        for (String r : requested.keySet()) {
            search.put(r+"-req", requested.get(r));
            log.info("req: "+r+" : "+IntRange.asString(requested.get(r)));
        }
        for (String a : available.keySet()) {
            search.put(a+"-avail", available.get(a));
            log.info("avail: "+a+" : "+IntRange.asString(available.get(a)));
        }

        Integer least = IntRange.leastInAll(search);
        if (least != null) {
            for (String r : requested.keySet()) {
                result.put(r, least);
            }
            log.info("identifier available for all: "+least);
            return result;
        }


        // if we couldn't find contained in all, we now try to find one per each requested / avail pair
        // make sure to not double-pick the same vlan for the same port
        Map<String, Set<Integer>> pickedOnPorts = new HashMap<>();
        for (String key : requested.keySet()) {
            String[] reqParts = key.split("#");
            String urn = reqParts[0];
            Set<IntRange> req = requested.get(key);
            Set<IntRange> avail = available.get(urn);
            if (pickedOnPorts.containsKey(urn)) {
                Set<Integer> picked = pickedOnPorts.get(urn);
                // need to subtract already picked from available
                for (Integer ident : picked) {
                    avail = IntRange.subtractFromSet(avail, ident);
                }
            }


            search = new HashMap<>();
            search.put("req", req);
            search.put("avail", avail);
            least = IntRange.leastInAll(search);
            log.info("identifier available / requested for "+key+": "+least);
            if (pickedOnPorts.containsKey(urn)) {
                pickedOnPorts.get(urn).add(least);
            } else {
                Set<Integer> idents = new HashSet<>();
                idents.add(least);
                pickedOnPorts.put(urn, idents);
            }
            result.put(key, least);
        }
        return result;

    }

}
