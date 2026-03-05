package net.es.oscars.task;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.props.EsdbProperties;
import net.es.oscars.app.props.StartupProperties;
import net.es.oscars.esdb.ESDBProxy;
import net.es.oscars.model.Interval;
import net.es.oscars.resv.beans.PeriodBandwidth;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.Components;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.ent.VlanFixture;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.svc.ResvLibrary;
import net.es.oscars.resv.svc.ResvService;
import net.es.oscars.topo.beans.Port;
import net.es.oscars.topo.beans.PortBwVlan;
import net.es.oscars.topo.beans.TopoUrn;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.pop.ConsistencyException;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.topo.common.dto.esdb.EsdbBwUtil;
import net.es.topo.common.dto.esdb.EsdbBwUtilPayload;
import net.es.topo.common.dto.esdb.EsdbVlan;
import net.es.topo.common.dto.esdb.EsdbVlanPayload;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Component
@Getter
@Setter
public class EsdbDataSync {
    public static String PREFIX = "OSCARS";
    private final Startup startup;
    private final ConnectionRepository cr;
    private final ESDBProxy esdbProxy;
    private final EsdbProperties esdbProperties;
    private final StartupProperties startupProperties;
    private final TopologyStore topologyStore;
    private final ResvService resvService;

    private boolean isSynchronized = false;
    public EsdbDataSync(Startup startup, ConnectionRepository cr, ESDBProxy esdbProxy,
                        EsdbProperties esdbProperties, StartupProperties startupProperties,
                        TopologyStore topologyStore, ResvService resvService) {
        this.startup = startup;
        this.cr = cr;
        this.esdbProxy = esdbProxy;
        this.esdbProperties = esdbProperties;
        this.startupProperties = startupProperties;
        this.topologyStore = topologyStore;
        this.resvService = resvService;
    }

