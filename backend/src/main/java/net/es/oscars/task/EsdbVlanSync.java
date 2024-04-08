package net.es.oscars.task;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.props.EsdbProperties;
import net.es.oscars.app.props.StartupProperties;
import net.es.oscars.esdb.ESDBProxy;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.Components;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.ent.VlanFixture;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.topo.beans.Port;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.topo.common.devel.DevelUtils;
import net.es.topo.common.dto.esdb.EsdbVlan;
import net.es.topo.common.dto.esdb.EsdbVlanPayload;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;


@Slf4j
@Component
public class EsdbVlanSync {
    public static String PREFIX = "OSCARS";
    private final Startup startup;
    private final ConnectionRepository cr;
    private final ESDBProxy esdbProxy;
    private final EsdbProperties esdbProperties;
    private final StartupProperties startupProperties;
    private final TopologyStore topologyStore;

    public EsdbVlanSync(Startup startup, ConnectionRepository cr, ESDBProxy esdbProxy, EsdbProperties esdbProperties, StartupProperties startupProperties, TopologyStore topologyStore) {
        this.startup = startup;
        this.cr = cr;
        this.esdbProxy = esdbProxy;
        this.esdbProperties = esdbProperties;
        this.startupProperties = startupProperties;
        this.topologyStore = topologyStore;
    }

    @Scheduled(initialDelay = 120000, fixedDelayString ="${esdb.vlan-sync-period}" )
    public void processingLoop() {
        if (!esdbProperties.isEnabled()) {
            return;
        } else if (startupProperties.getStandalone()) {
            return;
        } else if (startup.isInStartup() || startup.isInShutdown()) {
            return;
        }
        log.debug("starting VLAN sync");
        // fetch all ESDB vlans
        List<EsdbVlan> currentEsdbVlans = esdbProxy.getAllEsdbVlans();

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
            DevelUtils.dumpDebug("creating an ESDB vlan", evp);

            esdbProxy.createVlan(evp);
        }
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
}