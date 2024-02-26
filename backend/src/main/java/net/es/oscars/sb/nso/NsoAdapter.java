package net.es.oscars.sb.nso;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.NsoProperties;
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
import net.es.topo.common.devel.DevelUtils;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.NsoVPLS;
import net.es.topo.common.dto.nso.enums.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

import static net.es.oscars.resv.svc.ResvLibrary.validateServiceId;
import static net.es.topo.common.devel.DevelUtils.dumpDebug;

@Component
@Slf4j
public class NsoAdapter {
    public static String NSO_TEMPLATE_VERSION = "NSO 1.1";
    public static String WORK_LSP_NAME_PIECE = "WRK";
    public static String PROTECT_LSP_NAME_PIECE = "PRT";
    public static String LSP_NAME_DELIMITER = "-";

    private final NsoProperties nsoProperties;

    private final NsoProxy nsoProxy;

    private final NsoVcIdDAO nsoVcIdDAO;

    private final NsoQosSapPolicyIdDAO nsoQosSapPolicyIdDAO;

    private final NsoSdpIdDAO nsoSdpIdDAO;

    private final NsoSdpVcIdDAO nsoSdpVcIdDAO;

    private final CommandHistoryRepository historyRepo;

    private final RouterCommandsRepository rcr;

    private final MiscHelper miscHelper;

    public NsoAdapter(NsoProperties nsoProperties, NsoProxy nsoProxy, MiscHelper miscHelper,
                      NsoVcIdDAO nsoVcIdDAO, NsoQosSapPolicyIdDAO nsoQosSapPolicyIdDAO, NsoSdpIdDAO nsoSdpIdDAO,
                      NsoSdpVcIdDAO nsoSdpVcIdDAO, CommandHistoryRepository historyRepo, RouterCommandsRepository rcr) {
        this.nsoProperties = nsoProperties;
        this.nsoProxy = nsoProxy;
        this.nsoVcIdDAO = nsoVcIdDAO;
        this.nsoQosSapPolicyIdDAO = nsoQosSapPolicyIdDAO;
        this.nsoSdpIdDAO = nsoSdpIdDAO;
        this.nsoSdpVcIdDAO = nsoSdpVcIdDAO;
        this.historyRepo = historyRepo;
        this.rcr = rcr;
        this.miscHelper = miscHelper;
    }


