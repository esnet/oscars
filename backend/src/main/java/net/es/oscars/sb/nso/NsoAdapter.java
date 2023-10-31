package net.es.oscars.sb.nso;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.sb.nso.db.NsoSdpVcIdDAO;
import net.es.oscars.sb.nso.ent.NsoSdpVcId;
import net.es.oscars.sb.nso.exc.NsoCommitException;
import net.es.oscars.app.exc.PSSException;
import net.es.oscars.dto.pss.cmd.CommandType;
import net.es.oscars.dto.pss.st.ConfigStatus;
import net.es.oscars.sb.nso.exc.NsoDryrunException;
import net.es.oscars.sb.nso.exc.NsoGenException;
import net.es.oscars.sb.nso.db.NsoQosSapPolicyIdDAO;
import net.es.oscars.sb.nso.db.NsoSdpIdDAO;
import net.es.oscars.sb.nso.db.NsoVcIdDAO;
import net.es.oscars.sb.nso.ent.NsoQosSapPolicyId;
import net.es.oscars.sb.nso.ent.NsoSdpId;
import net.es.oscars.sb.db.RouterCommandsRepository;
import net.es.oscars.sb.ent.RouterCommandHistory;
import net.es.oscars.sb.ent.RouterCommands;
import net.es.oscars.sb.beans.MplsHop;
import net.es.oscars.sb.MiscHelper;
import net.es.oscars.resv.db.CommandHistoryRepository;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.DeploymentState;
import net.es.oscars.resv.enums.State;
import net.es.oscars.sb.SouthboundTaskResult;
import net.es.oscars.sb.nso.rest.NsoServicesWrapper;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.NsoVPLS;
import net.es.topo.common.dto.nso.enums.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

import static net.es.topo.common.devel.DevelUtils.dumpDebug;

@Component
@Slf4j
public class NsoAdapter {
    public static String NSO_TEMPLATE_VERSION = "NSO 1.1";

    @Autowired
    private NsoProxy nsoProxy;

    @Autowired
    private NsoVcIdDAO nsoVcIdDAO;

    @Autowired
    private NsoQosSapPolicyIdDAO nsoQosSapPolicyIdDAO;

    @Autowired
    private NsoSdpIdDAO nsoSdpIdDAO;

    @Autowired
    private NsoSdpVcIdDAO nsoSdpVcIdDAO;

    @Autowired
    private CommandHistoryRepository historyRepo;

    @Autowired
    private RouterCommandsRepository rcr;

    @Autowired
    private MiscHelper miscHelper;

