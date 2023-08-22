package net.es.oscars.nso.rest;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.NsoException;
import net.es.oscars.app.exc.PSSException;
import net.es.oscars.dto.pss.cmd.CommandType;
import net.es.oscars.nso.db.NsoQosSapPolicyIdDAO;
import net.es.oscars.nso.db.NsoSdpIdDAO;
import net.es.oscars.nso.db.NsoVcIdDAO;
import net.es.oscars.nso.ent.NsoQosSapPolicyId;
import net.es.oscars.nso.ent.NsoSdpId;
import net.es.oscars.pss.params.MplsHop;
import net.es.oscars.pss.svc.MiscHelper;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.DeploymentState;
import net.es.oscars.resv.enums.State;
import net.es.oscars.sb.SouthboundTaskResult;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.NsoVPLS;
import net.es.topo.common.dto.nso.enums.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class NsoAdapter {
    @Autowired
    private NsoProxy nsoProxy;

    @Autowired
    private NsoVcIdDAO nsoVcIdDAO;

    @Autowired
    private NsoQosSapPolicyIdDAO nsoQosSapPolicyIdDAO;

    @Autowired
    private NsoSdpIdDAO nsoSdpIdDAO;

    @Autowired
    private MiscHelper miscHelper;

    public final static String ROUTING_DOMAIN = "esnet-293"; // FIXME: this needs to come from topology, probably
    public SouthboundTaskResult processTask(Connection conn, CommandType commandType, State intent) throws NsoException, PSSException {
        log.info("processing southbound NSO task "+conn.getConnectionId()+" "+commandType.toString());
        DeploymentState depState = DeploymentState.DEPLOYED;
        if (commandType.equals(CommandType.DISMANTLE)) {
            depState = DeploymentState.UNDEPLOYED;
        } else if (commandType.equals(CommandType.BUILD)) {
            depState = DeploymentState.DEPLOYED;
        }


        try {
            if (commandType.equals(CommandType.BUILD)) {
                NsoOscarsServices services = this.nsoOscarsServices(conn);
                // LSPs before VPLSs
                if (services.getLspWrapper() != null) {
                    nsoProxy.postToServices(services.getLspWrapper());
                }
                nsoProxy.postToServices(services.getVplsWrapper());

                return SouthboundTaskResult.builder()
                        .connectionId(conn.getConnectionId())
                        .deploymentState(depState)
                        .state(intent)
                        .commandType(commandType)
                        .build();
            } else if (commandType.equals(CommandType.DISMANTLE)) {
                NsoOscarsDismantle dismantle = this.nsoOscarsDismantle(conn);
                nsoProxy.deleteServices(conn.getConnectionId(), dismantle);
                return SouthboundTaskResult.builder()
                        .connectionId(conn.getConnectionId())
                        .deploymentState(depState)
                        .state(intent)
                        .commandType(commandType)
                        .build();
            } else {
                return SouthboundTaskResult.builder()
                        .connectionId(conn.getConnectionId())
                        .deploymentState(conn.getDeploymentState())
                        .state(State.FAILED)
                        .commandType(commandType)
                        .build();
            }
        } catch (NsoException e) {
            return SouthboundTaskResult.builder()
                    .connectionId(conn.getConnectionId())
                    .deploymentState(conn.getDeploymentState())
                    .state(State.FAILED)
                    .commandType(commandType)
                    .build();

        }
    }
    public NsoLSP makeNsoLSP(String connectionId, VlanJunction thisJunction, VlanJunction otherJunction, List<EroHop> hops, boolean isProtect) throws PSSException {

        String lspNamePiece = "-WRK-";
        int holdSetupPriority = 5;
        NsoLspPathType pathType = NsoLspPathType.STRICT;
        if (isProtect) {
            lspNamePiece = "-PRT-";
            holdSetupPriority = 4;
            pathType = NsoLspPathType.LOOSE;
        }


        NsoLSP.MplsPath mplsPath = NsoLSP.MplsPath.builder()
                .pathType(pathType)
                .holdPriority(holdSetupPriority)
                .setupPriority(holdSetupPriority)
                .build();
        if (!isProtect) {
            List<NsoLSP.Hop> nsoHops = new ArrayList<>();
            int i = 1;
            List<MplsHop> mplsHops = miscHelper.mplsHops(hops);

            for (MplsHop hop : mplsHops) {
                nsoHops.add(NsoLSP.Hop.builder()
                        .ipv4(hop.getAddress())
                        .number(i)
                        .build());
                i++;
            }
            mplsPath.setHop(nsoHops);
        }

        return NsoLSP.builder()
                .name(connectionId + lspNamePiece + otherJunction.getDeviceUrn())
                .device(thisJunction.getDeviceUrn())
                .primary(mplsPath)
                .secondary(null)
                .metric(100000)
                .target(NsoLSP.Target.builder()
                        .device(otherJunction.getDeviceUrn())
                        .build())
                // .metric() we don't need it
                .routingDomain(ROUTING_DOMAIN)
                .build();
    }

    public NsoOscarsDismantle nsoOscarsDismantle(Connection conn) {
        Integer vcId = nsoVcIdDAO.findNsoVcIdByConnectionId(conn.getConnectionId()).orElseThrow().getVcId();
        Components cmp = null;
        if (conn.getReserved() != null) {
            cmp = conn.getReserved().getCmp();
        } else {
            cmp = conn.getArchived().getCmp();
        }
        List<String> lspInstanceKeys = new ArrayList<>();
        for (VlanPipe pipe : cmp.getPipes()) {
            String lspNamePiece = "-WRK-";
            // LSP instance key is "lspname,device"
            // we do it both for A-Z and Z-A
            String azInstanceKey = conn.getConnectionId() + lspNamePiece + pipe.getZ().getDeviceUrn()+","+pipe.getA().getDeviceUrn();
            String zaInstanceKey = conn.getConnectionId() + lspNamePiece + pipe.getA().getDeviceUrn()+","+pipe.getZ().getDeviceUrn();
            lspInstanceKeys.add(azInstanceKey);
            lspInstanceKeys.add(zaInstanceKey);

            if (pipe.getProtect()) {
                lspNamePiece = "-PRT-";
                azInstanceKey = conn.getConnectionId() + lspNamePiece + pipe.getZ().getDeviceUrn()+","+pipe.getA().getDeviceUrn();;
                zaInstanceKey = conn.getConnectionId() + lspNamePiece + pipe.getA().getDeviceUrn()+","+pipe.getZ().getDeviceUrn();;
                lspInstanceKeys.add(azInstanceKey);
                lspInstanceKeys.add(zaInstanceKey);
            }
        }


        return NsoOscarsDismantle.builder()
                .vcId(vcId)
                .lspNsoKeys(lspInstanceKeys)
                .build();
    }

    public NsoOscarsServices nsoOscarsServices(Connection conn) throws PSSException, NsoException {

        Map<LspMapKey, String> lspNames = new HashMap<>();
        NsoLspWrapper lspWrapper = null;
        if (conn.getReserved().getCmp().getJunctions().size() > 1) {
            lspWrapper = new NsoLspWrapper();
            for (VlanPipe pipe : conn.getReserved().getCmp().getPipes()) {
                // primary path
                NsoLSP azLsp = makeNsoLSP(conn.getConnectionId(), pipe.getA(), pipe.getZ(), pipe.getAzERO(), false);
                LspMapKey azKey = LspMapKey.builder()
                        .device(pipe.getA().getDeviceUrn())
                        .target(pipe.getZ().getDeviceUrn())
                        .protect(false)
                        .build();
                lspNames.put(azKey, azLsp.getName());
                lspWrapper.getLspInstances().add(azLsp);
                NsoLSP zaLsp = makeNsoLSP(conn.getConnectionId(), pipe.getZ(), pipe.getA(), pipe.getZaERO(), false);
                LspMapKey zaKey = LspMapKey.builder()
                        .device(pipe.getZ().getDeviceUrn())
                        .target(pipe.getA().getDeviceUrn())
                        .protect(false)
                        .build();
                lspNames.put(zaKey, zaLsp.getName());

                lspWrapper.getLspInstances().add(zaLsp);

                if (pipe.getProtect()) {
                    NsoLSP azProtectLsp = makeNsoLSP(conn.getConnectionId(), pipe.getA(), pipe.getZ(), pipe.getAzERO(), true);
                    lspWrapper.getLspInstances().add(azProtectLsp);
                    LspMapKey azProtectKey = LspMapKey.builder()
                            .device(pipe.getA().getDeviceUrn())
                            .target(pipe.getZ().getDeviceUrn())
                            .protect(true)
                            .build();
                    lspNames.put(azProtectKey, azProtectLsp.getName());

                    NsoLSP zaProtectLsp = makeNsoLSP(conn.getConnectionId(), pipe.getZ(), pipe.getA(), pipe.getZaERO(), true);
                    lspWrapper.getLspInstances().add(zaProtectLsp);
                    LspMapKey zaProtectKey = LspMapKey.builder()
                            .device(pipe.getZ().getDeviceUrn())
                            .target(pipe.getA().getDeviceUrn())
                            .protect(true)
                            .build();
                    lspNames.put(zaProtectKey, zaProtectLsp.getName());
                }
            }
        }


        Map<String, NsoVPLS.DeviceContainer> vplsDeviceMap = new HashMap<>();
        Integer vcid = nsoVcIdDAO.findNsoVcIdByConnectionId(conn.getConnectionId()).orElseThrow().getVcId();
        for (VlanFixture f : conn.getReserved().getCmp().getFixtures()) {
            String deviceUrn = f.getJunction().getDeviceUrn();

            // FIXME: this needs to be populated correctly as a separate property instead of relying on string split
            String portUrn = f.getPortUrn();
            String[] parts = portUrn.split(":");
            if (parts.length != 2) {
                throw new NsoException("Invalid port URN format");
            }
            String portIfce = parts[1];

            if (!vplsDeviceMap.containsKey(deviceUrn)) {
                NsoVPLS.DeviceContainer dc = NsoVPLS.DeviceContainer.builder()
                        .device(deviceUrn)
                        .endpoint(new ArrayList<>())
                        .virtualIfces(new ArrayList<>())
                        .build();
                vplsDeviceMap.put(deviceUrn, dc);
            }
            NsoQosSapPolicyId sapQosId = nsoQosSapPolicyIdDAO.findNsoQosSapPolicyIdByFixtureId(f.getId()).orElseThrow();
            NsoVPLS.DeviceContainer dc = vplsDeviceMap.get(deviceUrn);

            // NSO yang sets this to min 1
            int ingBw = f.getIngressBandwidth();
            int egBw = f.getEgressBandwidth();
            if (ingBw == 0) {
                ingBw = 1;
            }
            if (egBw == 0) {
                egBw = 1;
            }
            NsoVPLS.QoS qos = NsoVPLS.QoS.builder()
                    .qosId(sapQosId.getPolicyId())
                    .excessAction(NsoVplsQosExcessAction.KEEP)
                    .ingressMbps(ingBw)
                    .egressMbps(egBw)
                    .build();

            NsoVPLS.Endpoint endpoint = NsoVPLS.Endpoint.builder()
                    .ifce(portIfce)
                    .vlanId(f.getVlan().getVlanId())
                    .layer2Description(conn.getConnectionId())
                    .qos(qos)
                    .build();
            dc.getEndpoint().add(endpoint);
        }
        List<NsoVPLS.SDP> sdps = new ArrayList<>();
        List<NsoSdpId> sdpIds = nsoSdpIdDAO.findNsoSdpIdByConnectionId(conn.getConnectionId());
        for (VlanPipe pipe : conn.getReserved().getCmp().getPipes()) {
            NsoSdpId primarySdpId = null;
            NsoSdpId protectSdpId = null;

            for (NsoSdpId sdpId : sdpIds) {
                if (sdpId.getDevice().equals(pipe.getA().getDeviceUrn()) && sdpId.getTarget().equals(pipe.getZ().getDeviceUrn())) {
                    if (sdpId.getPrecedence().equals(NsoVplsSdpPrecedence.PRIMARY.toString())) {
                        primarySdpId = sdpId;
                    } else if (sdpId.getPrecedence().equals(NsoVplsSdpPrecedence.SECONDARY.toString())) {
                        protectSdpId = sdpId;
                    }
                }
            }
            if (primarySdpId == null) {
                throw new NsoException("could not locate primary SDP id");
            }
            LspMapKey azKey = LspMapKey.builder()
                    .device(pipe.getA().getDeviceUrn())
                    .target(pipe.getZ().getDeviceUrn())
                    .protect(false)
                    .build();
            LspMapKey zaKey = LspMapKey.builder()
                    .device(pipe.getZ().getDeviceUrn())
                    .target(pipe.getA().getDeviceUrn())
                    .protect(false)
                    .build();
            String azLspName = lspNames.get(azKey);
            String zaLspName = lspNames.get(zaKey);

            NsoVPLS.SDPMember a = NsoVPLS.SDPMember.builder()
                    .device(pipe.getA().getDeviceUrn())
                    .lsp(azLspName)
                    .mode(NsoVplsSdpMode.SPOKE)
                    .build();
            NsoVPLS.SDPMember z = NsoVPLS.SDPMember.builder()
                    .device(pipe.getZ().getDeviceUrn())
                    .lsp(zaLspName)
                    .mode(NsoVplsSdpMode.SPOKE)
                    .build();
            NsoVPLS.SDP sdp = NsoVPLS.SDP.builder()
                    .description(conn.getConnectionId())
                    .precedence(NsoVplsSdpPrecedence.PRIMARY)
                    .a(a)
                    .z(z)
                    .sdpId(primarySdpId.getSdpId())
                    .build();
            sdps.add(sdp);

            if (pipe.getProtect()) {
                if (protectSdpId == null) {
                    throw new NsoException("could not locate protect SDP id");
                }
                LspMapKey azProtectKey = LspMapKey.builder()
                        .device(pipe.getA().getDeviceUrn())
                        .target(pipe.getZ().getDeviceUrn())
                        .protect(true)
                        .build();
                LspMapKey zaProtectKey = LspMapKey.builder()
                        .device(pipe.getZ().getDeviceUrn())
                        .target(pipe.getA().getDeviceUrn())
                        .protect(true)
                        .build();
                String azProtectLspName = lspNames.get(azProtectKey);
                String zaProtectLspName = lspNames.get(zaProtectKey);

                NsoVPLS.SDPMember protectA = NsoVPLS.SDPMember.builder()
                        .device(pipe.getA().getDeviceUrn())
                        .lsp(azProtectLspName)
                        .mode(NsoVplsSdpMode.SPOKE)
                        .build();
                NsoVPLS.SDPMember protectZ = NsoVPLS.SDPMember.builder()
                        .device(pipe.getZ().getDeviceUrn())
                        .lsp(zaProtectLspName)
                        .mode(NsoVplsSdpMode.SPOKE)
                        .build();

                NsoVPLS.SDP protectSdp = NsoVPLS.SDP.builder()
                        .description(conn.getConnectionId())
                        .precedence(NsoVplsSdpPrecedence.SECONDARY)
                        .a(protectA)
                        .z(protectZ)
                        .sdpId(protectSdpId.getSdpId())
                        .build();
                sdps.add(protectSdp);

            }

        }

        NsoVPLS vpls = NsoVPLS.builder()
                .description(conn.getDescription())
                .name(conn.getConnectionId())
                .qosMode(NsoVplsQosMode.GUARANTEED)
                .routingDomain(ROUTING_DOMAIN)
                .vcId(vcid)
                .sdp(sdps)
                .device(vplsDeviceMap.values().stream().toList())
                .build();

        NsoVplsWrapper vplsWrapper = new NsoVplsWrapper();
        vplsWrapper.setVplsInstances(new ArrayList<>());
        vplsWrapper.getVplsInstances().add(vpls);

        return NsoOscarsServices.builder()
                .lspWrapper(lspWrapper)
                .vplsWrapper(vplsWrapper)
                .build();

    }

    @Data
    @Builder
    public static class NsoOscarsServices {
        private NsoLspWrapper lspWrapper;
        private NsoVplsWrapper vplsWrapper;
    }

    @Data
    @Builder
    public static class NsoOscarsDismantle {
        private int vcId;
        private List<String> lspNsoKeys;
    }

    @Data
    @Builder
    public static class LspMapKey {
        String device;
        String target;
        boolean protect;
    }
}
