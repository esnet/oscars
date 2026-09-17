package net.es.oscars.topo.svc;

import lombok.extern.slf4j.Slf4j;
import net.es.topo.common.dto.esdb.EsdbEquip;
import net.es.topo.common.dto.esdb.EsdbVlanWithDetails;
import net.es.topo.common.dto.nso.NsoBBL;
import net.es.topo.common.dto.nso.NsoPort;
import net.es.topo.common.dto.nso.NsoSystem;
import net.es.topo.common.model.oscars1.*;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.*;

import static net.es.oscars.topo.svc.TopoGenerator.getReservableBw;

@Slf4j
@Component
public class ServiceToOscars {
    public OscarsOneDevice makeDevice(String device, EsdbEquip equip, List<EsdbVlanWithDetails> esdbVlans,
                                      List<NsoPort> ports, NsoSystem system, Set<String> mplsPorts) {

        // these are the default sets of vlan ids available for tagged and untagged ports
        if (equip == null) {
            log.error("null equip - skipping {}", device);
            return null;

        } else if (system == null) {
            log.error("system is null for {}", device);
            return null;
        }

        Map<NsoPort.EthEncap, Set<Integer>> baseVlans = baseVlanMap();

        // for each interface, map it to the vlans marked as in use on it in ESDB
        Map<String, List<EsdbVlanWithDetails>> byIfceName = new HashMap<>();
        for (EsdbVlanWithDetails vlan : esdbVlans) {
            String ifceName = vlan.getEquipmentInterface().getIfce();
            if (!byIfceName.containsKey(ifceName)) {
                byIfceName.put(ifceName, new ArrayList<>());
            }
            byIfceName.get(ifceName).add(vlan);
        }


        OscarsOneDevice osDevice = OscarsOneDevice.builder()
                .urn(equip.getName())
                .capabilities(List.of(OscarsOneCapability.ETHERNET, OscarsOneCapability.MPLS))
                // hard-coding SR7750 for now
                .model(OscarsOneDevice.Model.ALCATEL_SR7750)
                .reservableVlans(new ArrayList<>())
                .ipv4Address(system.getLoopbackIpv4())
                .location(equip.getLocation().getShortName())
                .locationId(equip.getLocation().getId())
                .latitude(equip.getLatitude().toString())
                .longitude(equip.getLongitude().toString())
                .esdbEquipmentId(equip.getId())
                .type("ROUTER")
                // we will fill in the ports next
                .ports(new ArrayList<>())
                .build();

        // we walk all the NsoPort instances
        ports.stream().filter(p -> p.getDevice().equals(equip.getName())).forEach(p -> {
            // skip down or no-config ports if they exist
            boolean skip = false;

            if (p.getAdminState() != null && !p.getAdminState().equals(NsoPort.AdminState.UP)) {
                skip = true;
            }

            if (!skip) {
                // for each port we make an unused vlan set that starts with the base vlan set:
                NsoPort.EthEncap encap = NsoPort.EthEncap.DOT1Q;
                if (p.getEthEncap() != null) {
                    encap = p.getEthEncap();
                }
                Set<Integer> unusedVlanSet = new HashSet<>(baseVlans.get(encap));

                // then we walk the ESDB vlan entries and mark all vlan-ids referenced there
                // as in-use (i.e. exclude them from our topology)
                if (byIfceName.containsKey(p.getIfce())) {
                    for (EsdbVlanWithDetails vlan : byIfceName.get(p.getIfce())) {
                        // we don't want to mark vlans already assigned by OSCARS as in-use
                        boolean markVlanIdAsUsed = !vlan.getDescription().startsWith("OSCARS ");

                        if (markVlanIdAsUsed) {
                            unusedVlanSet.remove(vlan.getVlanId());
                        }
                    }
                }

                if (p.getVlan() != null && p.getVlan().getAssigned() != null) {

                    // then we walk the assigned vlan entries from NSO port service, and again mark
                    // all vlan-ids referenced there as in-use (i.e. exclude them from our topology)
                    for (NsoPort.Assigned assigned : p.getVlan().getAssigned()) {
                        for (NsoPort.Service svc : assigned.getService()) {
                            // we don't want to mark vlans already assigned by OSCARS as in-use
                            boolean markVlanIdAsUsed = !svc.getServiceType().equals("l2vpn");
                            if (markVlanIdAsUsed) {
                                unusedVlanSet.remove(assigned.getVlanId());
                            }
                        }
                    }
                }
                buildPort(p, equip, unusedVlanSet, mplsPorts).ifPresent(
                    op -> osDevice.getPorts().add(op)
                );

            }

        });

        return osDevice;
    }

