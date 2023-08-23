package net.es.oscars.esdb;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.resv.ent.Components;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.ent.VlanFixture;
import net.es.oscars.topo.db.PortRepository;
import net.es.oscars.topo.ent.Port;
import net.es.topo.common.dto.esdb.EsdbVlan;
import net.es.topo.common.dto.esdb.EsdbVlanPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class ESDBService {
    @Autowired
    private ESDBProxy esdbProxy;

    @Autowired
    private PortRepository portRepo;

    public void reserveEsdbVlans(Connection c) {
        log.info("Reserving ESDB vlans for: " + c.getConnectionId());
        for (EsdbVlanPayload payload : makeVlanPayloads(c)) {
            esdbProxy.createVlan(payload);
        }
    }
    public void releaseEsdbVlans(Connection c) {
        log.info("Releasing ESDB vlans for: " + c.getConnectionId());
        List<EsdbVlan> allEsdbVlans = esdbProxy.getAllEsdbVlans();
        for (EsdbVlanPayload payload : makeVlanPayloads(c)) {
            boolean deletedIt = false;
            for (EsdbVlan vlan : allEsdbVlans) {
                if (vlan.getVlanId().equals(payload.getVlanId()) &&
                        vlan.getEquipmentInterface().equals(payload.getEquipmentInterface())) {
                    esdbProxy.deleteVlan(vlan.getId());
                    log.info("Deleted "+vlan.getId());
                    deletedIt = true;
                    break;
                }
            }
            if (!deletedIt) {
                log.warn("Could not find vlan_id: "+payload.getVlanId()+" equipnment_interface: "+payload.getEquipmentInterface());
            }

        }

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
            Optional<Port> maybePort =  portRepo.findByUrn(portUrn);
            maybePort.ifPresent(port -> {
                payloads.add(EsdbVlanPayload.builder()
                        .vlanId(f.getVlan().getVlanId())
                        .description("OSCARS "+c.getConnectionId()+" ("+c.getDescription()+")")
                        .equipment(port.getDevice().getEsdbEquipmentId())
                        .equipmentInterface(port.getEsdbEquipmentInterfaceId())
                        .build());
            });
        }

        return payloads;
    }


}
