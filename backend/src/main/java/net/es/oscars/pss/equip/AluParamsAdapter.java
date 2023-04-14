package net.es.oscars.pss.equip;

import inet.ipaddr.ipv4.IPv4Address;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PSSException;
import net.es.oscars.pss.params.Lsp;
import net.es.oscars.pss.params.MplsHop;
import net.es.oscars.pss.params.MplsPath;
import net.es.oscars.pss.params.Policing;
import net.es.oscars.pss.params.alu.*;
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
import java.util.List;

@Component
@Slf4j
public class AluParamsAdapter {

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

    public AluParams params(Connection c, VlanJunction rvj, boolean aluSuppressStandbySignaling) throws PSSException {
        Integer aluSvcId = null;
        log.info("making ALU params");
        Integer loopback = null;
        Integer protectVcId = null;
        boolean protectEnabled = false;
        Components cmp = c.getReserved().getCmp();
        for (VlanPipe p : cmp.getPipes()) {
            if (p.getProtect()) {
                protectEnabled = true;
            }
        }

        for (CommandParam rpr : rvj.getCommandParams()) {
            if (rpr.getParamType().equals(CommandParamType.ALU_SVC_ID)) {
                aluSvcId = rpr.getResource();
            }
            if (rpr.getParamType().equals(CommandParamType.VC_ID)) {
                if (rpr.getIntent() == null) {
                    throw new PSSException("null intent for svc id!");
                } else if (rpr.getIntent().equals(CommandParamIntent.PROTECT)) {
                    protectVcId = rpr.getResource();
                }
            }
            if (rpr.getParamType().equals(CommandParamType.VPLS_LOOPBACK)) {
                loopback = rpr.getResource();
            }
        }
        if (aluSvcId == null) {
            log.error("no ALU SVC ID");
            throw new PSSException("ALU svc id not found!");
        }
        if (protectEnabled && protectVcId == null) {
            log.error("no protect VC id even though protected pipes");
            throw new PSSException("protect VC id not found!");

        }

        List<AluQos> qoses = new ArrayList<>();
        List<AluSap> saps = new ArrayList<>();

        for (VlanFixture rvf : cmp.getFixtures()) {
            if (rvf.getJunction().equals(rvj)) {
                Integer inQosId = null;
                Integer egQosId = null;
                Integer vlan = rvf.getVlan().getVlanId();
                for (CommandParam cp : rvf.getCommandParams()) {
                    if (cp.getParamType().equals(CommandParamType.ALU_QOS_POLICY_ID)) {
                        inQosId = cp.getResource();
                        egQosId = cp.getResource();
                    }
                }

                if (inQosId == null || egQosId == null) {
                    throw new PSSException("in / eg qosId not reserved");
                }

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
                Policing policing = Policing.STRICT;
                if (!rvf.getStrict()) {
                    policing = Policing.SOFT;
                }

                String configPortStr = port.replace('/', '_') + '_' + vlan;
                AluQos inQos = AluQos.builder()
                        .description(c.getConnectionId())
                        .mbps(rvf.getIngressBandwidth())
                        .policing(policing)
                        .policyId(inQosId)
                        .policyName("IN-" + c.getConnectionId() + "-" + configPortStr)
                        .type(AluQosType.SAP_INGRESS)
                        .build();
                qoses.add(inQos);
                AluQos egQos = AluQos.builder()
                        .description(c.getConnectionId())
                        .mbps(rvf.getEgressBandwidth())
                        .policing(policing)
                        .policyId(egQosId)
                        .policyName("EG-" + c.getConnectionId() + "-" + configPortStr)
                        .type(AluQosType.SAP_EGRESS)
                        .build();
                qoses.add(egQos);

                AluSap sap = AluSap.builder()
                        .vlan(vlan)
                        .ingressQosId(inQosId)
                        .egressQosId(egQosId)
                        .port(port)
                        .description("OSCARS-" + c.getConnectionId() + ":0:oscars-l2circuit:show:circuit-intercloud")
                        .build();
                saps.add(sap);
            }
        }

        AluVpls vpls = AluVpls.builder()
                .protectVcId(protectVcId)
                .suppressStandby(aluSuppressStandbySignaling)
                .endpointNames(new ArrayList<>())
                .protectEnabled(protectEnabled)
                .description("OSCARS-" + c.getConnectionId() + "-VPLS")
                .saps(saps)
                .serviceName("OSCARS-" + c.getConnectionId() + "-SVC")
                .sdpToVcIds(new ArrayList<>())
                .svcId(aluSvcId)
                .mtu(c.getConnection_mtu() + 114)
                .build();


        List<Lsp> lsps = new ArrayList<>();
        List<AluSdp> sdps = new ArrayList<>();
        List<MplsPath> paths = new ArrayList<>();

        // for each pipe we need:
        // - an LSP to specify the hop-by-hop route over the network
        // - a Path that uses the LSP
        // - an SDP to use the path
        boolean isInPipes = false;
        for (VlanPipe p : cmp.getPipes()) {
            VlanJunction other_j = null;
            List<EroHop> hops = null;

            if (p.getA().getDeviceUrn().equals(rvj.getDeviceUrn())) {
                other_j = p.getZ();
                hops = p.getAzERO();
                isInPipes = true;
            } else if (p.getZ().getDeviceUrn().equals(rvj.getDeviceUrn())) {
                other_j = p.getA();
                hops = p.getZaERO();
                isInPipes = true;
            }

            if (other_j == null) {
                throw new PSSException("unable to locate other end of junction");
            }
            vpls.getEndpointNames().add((c.getConnectionId()+"-"+other_j.getDeviceUrn()+"-endpoint"));

            if (hops != null) {
                List<AluPipeResult> aluPipes = this.makePipe(other_j, hops, p, rvj, c, vpls);
                for (AluPipeResult pr : aluPipes) {
                    sdps.add(pr.getSdp());
                    paths.add(pr.getPath());
                    lsps.add(pr.getLsp());
                }
            }
        }

        AluParams params = AluParams.builder()
                .applyQos(true)
                .lsps(lsps)
                .paths(paths)
                .sdps(sdps)
                .qoses(qoses)
                .aluVpls(vpls)
                .build();

        if (isInPipes) {
            if (loopback == null) {
                throw new PSSException("no loopback reserved for " + rvj.getDeviceUrn());
            }
            IPv4Address address = new IPv4Address(loopback);
            params.setLoopbackInterface("lo0-" + c.getConnectionId());
            params.setLoopbackAddress(address.toString());
        }

        return params;
    }

