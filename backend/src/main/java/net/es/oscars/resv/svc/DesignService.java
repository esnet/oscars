package net.es.oscars.resv.svc;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.resv.beans.DesignResponse;
import net.es.oscars.resv.ent.*;
import net.es.oscars.topo.beans.IntRange;
import net.es.oscars.topo.beans.TopoUrn;
import net.es.oscars.topo.enums.UrnType;
import net.es.oscars.topo.svc.TopologyStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Component
@Slf4j
public class DesignService {
    @Autowired
    private TopologyStore topologyStore;

    public DesignResponse verifyDesign(Design design) {
        Map<String, TopoUrn> topoUrnMap = topologyStore.getTopoUrnMap();

        List<String > errors = new ArrayList<>();
        boolean valid = true;

        if (design.getDesignId() == null || design.getDesignId().length() == 0) {
            errors.add("designId null or empty");
            valid = false;
        }
        if (design.getDescription() == null || design.getDescription().length() == 0) {
            errors.add("Description null or empty");
            valid = false;
        }
        if (design.getUsername() == null || design.getUsername().length() == 0) {
            errors.add("username null or empty");
            valid = false;
        }

        Components cmp = design.getCmp();
        for (VlanJunction j: cmp.getJunctions()) {
            if (topoUrnMap.containsKey(j.getDeviceUrn())) {
                TopoUrn topoUrn = topoUrnMap.get(j.getDeviceUrn());
                if (!topoUrn.getUrnType().equals(UrnType.DEVICE)) {
                    errors.add("junction URN "+j.getDeviceUrn()+" does not point to device ");
                    valid = false;
                }
            } else {
                errors.add("junction URN "+j.getDeviceUrn()+" not found in topology");
                valid = false;
            }
        }
        for (VlanFixture f: cmp.getFixtures()) {
            if (topoUrnMap.containsKey(f.getPortUrn())) {
                TopoUrn topoUrn = topoUrnMap.get(f.getPortUrn());
                if (!topoUrn.getUrnType().equals(UrnType.PORT)) {
                    errors.add("fixture port URN "+f.getPortUrn()+" does not point to a port");
                    valid = false;
                }
                if (topoUrn.getReservableIngressBw() == null) {
                    errors.add("fixture "+f.getPortUrn()+" requests ingress bandwidth where none is defined (incorrect URN type?)");
                    valid = false;
                } else if (topoUrn.getReservableIngressBw() < f.getIngressBandwidth()) {
                    errors.add("fixture "+f.getPortUrn()+" requests excessive ingress bandwidth");
                    valid = false;
                }
                if (topoUrn.getReservableEgressBw() == null) {
                    errors.add("fixture " + f.getPortUrn() + " requests egress bandwidth where none is defined (incorrect URN type?)");
                    valid = false;
                } else if (topoUrn.getReservableEgressBw() < f.getEgressBandwidth()) {
                    errors.add("fixture "+f.getPortUrn()+" requests excessive egress bandwidth");
                    valid = false;
                }

                boolean vlanIsInReservableRange = false;
                for (IntRange ir : topoUrn.getReservableVlans()) {
                    if (ir.contains(f.getVlan().getVlanId())) {
                        vlanIsInReservableRange = true;
                    }
                }
                if (!vlanIsInReservableRange) {
                    errors.add("fixture "+f.getPortUrn()+" vlan "+f.getVlan().getVlanId()+" is not reservable");
                    valid = false;
                }

            } else {
                errors.add("fixture port URN "+f.getPortUrn()+" not found in topology");
                valid = false;
            }
        }

        return DesignResponse.builder()
                .valid(valid)
                .errors(errors)
                .design(design)
                .build();
    }



}