    public List<OscarsOneAdjcy> makeAdjacencies(List<NsoBBL> bbls, List<String> coreRouters) {
        List<OscarsOneAdjcy> adjcies = new ArrayList<>();

        for (NsoBBL bbl : bbls) {

            for (NsoBBL.NsoLayer3Circuit l3c : bbl.getNsoLayer3CircuitList()) {
                boolean skip = false;

                if (l3c.getAdminState() != null) {
                    if (l3c.getAdminState().equals("no-config")) {
                        skip = true;
                    }
                }
                if (!coreRouters.contains(bbl.getA()) || !coreRouters.contains(bbl.getZ())) {
                    skip = true;
                }

                if (!skip) {


                    Pair<String, String> ifceNames = this.bblInterfaceNames(bbl, l3c);

                    Map<String, Integer> metricsMap = new HashMap<>();
                    metricsMap.put("MPLS", metric(bbl.getLinkMemberSpeed(), bbl.getIgp(), l3c.getAdminState()));

                    String aPortUrn = "%s:%s".formatted(bbl.getA(), l3c.getAParams().getPort());
                    String zPortUrn = "%s:%s".formatted(bbl.getZ(), l3c.getZParams().getPort());

                    adjcies.add(OscarsOneAdjcy.builder()
                            .metrics(metricsMap)
                            .a(OscarsOneAdjcy.AdjcyIfce.builder()
                                    .device(bbl.getA())
                                    .addr(l3c.getAParams().getIpv4Address())
                                    .ifce(ifceNames.getFirst())
                                    .port(aPortUrn)
                                    .build())
                            .z(OscarsOneAdjcy.AdjcyIfce.builder()
                                    .device(bbl.getZ())
                                    .addr(l3c.getZParams().getIpv4Address())
                                    .ifce(ifceNames.getSecond())
                                    .port(zPortUrn)
                                    .build())
                            .build());
                }
            }
        }
        return adjcies;
    }

    Pair<String, String> bblInterfaceNames(NsoBBL bbl, NsoBBL.NsoLayer3Circuit l3c) {
        String aName, zName;
        String flavorPart = "bb";
        if (bbl.getFlavor().equals("management")) {
            flavorPart = "mgt";
        }

        String aSite, zSite;

        aSite = bbl.getA();
        String[] aRouterPart = bbl.getA().split("-");
        if (aRouterPart.length > 0) {
            aSite = aRouterPart[0];
        }
        zName = aSite + "-" + flavorPart + "-" + l3c.getIfceLetterSeq();


        zSite = bbl.getZ();
        String[] zRouterPart = bbl.getZ().split("-");
        if (aRouterPart.length > 0) {
            zSite = zRouterPart[0];
        }
        aName = zSite + "-" + flavorPart + "-" + l3c.getIfceLetterSeq();


        return Pair.of(aName, zName);
    }

    /**
     * Generate a OscarsOnePort object.
     *
     * @param np         The NsoNokiaDevice.Port object
     * @param equip      The EsdbEquip object
     * @param availVlans Hash map of available VLANs
     * @return OscarsOnePort
     */
    public static Optional<OscarsOnePort> buildPort(
            NsoPort np,
            EsdbEquip equip,
            Set<Integer> availVlans,
            Set<String> mplsPorts
    ) {
        String portId = np.getIfce();

        EthernetEncapsulation ethernetEncapsulation = getEncapsulationType(np);
        EsdbEquip.EsdbEquipmentIfceInline eqIfce = equip.getIfcesByName().get(portId);
        if (eqIfce == null) {
            String ps = "port %s : null eq_ifce. description: %s ".formatted(portId, np.getDescription());
            log.error(ps);
            return Optional.empty();
        }
        Integer portSpeed = eqIfce.getSpeed(); // may be null
        if (portSpeed == null) {
            String ps = "port %s (ESDB id: [ %d ] ): null eq_ifce_bw. description: %s ".formatted(portId, eqIfce.getId(), np.getDescription());
            log.error(ps);
            return Optional.empty();
        }

        String portUrn = equip.getName() + ":" + portId;
        Integer reservableBw = getReservableBw(portSpeed, eqIfce);
        List<IntRange> reservableVlans = new ArrayList<>(IntRange.fromSet(availVlans));
        OscarsOnePort op = OscarsOnePort.builder()
                .reservableVlans(reservableVlans)
                .capabilities(new ArrayList<>())
                .urn(portUrn)
                .ifces(new ArrayList<>())
                .reservableBw(reservableBw)
                .reservableIngressBw(reservableBw)
                .reservableEgressBw(reservableBw)
                .esdbEquipmentInterfaceId(eqIfce.getId())
                .ethernetEncapsulation(ethernetEncapsulation)
                .tags(new ArrayList<>())
                .build();

        // all ports in the NSO port service are ethernet
        op.getCapabilities().add(OscarsOneCapability.ETHERNET);

        // some are EDGE, some are TAGGED and some UNTAGGED
        if (isEdge(np)) {
            op.getCapabilities().add(OscarsOneCapability.EDGE);
        }
        if (np.getEthEncap().equals(NsoPort.EthEncap.ACCESS)) {
            op.getCapabilities().add(OscarsOneCapability.UNTAGGED);
        } else {
            op.getCapabilities().add(OscarsOneCapability.TAGGED);
        }

        if (mplsPorts.contains(portUrn)) {
            op.getCapabilities().add(OscarsOneCapability.MPLS);
        }

        if (np.getDescription() != null) {
            op.getTags().add(np.getDescription());
        }

        return Optional.of(op);
    }