    @Data
    private class AluPipeResult {
        private MplsPath path;
        private Lsp lsp;
        private AluSdp sdp;
    }

    public List<AluPipeResult> makePipe(VlanJunction otherJunction, List<EroHop> hops, VlanPipe p, VlanJunction j, Connection c, AluVpls vpls) throws PSSException {
        List<AluPipeResult> aluPipes = new ArrayList<>();

        List<MplsHop> mplsHops = miscHelper.mplsHops(hops);
        String pathName = c.getConnectionId() + "-WRK-" + otherJunction.getDeviceUrn();
        if (pathName.length() > 32) {
            pathName = pathName.substring(0, 31);
            log.warn("path name trimmed to: " + pathName);
        }

        String lspName = pathName;

        MplsPath path = MplsPath.builder()
                .hops(mplsHops)
                .name(pathName)
                .build();

        TopoUrn deviceTopoUrn = topoService.getTopoUrnMap().get(otherJunction.getDeviceUrn());
        String remoteLoopback = deviceTopoUrn.getDevice().getIpv4Address();
        /*
        Integer otherLoopbackInt = null;
        for (CommandParam rpr : otherJunction.getCommandParams()) {
            if (rpr.getParamType().equals(CommandParamType.VPLS_LOOPBACK)) {
                otherLoopbackInt = rpr.getResource();
            }
        }
        if (otherLoopbackInt == null) {
            log.error("no loopback found for "+otherJunction.getDeviceUrn());
            throw new PSSException("no loopback found for "+otherJunction.getDeviceUrn());
        }
        IPv4Address otherLoopback = new IPv4Address(otherLoopbackInt);
        */

        Lsp lsp = Lsp.builder()
                .holdPriority(LSP_WRK_HOLD_PRIORITY)
                .setupPriority(LSP_WRK_SETUP_PRIORITY)
                .metric(LSP_WRK_METRIC)
                .to(remoteLoopback)
                .pathName(path.getName())
                .name(lspName)
                .build();

        Integer sdpId = null;
        Integer protectSdpId = null;
        for (CommandParam cp : j.getCommandParams()) {
            if (cp.getParamType().equals(CommandParamType.ALU_SDP_ID)) {
                if (cp.getIntent() == null) {
                    throw new PSSException("null ALU_SDP intent!");

                } else if (cp.getIntent().equals(CommandParamIntent.PRIMARY)) {
                    if (cp.getTarget().equals(otherJunction.getDeviceUrn())) {
                        sdpId = cp.getResource();
                    }

                } else if (cp.getIntent().equals(CommandParamIntent.PROTECT)) {
                    if (cp.getTarget().equals(otherJunction.getDeviceUrn())) {
                        protectSdpId = cp.getResource();
                    }

                }
            }
        }

        if (sdpId == null) {
            throw new PSSException("no sdp id reserved!");
        }
        String sdpDescription = c.getConnectionId() + "-WRK-" + otherJunction.getDeviceUrn();


        AluSdp sdp = AluSdp.builder()
                .sdpId(sdpId)
                .description(sdpDescription)
                .farEnd(remoteLoopback)
                .lspName(lsp.getName())
                .build();


        AluPipeResult pr = new AluPipeResult();
        pr.setLsp(lsp);
        pr.setPath(path);
        pr.setSdp(sdp);
        aluPipes.add(pr);

        // bleh, side effect! but can't help it
        AluSdpToVcId sdpToVcId = AluSdpToVcId.builder()
                .sdpId(sdpId)
                .primary(true)
                .besteffort(false)
                .vcId(vpls.getSvcId())
                .build();
        vpls.getSdpToVcIds().add(sdpToVcId);


        if (vpls.getProtectEnabled()) {
            if (protectSdpId == null) {
                throw new PSSException("no protect SDP id reserved!");
            }
            String prtPathName = c.getConnectionId() + "-PRT-" + otherJunction.getDeviceUrn();
            if (prtPathName.length() > 32) {
                prtPathName = prtPathName.substring(0, 31);
                log.warn("path name trimmed to: " + prtPathName);
            }

            String prtLspName = prtPathName;
            String prtSdpDescription = c.getConnectionId() + "-PRT-" + otherJunction.getDeviceUrn();

            MplsPath protectPath = MplsPath.builder()
                    .hops(new ArrayList<>())
                    .name(prtPathName)
                    .build();

            Lsp protectLsp = Lsp.builder()
                    .holdPriority(LSP_PRT_HOLD_PRIORITY)
                    .setupPriority(LSP_PRT_SETUP_PRIORITY)
                    .metric(LSP_PRT_METRIC)
                    .to(remoteLoopback)
                    .pathName(protectPath.getName())
                    .name(prtLspName)
                    .build();
            AluSdp protectSdp = AluSdp.builder()
                    .sdpId(protectSdpId)
                    .description(prtSdpDescription)
                    .farEnd(remoteLoopback)
                    .lspName(protectLsp.getName())
                    .build();
            AluPipeResult protectAluPipe = new AluPipeResult();
            protectAluPipe.setLsp(protectLsp);
            protectAluPipe.setPath(protectPath);
            protectAluPipe.setSdp(protectSdp);

            AluSdpToVcId protectSdpToVcId = AluSdpToVcId.builder()
                    .sdpId(protectSdpId)
                    .primary(false)
                    .besteffort(true)
                    .vcId(vpls.getProtectVcId()).build();
            vpls.getSdpToVcIds().add(protectSdpToVcId);


            aluPipes.add(protectAluPipe);
        }

        return aluPipes;

    }


}