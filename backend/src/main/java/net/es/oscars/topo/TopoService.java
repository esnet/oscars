package net.es.oscars.topo;

import net.es.oscars.topo.beans.PortBwVlan;
import net.es.oscars.topo.beans.v2.BackbonePort;
import net.es.oscars.topo.beans.v2.Bandwidth;
import net.es.oscars.topo.beans.v2.EdgePort;
import net.es.oscars.topo.beans.v2.VlanAvailability;
import net.es.oscars.topo.enums.Layer;
import net.es.oscars.topo.pop.ConsistencyException;
import net.es.topo.common.model.oscars1.EthernetEncapsulation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TopoService {

    public static EdgePort mapEdgePort(net.es.oscars.topo.beans.Port p,
                                       Map<String, PortBwVlan> available,
                                       Map<String, Map<Integer, Set<String>>> vlanUsageMap) throws ConsistencyException {


        String[] parts = p.getUrn().split(":");
        if (parts.length != 2) {
            throw new ConsistencyException("Invalid port URN format");
        }

        if (!available.containsKey(p.getUrn())) {
            throw new ConsistencyException("cannot get available bw and vlans for " + p.getUrn());
        }
        PortBwVlan pbw = available.get(p.getUrn());

        VlanAvailability vlanAvailability = VlanAvailability.builder()
                .ranges(pbw.getVlanRanges())
                .build();

        EthernetEncapsulation encapsulation;
        if (p.getCapabilities().contains(Layer.UNTAGGED)) {
            encapsulation = EthernetEncapsulation.NULL;
        } else if (p.getCapabilities().contains(Layer.TAGGED)) {
            encapsulation = EthernetEncapsulation.DOT1Q;
        } else {
            throw new ConsistencyException("cannot determine encapsulation for " + p.getUrn());
        }

        // get the least of ingress / egress available
        int bwPhysical = p.getReservableIngressBw();
        if (p.getReservableEgressBw() < bwPhysical) {
            bwPhysical = p.getReservableEgressBw();
        }
        int bwAvailable = pbw.getIngressBandwidth();
        if (pbw.getEgressBandwidth() < bwAvailable) {
            bwAvailable = pbw.getEgressBandwidth();
        }

        Bandwidth bw = Bandwidth.builder()
                .unit(Bandwidth.Unit.MBPS)
                .available(bwAvailable)
                .physical(bwPhysical)
                .build();

        Map<Integer, Set<String>> vlanUsage = new HashMap<>();
        if (vlanUsageMap.containsKey(p.getUrn())) {
            vlanUsage = vlanUsageMap.get(p.getUrn());
        }

        return EdgePort.builder()
                .device(parts[0])
                .name(parts[1])
                .bandwidth(bw)
                .encapsulation(encapsulation)
                .availability(EdgePort.Availability.builder().vlan(vlanAvailability).build())
                .description(p.getTags())
                .esdbEquipmentInterfaceId(p.getEsdbEquipmentInterfaceId())
                .usage(EdgePort.Usage.builder().vlan(vlanUsage).build())
                .build();
    }

    public static BackbonePort mapBackbonePort(net.es.oscars.topo.beans.Port p, Map<String, PortBwVlan> available) throws ConsistencyException {

        String[] parts = p.getUrn().split(":");
        if (parts.length != 2) {
            throw new ConsistencyException("Invalid port URN format");
        }

        // get the least of ingress / egress available
        int bwPhysical = p.getReservableIngressBw();
        if (p.getReservableEgressBw() < bwPhysical) {
            bwPhysical = p.getReservableEgressBw();
        }

        Bandwidth bw = Bandwidth.builder()
                .unit(Bandwidth.Unit.MBPS)
                .physical(bwPhysical)
                .build();

        if (available != null) {
            if (!available.containsKey(p.getUrn())) {
                throw new ConsistencyException("cannot get available bw for " + p.getUrn());
            }
            PortBwVlan pbw = available.get(p.getUrn());

            int bwAvailable = pbw.getIngressBandwidth();
            if (pbw.getEgressBandwidth() < bwAvailable) {
                bwAvailable = pbw.getEgressBandwidth();
            }
            bw.setAvailable(bwAvailable);
        }

        return BackbonePort.builder()
                .device(parts[0])
                .name(parts[1])
                .bandwidth(bw)
                .description(p.getTags())
                .esdbEquipmentInterfaceId(p.getEsdbEquipmentInterfaceId())
                .build();
    }
}