    public final static String ROUTING_DOMAIN = "esnet-293"; // FIXME: this needs to come from topology, probably
    public SouthboundTaskResult processTask(Connection conn, CommandType commandType, State intent)  {
        log.info("processing southbound NSO task "+conn.getConnectionId()+" "+commandType.toString());

        // we ass-u-me that incoming deployment state is the opposite of what is asked
        DeploymentState failureDepState = DeploymentState.DEPLOY_FAILED;
        if (commandType.equals(CommandType.DISMANTLE)) {
            failureDepState = DeploymentState.UNDEPLOY_FAILED;
        }

        DeploymentState newDepState;

        State newState = intent;
        String commands = "";
        String dryRun = "";
        ConfigStatus configStatus = ConfigStatus.NONE;

        boolean shouldWriteHistory = false;

        if (commandType.equals(CommandType.BUILD) || commandType.equals(CommandType.DISMANTLE)) {
            try {
                if (commandType.equals(CommandType.BUILD)) {
                    NsoServicesWrapper oscarsServices = this.nsoOscarsServices(conn);
                    commands = oscarsServices.asCliCommands();
                    log.info(commands);
                    dumpDebug(conn.getConnectionId()+" BUILD services", oscarsServices);
                    dryRun = nsoProxy.buildDryRun(oscarsServices);
                    nsoProxy.buildServices(oscarsServices);
                    newDepState = DeploymentState.DEPLOYED;
                } else {
                    NsoOscarsDismantle dismantle = this.nsoOscarsDismantle(conn);
                    commands = dismantle.asCliCommands();
                    log.info(commands);
                    dryRun = nsoProxy.dismantleDryRun(dismantle);
                    nsoProxy.deleteServices(dismantle);
                    newDepState = DeploymentState.UNDEPLOYED;
                }
                // only set this after all has gone well
                shouldWriteHistory = true;
            } catch (NsoDryrunException ex) {
                commands = ex.getMessage();
                newDepState = failureDepState;
                newState = State.FAILED;
            } catch (NsoCommitException | NsoGenException ex) {
                configStatus = ConfigStatus.ERROR;
                newDepState = failureDepState;
                newState = State.FAILED;
            }
        } else {
            newDepState = failureDepState;
            newState = State.FAILED;
        }

        if (shouldWriteHistory) {
        // save the NSO service config and dry-run
            Components cmp;
            if (conn.getReserved() != null) {
                cmp = conn.getReserved().getCmp();
            } else {
                cmp = conn.getArchived().getCmp();
            }
            for (VlanJunction j : cmp.getJunctions()) {
                RouterCommands rcb = RouterCommands.builder()
                        .connectionId(conn.getConnectionId())
                        .deviceUrn(j.getDeviceUrn())
                        .contents(commands)
                        .templateVersion(NSO_TEMPLATE_VERSION)
                        .type(commandType)
                        .build();
                rcr.save(rcb);
                RouterCommandHistory rch = RouterCommandHistory.builder()
                        .deviceUrn(j.getDeviceUrn())
                        .templateVersion(NSO_TEMPLATE_VERSION)
                        .connectionId(conn.getConnectionId())
                        .date(Instant.now())
                        .commands(commands)
                        .output(dryRun)
                        .configStatus(configStatus)
                        .type(commandType)
                        .build();
                historyRepo.save(rch);
            }
        }

        return SouthboundTaskResult.builder()
                .connectionId(conn.getConnectionId())
                .deploymentState(newDepState)
                .state(newState)
                .commandType(commandType)
                .build();
    }
    public NsoLSP makeNsoLSP(String connectionId, VlanJunction thisJunction, VlanJunction otherJunction, List<EroHop> hops, boolean isProtect) throws NsoGenException {

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
            List<MplsHop> mplsHops = null;
            try {
                mplsHops = miscHelper.mplsHops(hops);
            } catch (PSSException e) {
                throw new NsoGenException(e.getMessage());
            }

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
                .connectionId(conn.getConnectionId())
                .vcId(vcId)
                .lspNsoKeys(lspInstanceKeys)
                .build();
    }