    @Scheduled(initialDelay = 120000, fixedDelayString ="${esdb.vlan-sync-period}" )
    public void processingLoop() {
        isSynchronized = false;
        if (!esdbProperties.isEnabled()) {
            return;
        } else if (startupProperties.getStandalone()) {
            return;
        } else if (startup.isInStartup() || startup.isInShutdown()) {
            return;
        }
        log.info("starting BW utilization sync");
        List<EsdbBwUtil> allExistingBwUtils = esdbProxy.gqlBwUtil(null, null, null, null);
        List<EsdbBwUtil> existingOscarsBwUtils = allExistingBwUtils.stream()
                .filter(util -> util.getSystem().equals("oscars"))
                .toList();
        Map<Integer, EsdbBwUtil> existingBwUtilsByIfceId = existingOscarsBwUtils
                .stream()
                .collect(Collectors.toMap(EsdbBwUtil::getEquipmentInterface, item -> item));

        try {
            topologyStore.getCurrentTopology();
        } catch (ConsistencyException e) {
            log.error("failed to get current topology for bandwidth sync", e);
        }

        List<EsdbBwUtilPayload> updatedOscarsBwUtils = this.makeBwPayloads();
        Map<Integer, EsdbBwUtilPayload> updatedBwUtilsByIfceId = updatedOscarsBwUtils
                .stream()
                .collect(Collectors.toMap(EsdbBwUtilPayload::getEquipmentInterface, p -> p));

        Set<Integer> bwUtilsToRemove = new HashSet<>();

        for (Integer ifceId : existingBwUtilsByIfceId.keySet()) {
            // first check if any items existing in ESDB have ifceids that are no longer around; those must be deleted
            if (!updatedBwUtilsByIfceId.containsKey(ifceId)) {
                bwUtilsToRemove.add(existingBwUtilsByIfceId.get(ifceId).getId());
            }
        }

        List<EsdbBwUtilPayload> bwUtilsToAdd = new ArrayList<>();
        for (Integer ifceId : updatedBwUtilsByIfceId.keySet()) {
            // first check if any of the new ifce ids were not previously existing in ESDB; those must be added
            if (!existingBwUtilsByIfceId.containsKey(ifceId)) {
                bwUtilsToAdd.add(updatedBwUtilsByIfceId.get(ifceId));
            } else {
                // now check if the bandwidth has changed; we will delete and re-add these
                EsdbBwUtilPayload newPayload = updatedBwUtilsByIfceId.get(ifceId);
                EsdbBwUtil existingBwUtil = existingBwUtilsByIfceId.get(ifceId);
                if (!existingBwUtil.getBandwidth().equals(newPayload.getBandwidth())) {
                    bwUtilsToRemove.add(existingBwUtilsByIfceId.get(ifceId).getId());
                    bwUtilsToAdd.add(updatedBwUtilsByIfceId.get(ifceId));
                }
            }
        }


        if (!bwUtilsToRemove.isEmpty()) {
            log.info("deleting {}  OSCARS utilizations ", bwUtilsToRemove.size());
            for (Integer bwUtilId : bwUtilsToRemove) {
                esdbProxy.deleteBandwidthUtilization(bwUtilId);
            }
        }

        if (!bwUtilsToAdd.isEmpty()) {
            log.info("creating {} OSCARS bandwidth utilizations ", bwUtilsToAdd.size());
            for (EsdbBwUtilPayload bwUtilPayload : bwUtilsToAdd) {
                esdbProxy.createBandwidthUtilization(bwUtilPayload);
            }
        }


        log.info("starting VLAN sync");
        // fetch all ESDB vlans
        // ...Use GraphQL client.
        List<EsdbVlan> currentEsdbVlans = esdbProxy.gqlVlanList();

        // generate all the EsdbVlanPayloads from our RESERVED connections
        Set<EsdbVlanPayload> fromReserved = new HashSet<>();
        for (Connection c : cr.findByPhase(Phase.RESERVED)) {
            fromReserved.addAll(makeVlanPayloads(c));
        }

        Set<EsdbVlan> keep = new HashSet<>();
        Set<EsdbVlan> delete = new HashSet<>();

        // decide which EsdbVLans should be deleted and which kept
        // for each one from ESDB, it is kept only if it matches
        // the port and vlan-id of a RESERVED OSCARS connection
        for (EsdbVlan ev : currentEsdbVlans) {
            if (ev.getDescription() == null || !ev.getDescription().startsWith(PREFIX)) {
                continue;
            }
            boolean shouldDelete = true;
            for (EsdbVlanPayload payload : fromReserved) {
                if (vlanMatchesPayload(ev, payload)) {
                    shouldDelete = false;
                    break;
                }
            }
            if (shouldDelete) {
                delete.add(ev);
            } else {
                keep.add(ev);
            }
        }
        // delete all EsdbVlans that need to be deleted...
        for (EsdbVlan ev : delete) {
            log.info("deleting ESDB vlan "+ev.getId());
            esdbProxy.deleteVlan(ev.getId());
        }

        // Decide what should be added
        //
        // A bit more complicated:
        // We might have entries in our fromReserved set that have identical
        // (equipmentInterface, vlanId) data - but these are represented by
        // a single EsdbVlan entry.
        // We stuff them in a map keyed by that tuple, so we can pull just the value-set out
        //
        // possible problem: the description might become out-of-date

        Map<Pair<Integer, Integer>, EsdbVlanPayload> payloadsByEqIfceAndVlanId = new HashMap<>();
        for (EsdbVlanPayload evp : fromReserved) {
            Pair<Integer, Integer> key = Pair.of(evp.getEquipmentInterface(), evp.getVlanId());
            payloadsByEqIfceAndVlanId.put(key, evp);
        }
        // if a payload should be there but is not, we add it to our "add" set"
        Set<EsdbVlanPayload> add = new HashSet<>();
        for (EsdbVlanPayload evp : payloadsByEqIfceAndVlanId.values()) {
            boolean found = false;
            // check the payload against all esdbVlans
            for (EsdbVlan esdbVlan : keep) {
                if (vlanMatchesPayload(esdbVlan, evp)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                add.add(evp);
            }
        }
        // now add all EsdbVlanPayloads that need to be added
        for (EsdbVlanPayload evp: add) {
            // ESDB doesn't let us create VLANS with id -
            if (evp.getVlanId() == 0) {
                log.info("skipping a vlan on an untagged port, equipIfceId: "+evp.getEquipmentInterface());
                continue;
            }

            esdbProxy.createVlan(evp);
        }

        isSynchronized = true;
        log.info("ESDB VLAN synchronization complete!");
    }

    public static boolean vlanMatchesPayload(EsdbVlan esdbVlan, EsdbVlanPayload payload) {
        return esdbVlan.getVlanId().equals(payload.getVlanId()) && esdbVlan.getEquipmentInterface().equals(payload.getEquipmentInterface());
    }
    public List<EsdbVlanPayload> makeVlanPayloads(Connection c) {
        List<EsdbVlanPayload> payloads = new ArrayList<>();

        Components cmp;
        if (c.getReserved() != null) {
            cmp = c.getArchived().getCmp();
        } else {
            cmp = c.getArchived().getCmp();
        }
        for (VlanFixture f : cmp.getFixtures()) {
            String portUrn = f.getPortUrn();
            Optional<Port> maybePort = topologyStore.findPortByUrn(portUrn);
            maybePort.ifPresent(port -> payloads.add(EsdbVlanPayload.builder()
                    .vlanId(f.getVlan().getVlanId())
                    .description(PREFIX+" " + c.getConnectionId() + " (" + c.getDescription() + ")")
                    .equipment(port.getDevice().getEsdbEquipmentId())
                    .equipmentInterface(port.getEsdbEquipmentInterfaceId())
                    .build()));
        }

        return payloads;
    }

    public List<EsdbBwUtilPayload> makeBwPayloads() {
        List<EsdbBwUtilPayload> payloads = new ArrayList<>();
        // a random UUID for this run
        UUID uuid = UUID.randomUUID();

        Interval thisWeek = Interval.builder()
                .beginning(Instant.now())
                .ending(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        // first we collect any reservations overlapping the next week
        // addy any ports that have a non-zero reservation to a set
        Set<String> portsWithAnyReservations = new HashSet<>();
        Map<String, List<PeriodBandwidth>> reservedIngBws = resvService.reservedIngBws(thisWeek, new HashMap<>(), null);
        for (String portUrn : reservedIngBws.keySet()) {
            for (PeriodBandwidth bw : reservedIngBws.get(portUrn)) {
                if (bw.getBandwidth() > 0) {
                    portsWithAnyReservations.add(portUrn);
                }
            }
        }

        Map<String, List<PeriodBandwidth>> reservedEgBws = resvService.reservedEgBws(thisWeek, new HashMap<>(), null);


        Map<String, TopoUrn> topoUrnMap = topologyStore.getTopoUrnMap();
        Map<String, PortBwVlan> available = ResvLibrary.portBwVlans(topoUrnMap, new ArrayList<>(), reservedIngBws, reservedEgBws);

        for (String portUrn : portsWithAnyReservations) {
            Optional<Port> p = topologyStore.findPortByUrn(portUrn);
            if (p.isPresent()) {
                Port port = p.get();
                Integer availableBw = available.get(portUrn).getIngressBandwidth();
                Integer baselineBw = port.getReservableIngressBw();
                Integer reservedBw = baselineBw - availableBw;
                EsdbBwUtilPayload payload = EsdbBwUtilPayload.builder()
                        .bandwidth(reservedBw)
                        .system("oscars")
                        .bandwidth(reservedBw)
                        .equipmentInterface(port.getEsdbEquipmentInterfaceId())
                        .remoteSystemId(uuid.toString())
                        .build();
                payloads.add(payload);
            } else {
                log.error("portUrn: {} not found!", portUrn);
            }
        }

        return payloads;
    }

}