    public SouthboundTaskResult processTask(Connection conn, CommandType commandType, State intent)  {
        log.info("processing southbound NSO task "+conn.getConnectionId()+" "+commandType+ " " +intent);

        // we ass-u-me that incoming deployment state is the opposite of what is asked
        DeploymentState failureDepState = DeploymentState.DEPLOY_FAILED;
        if (commandType.equals(CommandType.DISMANTLE)) {
            failureDepState = DeploymentState.UNDEPLOY_FAILED;
        } else if (commandType.equals(CommandType.REDEPLOY)) {
            failureDepState = DeploymentState.REDEPLOY_FAILED;
        }

        DeploymentState newDepState;

        State newState = intent;
        String commands = "";
        String dryRun = "";
        ConfigStatus configStatus = ConfigStatus.NONE;

        boolean shouldWriteHistory = false;

        if (commandType.equals(CommandType.BUILD) || commandType.equals(CommandType.DISMANTLE) || commandType.equals(CommandType.REDEPLOY)) {
            try {
                switch (commandType) {
                    case BUILD -> {
                        NsoServicesWrapper oscarsServices = this.nsoOscarsServices(conn);
                        log.info("got services");
                        commands = oscarsServices.asCliCommands();
                        log.info("\n"+commands);
                        dumpDebug(conn.getConnectionId()+" BUILD services", oscarsServices);
                        dryRun = nsoProxy.buildDryRun(oscarsServices);
                        nsoProxy.buildServices(oscarsServices);
                        newDepState = DeploymentState.DEPLOYED;

                    }
                    case DISMANTLE ->  {
                        NsoOscarsDismantle dismantle = this.nsoOscarsDismantle(conn);
                        commands = dismantle.asCliCommands();
                        log.info("\n"+commands);
                        dryRun = nsoProxy.dismantleDryRun(dismantle);
                        nsoProxy.deleteServices(dismantle);
                        newDepState = DeploymentState.UNDEPLOYED;

                    }
                    case REDEPLOY ->  {
                        NsoVPLS oscarsServices = this.nsoOscarsServicesRedeploy(conn);
                        dumpDebug(conn.getConnectionId()+" REDEPLOY services", oscarsServices);
                        nsoProxy.redeployServices(oscarsServices, conn.getConnectionId());
                        newDepState = DeploymentState.DEPLOYED;
                    }
                    default -> {
                        newDepState = conn.getDeploymentState();
                    }
                }
                // only set this after all has gone well
                shouldWriteHistory = true;
            } catch (NsoDryrunException ex) {
                log.error("dry run error"+ex.getMessage());
                commands = ex.getMessage();
                newDepState = failureDepState;
                newState = State.FAILED;
            } catch (NsoCommitException | NsoGenException ex) {
                log.error("commit or gen error"+ex.getMessage());
                configStatus = ConfigStatus.ERROR;
                newDepState = failureDepState;
                newState = State.FAILED;
            }
        } else {
            newDepState = failureDepState;
            newState = State.FAILED;
        }

        if (shouldWriteHistory && !commandType.equals(CommandType.REDEPLOY)) {
        // save the NSO service config and dry-run; we don't save redeploys
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
    public NsoLSP makeNsoLSP(Connection conn, VlanJunction thisJunction, VlanJunction otherJunction, List<EroHop> hops, boolean isProtect) throws NsoGenException {

        int holdSetupPriority = 5;
        NsoLspPathType pathType = NsoLspPathType.STRICT;
        if (isProtect) {
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
            List<MplsHop> mplsHops;
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
        String lspName = lspName(conn, isProtect, otherJunction.getDeviceUrn());

        return NsoLSP.builder()
                .name(lspName)
                .device(thisJunction.getDeviceUrn())
                .primary(mplsPath)
                .secondary(null)
                .metric(100000)
                .target(NsoLSP.Target.builder()
                        .device(otherJunction.getDeviceUrn())
                        .build())
                // .metric() we don't need it
                .routingDomain(nsoProperties.getRoutingDomain())
                .build();
    }

    public static String lspName(Connection c, boolean protect, String target) {

        List<String> parts = new ArrayList<>();

        if (c.getServiceId() != null && !c.getServiceId().isEmpty()) {
            if (validateServiceId(c.getServiceId())) {
                parts.add(c.getServiceId());
            } else {
                log.info("serviceId "+c.getServiceId()+" did not pass validation");
            }
        }

        parts.add(c.getConnectionId());
        if (protect) {
            parts.add(PROTECT_LSP_NAME_PIECE);
        } else {
            parts.add(WORK_LSP_NAME_PIECE);
        }
        parts.add(target);

        return String.join(LSP_NAME_DELIMITER, parts);
    }



    public NsoOscarsDismantle nsoOscarsDismantle(Connection conn) {
        Integer vcId = nsoVcIdDAO.findNsoVcIdByConnectionId(conn.getConnectionId()).orElseThrow().getVcId();
        Components cmp;
        if (conn.getReserved() != null) {
            cmp = conn.getReserved().getCmp();
        } else {
            cmp = conn.getArchived().getCmp();
        }
        List<String> lspInstanceKeys = new ArrayList<>();
        for (VlanPipe pipe : cmp.getPipes()) {
            // LSP instance key is "lspname,device"
            // we do it both for A-Z and Z-A
            String azLspName = lspName(conn, false, pipe.getZ().getDeviceUrn());
            String zaLspName = lspName(conn, false, pipe.getA().getDeviceUrn());

            String azInstanceKey = azLspName+","+pipe.getA().getDeviceUrn();
            String zaInstanceKey = zaLspName+","+pipe.getZ().getDeviceUrn();
            lspInstanceKeys.add(azInstanceKey);
            lspInstanceKeys.add(zaInstanceKey);

            if (pipe.getProtect()) {
                azLspName = lspName(conn, true, pipe.getZ().getDeviceUrn());
                zaLspName = lspName(conn, true, pipe.getA().getDeviceUrn());

                azInstanceKey = azLspName+","+pipe.getA().getDeviceUrn();
                zaInstanceKey = zaLspName+","+pipe.getZ().getDeviceUrn();

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
        log.info("NSO services wrapper");
        Map<LspMapKey, String> lspNames = new HashMap<>();
        List<NsoLSP> lspInstances = new ArrayList<>();

        if (conn.getReserved().getCmp().getJunctions().size() > 1) {
            for (VlanPipe pipe : conn.getReserved().getCmp().getPipes()) {
                // primary path
                NsoLSP azLsp = makeNsoLSP(conn, pipe.getA(), pipe.getZ(), pipe.getAzERO(), false);
                LspMapKey azKey = LspMapKey.builder()
                        .device(pipe.getA().getDeviceUrn())
                        .target(pipe.getZ().getDeviceUrn())
                        .protect(false)
                        .build();
                lspNames.put(azKey, azLsp.getName());
                lspInstances.add(azLsp);
                NsoLSP zaLsp = makeNsoLSP(conn, pipe.getZ(), pipe.getA(), pipe.getZaERO(), false);
                LspMapKey zaKey = LspMapKey.builder()
                        .device(pipe.getZ().getDeviceUrn())
                        .target(pipe.getA().getDeviceUrn())
                        .protect(false)
                        .build();
                lspNames.put(zaKey, zaLsp.getName());

                lspInstances.add(zaLsp);

                if (pipe.getProtect()) {
                    NsoLSP azProtectLsp = makeNsoLSP(conn, pipe.getA(), pipe.getZ(), pipe.getAzERO(), true);
                    lspInstances.add(azProtectLsp);
                    LspMapKey azProtectKey = LspMapKey.builder()
                            .device(pipe.getA().getDeviceUrn())
                            .target(pipe.getZ().getDeviceUrn())
                            .protect(true)
                            .build();
                    lspNames.put(azProtectKey, azProtectLsp.getName());

                    NsoLSP zaProtectLsp = makeNsoLSP(conn, pipe.getZ(), pipe.getA(), pipe.getZaERO(), true);
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
            Optional<NsoQosSapPolicyId> maybeSapQosId = nsoQosSapPolicyIdDAO.findNsoQosSapPolicyIdByFixtureId(f.getId());
            if (maybeSapQosId.isEmpty()) {
                throw new NsoGenException("unable to retrieve NSO SAP QoS id");
            }
            NsoQosSapPolicyId sapQosId = maybeSapQosId.get();

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

            Boolean cflowd = null;
            switch (nsoProperties.getCflowd()) {
                case ENABLED -> cflowd = true;
                case DISABLED -> cflowd = false;
            }

            NsoVPLS.Endpoint endpoint = NsoVPLS.Endpoint.builder()
                    .ifce(portIfce)
                    .vlanId(f.getVlan().getVlanId())
                    .layer2Description(conn.getConnectionId())
                    .cflowd(cflowd)
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

        String nsoDescription = conn.getDescription();
        if (nsoDescription.length() > 80) {
            nsoDescription = conn.getDescription().substring(0, 79);
        }

        NsoVPLS vpls = NsoVPLS.builder()
                .description(nsoDescription)
                .name(conn.getConnectionId())
                .qosMode(NsoVplsQosMode.GUARANTEED)
                .routingDomain(nsoProperties.getRoutingDomain())
                .vcId(vcid)
                .sdp(sdps)
                .device(vplsDeviceMap.values().stream().toList())
                .build();

        List<NsoVPLS> vplsInstances = new ArrayList<>();
        vplsInstances.add(vpls);
        DevelUtils.dumpDebug("vpls", vpls);
        return NsoServicesWrapper.builder()
                .lspInstances(lspInstances)
                .vplsInstances(vplsInstances)
                .build();
    }


    // TODO: factor this out?
    // this should just redeploy the vpls service part
    public NsoVPLS nsoOscarsServicesRedeploy(Connection conn) throws NsoGenException {
        log.info("NSO services wrapper - redeploy");

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
            Optional<NsoQosSapPolicyId> maybeSapQosId = nsoQosSapPolicyIdDAO.findNsoQosSapPolicyIdByFixtureId(f.getId());
            if (maybeSapQosId.isEmpty()) {
                throw new NsoGenException("unable to retrieve NSO SAP QoS id");
            }
            NsoQosSapPolicyId sapQosId = maybeSapQosId.get();

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

            Boolean cflowd = null;
            switch (nsoProperties.getCflowd()) {
                case ENABLED -> cflowd = true;
                case DISABLED -> cflowd = false;
            }

            NsoVPLS.Endpoint endpoint = NsoVPLS.Endpoint.builder()
                    .ifce(portIfce)
                    .vlanId(f.getVlan().getVlanId())
                    .layer2Description(conn.getConnectionId())
                    .cflowd(cflowd)
                    .qos(qos)
                    .build();
            dc.getEndpoint().add(endpoint);
        }

        String nsoDescription = conn.getDescription();
        if (nsoDescription.length() > 80) {
            nsoDescription = conn.getDescription().substring(0, 79);
        }

        return NsoVPLS.builder()
                .description(nsoDescription)
                .name(conn.getConnectionId())
                .qosMode(NsoVplsQosMode.GUARANTEED)
                .routingDomain(nsoProperties.getRoutingDomain())
                .vcId(vcid)
                .sdp(new ArrayList<>())
                .orchId("")
                .device(vplsDeviceMap.values().stream().toList())
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