    public NsoServicesWrapper nsoOscarsServices(Connection conn) throws NsoGenException {

        Map<LspMapKey, String> lspNames = new HashMap<>();
        List<NsoLSP> lspInstances = new ArrayList<>();

        if (conn.getReserved().getCmp().getJunctions().size() > 1) {
            for (VlanPipe pipe : conn.getReserved().getCmp().getPipes()) {
                // primary path
                NsoLSP azLsp = makeNsoLSP(conn.getConnectionId(), pipe.getA(), pipe.getZ(), pipe.getAzERO(), false);
                LspMapKey azKey = LspMapKey.builder()
                        .device(pipe.getA().getDeviceUrn())
                        .target(pipe.getZ().getDeviceUrn())
                        .protect(false)
                        .build();
                lspNames.put(azKey, azLsp.getName());
                lspInstances.add(azLsp);
                NsoLSP zaLsp = makeNsoLSP(conn.getConnectionId(), pipe.getZ(), pipe.getA(), pipe.getZaERO(), false);
                LspMapKey zaKey = LspMapKey.builder()
                        .device(pipe.getZ().getDeviceUrn())
                        .target(pipe.getA().getDeviceUrn())
                        .protect(false)
                        .build();
                lspNames.put(zaKey, zaLsp.getName());

                lspInstances.add(zaLsp);

                if (pipe.getProtect()) {
                    NsoLSP azProtectLsp = makeNsoLSP(conn.getConnectionId(), pipe.getA(), pipe.getZ(), pipe.getAzERO(), true);
                    lspInstances.add(azProtectLsp);
                    LspMapKey azProtectKey = LspMapKey.builder()
                            .device(pipe.getA().getDeviceUrn())
                            .target(pipe.getZ().getDeviceUrn())
                            .protect(true)
                            .build();
                    lspNames.put(azProtectKey, azProtectLsp.getName());

                    NsoLSP zaProtectLsp = makeNsoLSP(conn.getConnectionId(), pipe.getZ(), pipe.getA(), pipe.getZaERO(), true);
                    lspInstances.add(zaProtectLsp);
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
            log.info("working on fixture "+f.getPortUrn()+" id "+f.getId());

            // FIXME: this needs to be populated correctly as a separate property instead of relying on string split
            String portUrn = f.getPortUrn();
            String[] parts = portUrn.split(":");
            if (parts.length != 2) {
                throw new NsoGenException("Invalid port URN format");
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
        List<NsoSdpVcId> sdpVcIds = nsoSdpVcIdDAO.findNsoSdpVcIdByConnectionId(conn.getConnectionId());
        for (VlanPipe pipe : conn.getReserved().getCmp().getPipes()) {
            NsoSdpId primarySdpId = null;
            NsoSdpId protectSdpId = null;
            NsoSdpVcId primarySdpVcId = null;
            NsoSdpVcId protectSdpVcId = null;

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
                throw new NsoGenException("could not locate primary SDP id");
            }
            for (NsoSdpVcId sdpVcId: sdpVcIds) {
                if (sdpVcId.getDevice().equals(primarySdpId.getDevice()) &&
                        sdpVcId.getSdpId().equals(primarySdpId.getSdpId())) {
                    primarySdpVcId = sdpVcId;
                }
            }
            if (primarySdpVcId == null) {
                throw new NsoGenException("could not locate primary SDP VC id");
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
                    .vcId(primarySdpVcId.getVcId())
                    .mode(NsoVplsSdpMode.SPOKE)
                    .build();
            NsoVPLS.SDPMember z = NsoVPLS.SDPMember.builder()
                    .device(pipe.getZ().getDeviceUrn())
                    .lsp(zaLspName)
                    .vcId(primarySdpVcId.getVcId())
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
                    throw new NsoGenException("could not locate protect SDP id");
                }
                for (NsoSdpVcId sdpVcId: sdpVcIds) {
                    if (sdpVcId.getDevice().equals(protectSdpId.getDevice()) &&
                            sdpVcId.getSdpId().equals(protectSdpId.getSdpId())) {
                        protectSdpVcId = sdpVcId;
                    }
                }
                if (protectSdpVcId == null) {
                    throw new NsoGenException("could not locate protect SDP VC id");
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
                        .vcId(protectSdpVcId.getVcId())
                        .mode(NsoVplsSdpMode.SPOKE)
                        .build();
                NsoVPLS.SDPMember protectZ = NsoVPLS.SDPMember.builder()
                        .device(pipe.getZ().getDeviceUrn())
                        .lsp(zaProtectLspName)
                        .mode(NsoVplsSdpMode.SPOKE)
                        .vcId(protectSdpVcId.getVcId())
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
                .description(conn.getDescription().substring(0, 79))
                .name(conn.getConnectionId())
                .qosMode(NsoVplsQosMode.GUARANTEED)
                .routingDomain(ROUTING_DOMAIN)
                .vcId(vcid)
                .sdp(sdps)
                .device(vplsDeviceMap.values().stream().toList())
                .build();

        List<NsoVPLS> vplsInstances = new ArrayList<>();
        vplsInstances.add(vpls);
        return NsoServicesWrapper.builder()
                .lspInstances(lspInstances)
                .vplsInstances(vplsInstances)
                .build();
    }


    @Data
    @Builder
    public static class NsoOscarsDismantle {
        private String connectionId;
        private int vcId;
        private List<String> lspNsoKeys;
        public String asCliCommands() {
            StringBuilder cmds = new StringBuilder();
            cmds.append("delete services vpls %d%n".formatted(vcId));
            for (String lspNsoKey : lspNsoKeys) {
                cmds.append("delete services lsp %s%n".formatted(lspNsoKey));
            }
            return cmds.toString();
        }
    }

    @Data
    @Builder
    public static class LspMapKey {
        String device;
        String target;
        boolean protect;
    }
}
