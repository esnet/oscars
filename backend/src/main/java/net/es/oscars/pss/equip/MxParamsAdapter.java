package net.es.oscars.pss.equip;

import inet.ipaddr.ipv4.IPv4Address;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PSSException;
import net.es.oscars.pss.params.Lsp;
import net.es.oscars.pss.params.MplsHop;
import net.es.oscars.pss.params.MplsPath;
import net.es.oscars.pss.params.Policing;
import net.es.oscars.pss.params.mx.*;
import net.es.oscars.pss.svc.MiscHelper;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.CommandParamIntent;
import net.es.oscars.topo.beans.TopoUrn;
import net.es.oscars.topo.enums.CommandParamType;
import net.es.oscars.topo.enums.UrnType;
import net.es.oscars.topo.svc.TopoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class MxParamsAdapter {

    @Autowired
    private TopoService topoService;

    @Autowired
    private MiscHelper miscHelper;

    // TODO: configurize these
    private final static Integer LSP_WRK_HOLD_PRIORITY = 5;
    private final static Integer LSP_WRK_SETUP_PRIORITY = 5;
    private final static Integer LSP_WRK_METRIC = 65000;
    private final static Integer LSP_PRT_HOLD_PRIORITY = 4;
    private final static Integer LSP_PRT_SETUP_PRIORITY = 4;
    private final static Integer LSP_PRT_METRIC = 65100;


    public MxParams params(Connection c, VlanJunction rvj) throws PSSException {
        Components cmp = c.getReserved().getCmp();

        Integer vcId = null;
        Integer loopbackInt = null;
        Integer protectVcId = null;
        boolean protectEnabled = false;

        for (VlanPipe p : cmp.getPipes()) {
            if (p.getProtect()) {
                protectEnabled = true;
            }
        }

        for (CommandParam rpr : rvj.getCommandParams()) {
            if (rpr.getParamType().equals(CommandParamType.VC_ID)) {
                if (rpr.getIntent() == null) {
                    throw new PSSException("null intent for VC_ID");
                } else if (rpr.getIntent().equals(CommandParamIntent.PRIMARY)) {
                    vcId = rpr.getResource();

                } else if (rpr.getIntent().equals(CommandParamIntent.PROTECT)) {
                    protectVcId = rpr.getResource();

                }
            }
            if (rpr.getParamType().equals(CommandParamType.VPLS_LOOPBACK)) {
                loopbackInt = rpr.getResource();
            }

        }
        if (vcId == null) {
            throw new PSSException("VC id not found!");
        }
        if (protectEnabled && protectVcId == null) {
            log.error("no protect VC id even though protected pipes");
            throw new PSSException("protect VC id not found!");
        }

        List<TaggedIfce> ifces = new ArrayList<>();

        // check if we need to set the vlan-swap; only if there are multiple different VLANs
        Set<Integer> allVlans = new HashSet<>();
        for (VlanFixture rvf : cmp.getFixtures()) {
            allVlans.add(rvf.getVlan().getVlanId());
        }
        boolean vlanSwapNeeded = allVlans.size() > 1;

        for (VlanFixture rvf : cmp.getFixtures()) {
            if (rvf.getJunction().equals(rvj)) {
                Integer vlan = rvf.getVlan().getVlanId();

                TopoUrn urn = topoService.getTopoUrnMap().get(rvf.getPortUrn());
                if (!urn.getUrnType().equals(UrnType.PORT)) {
                    throw new PSSException("invalid urn type");
                }
                String portUrn = urn.getPort().getUrn();
                String[] parts = portUrn.split(":");
                if (parts.length != 2) {
                    throw new PSSException("Invalid port URN format");
                }
                String port = parts[1];
                TaggedIfce ti = TaggedIfce.builder()
                        .port(port)
                        .vlan(vlan)
                        .vlanSwap(vlanSwapNeeded)
                        .description("OSCARS-" + c.getConnectionId() + ":0:oscars-l2circuit:show:circuit-intercloud")
                        .build();
                ifces.add(ti);
            }
        }

        String description = c.getConnectionId();
        String serviceName = description + "-SVC";
        String policyName = c.getConnectionId() + "-POLICY";
        String statsFilter = c.getConnectionId() + "-STATS";


        List<MxLsp> lsps = new ArrayList<>();
        List<MplsPath> paths = new ArrayList<>();
        List<MxQos> qos = new ArrayList<>();

        boolean isInPipes = false;
        for (VlanPipe p : cmp.getPipes()) {
            VlanJunction other_j = null;
            Integer mbps = null;
            List<EroHop> hops = null;

            if (p.getA().getDeviceUrn().equals(rvj.getDeviceUrn())) {
                hops = p.getAzERO();
                mbps = p.getAzBandwidth();
                other_j = p.getZ();
                isInPipes = true;
            } else if (p.getZ().getDeviceUrn().equals(rvj.getDeviceUrn())) {
                other_j = p.getA();
                mbps = p.getZaBandwidth();
                hops = p.getZaERO();
                isInPipes = true;
            }
            if (hops != null) {
                MxPipeResult primary = this.makePipe(other_j, hops, false, p, mbps, c);
                paths.add(primary.getPath());
                lsps.add(primary.getMxLsp());
                qos.add(primary.getQos());
                if (protectEnabled) {
                    MxPipeResult protect = this.makePipe(other_j, hops, true, p, mbps, c);
                    paths.add(protect.getPath());
                    lsps.add(protect.getMxLsp());
                    qos.add(protect.getQos());
                }
            }
        }

        String loopback = null;
        if (isInPipes) {
            if (loopbackInt == null) {
                throw new PSSException("no loopback reserved for " + rvj.getDeviceUrn());
            }
            IPv4Address address = new IPv4Address(loopbackInt);
            loopback = address.toString();
        }
        Integer communityId = vcId;
        String communityName = "OSCARS-"+c.getConnectionId()+"-"+communityId;


        MxVpls mxVpls = MxVpls.builder()
                .vcId(vcId)
                .protectVcId(protectVcId)
                .protectEnabled(protectEnabled)
                .description(description)
                .serviceName(serviceName)
                .policyName(policyName)
                .communityId(communityId)
                .communityName(communityName)
                .statsFilter(statsFilter)
                .loopback(loopback)
                .mtu(c.getConnection_mtu() + 100)
                .build();

        return MxParams.builder()
                .ifces(ifces)
                .lsps(lsps)
                .paths(paths)
                .qos(qos)
                .mxVpls(mxVpls)
                .build();
    }


    @Data
    private class MxPipeResult {
        private MxLsp mxLsp;
        private MplsPath path;
        private MxQos qos;
    }


    public MxPipeResult makePipe(VlanJunction otherJ, List<EroHop> hops, boolean protect,
                                 VlanPipe p, Integer mbps, Connection c) throws PSSException {
        List<MplsHop> mplsHops = new ArrayList<>();
        if (!protect) {
            mplsHops = miscHelper.mplsHops(hops);
        }

        String pathName = c.getConnectionId();
        String lspName = c.getConnectionId();

        if (protect) {
            pathName += "-PRT-" + otherJ.getDeviceUrn();
            lspName += "-PRT-" + otherJ.getDeviceUrn();
        } else {
            pathName += "-WRK-" + otherJ.getDeviceUrn();
            lspName += "-WRK-" + otherJ.getDeviceUrn();
        }
        MplsPath path = MplsPath.builder()
                .hops(mplsHops)
                .name(pathName)
                .build();

        Integer otherLoopbackInt = null;
        for (CommandParam rpr : otherJ.getCommandParams()) {
            if (rpr.getParamType().equals(CommandParamType.VPLS_LOOPBACK)) {
                otherLoopbackInt = rpr.getResource();
            }
        }
        if (otherLoopbackInt == null) {
            log.error("no loopback found for " + otherJ.getDeviceUrn());
            throw new PSSException("no loopback found for " + otherJ.getDeviceUrn());
        }
        IPv4Address otherLoopback = new IPv4Address(otherLoopbackInt);

        TopoUrn otherDevice = topoService.getTopoUrnMap().get(otherJ.getDeviceUrn());
        if (otherDevice == null || !otherDevice.getUrnType().equals(UrnType.DEVICE)) {
            throw new PSSException("invalid other device");
        }
        String otherAddr = otherDevice.getDevice().getIpv4Address();

        String filterName = c.getConnectionId();
        String policerName =  c.getConnectionId();
        Integer holdPriority;
        Integer setupPriority;
        Integer lspMetric;
        Policing policing;
        MxQosForwarding forwarding;
        boolean createPolicer;

        if (protect) {
            filterName += "-PRT-" + otherDevice.getUrn();
            policerName += "-PRT-" + otherDevice.getUrn();
            holdPriority = LSP_PRT_HOLD_PRIORITY;
            setupPriority = LSP_PRT_SETUP_PRIORITY;
            lspMetric = LSP_PRT_METRIC;
            forwarding = MxQosForwarding.BEST_EFFORT;
            policing = Policing.SOFT;
            createPolicer = false;
        } else {
            filterName += "-WRK-" + otherDevice.getUrn();
            policerName += "-WRK-" + otherDevice.getUrn();
            holdPriority = LSP_WRK_HOLD_PRIORITY;
            setupPriority = LSP_WRK_SETUP_PRIORITY;
            lspMetric = LSP_WRK_METRIC;
            forwarding = MxQosForwarding.EXPEDITED;
            policing = Policing.SOFT;
            createPolicer = true;
        }


        Lsp lsp = Lsp.builder()
                .holdPriority(holdPriority)
                .setupPriority(setupPriority)
                .metric(lspMetric)
                .to(otherLoopback.toString())
                .pathName(path.getName())
                .name(lspName)
                .build();


        MxQos qos = MxQos.builder()
                .createPolicer(createPolicer)
                .filterName(filterName)
                .forwarding(forwarding)
                .mbps(mbps)
                .policerName(policerName)
                .policing(policing)
                .build();

        MxLsp mxLsp = MxLsp.builder()
                .lsp(lsp)
                .primary(!protect)
                .neighbor(otherAddr)
                .policeFilter(filterName)
                .build();

        MxPipeResult pr = new MxPipeResult();
        pr.setMxLsp(mxLsp);
        pr.setPath(path);
        pr.setQos(qos);
        return pr;

    }
}