    public static boolean isEdge(NsoPort np) {
        if (np.getMode() == null) {
            np.setMode(NsoPort.Mode.HYBRID);
        }
        return switch (np.getMode()) {
            case ACCESS -> true;
            case HYBRID -> true;
            case NETWORK -> false;
        };
    }

    /**
     * Check the encapsulation type of the specified NsoNokiaDevice.Port to correlate with Ethernet Encapsulation enumeration.
     *
     * @param np The NsoNokiaDevice Port object
     * @return EthernetEncapsulation
     */
    public static EthernetEncapsulation getEncapsulationType(NsoPort np) {
        if (np.getEthEncap() == null) {
            np.setEthEncap(NsoPort.EthEncap.DOT1Q);
        }
        return switch (np.getEthEncap()) {
            case DOT1Q -> EthernetEncapsulation.DOT1Q;
            case ACCESS -> EthernetEncapsulation.NULL;
        };
    }

    public static Map<NsoPort.EthEncap, Set<Integer>> baseVlanMap() {
        Map<NsoPort.EthEncap, Set<Integer>> baseVlans = new HashMap<>();
        Set<Integer> taggedVlansBaseSet = new HashSet<>();
        for (int i = 2; i <= 4095; i++) {
            taggedVlansBaseSet.add(i);
        }

        Set<Integer> untaggedVlansBaseSet = new HashSet<>();
        untaggedVlansBaseSet.add(0);

        baseVlans.put(NsoPort.EthEncap.ACCESS, untaggedVlansBaseSet);
        baseVlans.put(NsoPort.EthEncap.DOT1Q, taggedVlansBaseSet);
        return baseVlans;
    }


    public static Integer metric(int speed, NsoBBL.IGPParams igp, String adminState) {
        if (adminState != null) {
            if (adminState.equals("maintenance")) {
                return 15000000;
            } else if (adminState.equals("deployment") || adminState.equals("no-config")) {
                return 15123123;
            }
        }

        Map<String, Double> preferenceMap = new HashMap<>();
        Double prefScalar = 1.0;
        preferenceMap.put("core", 1.0);
        preferenceMap.put("worst", 1.5);
        preferenceMap.put("tail", 5.0);
        preferenceMap.put("preferred", 0.5);
        preferenceMap.put("sea-cable", 1.2);
        if (preferenceMap.containsKey(igp.getPreference())) {
            prefScalar = preferenceMap.get(igp.getPreference());
        }

        int latencyScalar = 10;

        int speedScalar = 1000000;
        if (speed >= 1000000) {
            speedScalar = 10;
        } else if (speed >= 900000) {
            speedScalar = 200;
        } else if (speed >= 800000) {
            speedScalar = 300;
        } else if (speed >= 700000) {
            speedScalar = 400;
        } else if (speed >= 600000) {
            speedScalar = 500;
        } else if (speed >= 500000) {
            speedScalar = 600;
        } else if (speed >= 400000) {
            speedScalar = 700;
        } else if (speed >= 300000) {
            speedScalar = 800;
        } else if (speed >= 200000) {
            speedScalar = 900;
        } else if (speed >= 100000) {
            speedScalar = 1000;
        } else if (speed >= 40000) {
            speedScalar = 70000;

        } else if (speed >= 30000) {
            speedScalar = 80000;
        } else if (speed >= 20000) {
            speedScalar = 90000;
        } else if (speed >= 10000) {
            speedScalar = 100000;

        } else if (speed > 3000) {
            speedScalar = 800000;
        } else if (speed > 2000) {
            speedScalar = 900000;
        } else if (speed > 1000) {
            speedScalar = 1000000;
        }

        int hopMetric = igp.getLatency() * latencyScalar + speedScalar;

        return Math.toIntExact(Math.round(prefScalar * hopMetric));


    }

}
