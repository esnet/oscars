package net.es.oscars.sb.nso;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.NsoProperties;
import net.es.oscars.sb.nso.exc.NsoCommitException;
import net.es.oscars.app.exc.PSSException;
import net.es.oscars.dto.pss.cmd.CommandType;
import net.es.oscars.dto.pss.st.ConfigStatus;
import net.es.oscars.sb.nso.exc.NsoDryrunException;
import net.es.oscars.sb.nso.exc.NsoGenException;
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
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.es.oscars.resv.svc.ResvLibrary.validateServiceId;
import static net.es.oscars.sb.nso.IntegerSet.*;

@Component
@Slf4j
public class NsoAdapter {
    public static String NSO_TEMPLATE_VERSION = "NSO 1.1";
    public static String WORK_LSP_NAME_PIECE = "WRK";
    public static String PROTECT_LSP_NAME_PIECE = "PRT";
    public static String LSP_NAME_DELIMITER = "-";
    public static String VPLS_NAME_PREFIX = "OSCARS-";
    public static String LSP_ORCHID_PREFIX = "OSCARS-";
    public static String VPLS_ORCHID_PREFIX = "OSCARS-";

    private final String regexPatternOscarsManagedLsp = "(.*)" + LSP_NAME_DELIMITER + "(" + WORK_LSP_NAME_PIECE + "|" + PROTECT_LSP_NAME_PIECE + ")" + LSP_NAME_DELIMITER + "(.*)";
    private final Pattern oscarsManagedLspPattern = Pattern.compile(regexPatternOscarsManagedLsp);


    private final NsoProperties nsoProperties;

    private final NsoProxy nsoProxy;


    private final CommandHistoryRepository historyRepo;

    private final RouterCommandsRepository rcr;

    private final MiscHelper miscHelper;

    public NsoAdapter(NsoProperties nsoProperties, NsoProxy nsoProxy, MiscHelper miscHelper,
                      CommandHistoryRepository historyRepo, RouterCommandsRepository rcr) {
        this.nsoProperties = nsoProperties;
        this.nsoProxy = nsoProxy;
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
        String connectionId = conn.getConnectionId();

        boolean shouldWriteHistory = false;

        if (commandType.equals(CommandType.BUILD) || commandType.equals(CommandType.DISMANTLE) || commandType.equals(CommandType.REDEPLOY)) {
            log.info("generating NSO payload for "+conn.getConnectionId()+" "+commandType);
            try {
                switch (commandType) {
                    case BUILD -> {
                        NsoServicesWrapper oscarsServices = this.nsoOscarsServices(conn);
                        commands = oscarsServices.asCliCommands();
                        log.info("BUILD cli commands\n"+commands);
                        dryRun = nsoProxy.buildDryRun(oscarsServices, conn.getConnectionId());
                        nsoProxy.buildServices(oscarsServices, conn.getConnectionId());
                        newDepState = DeploymentState.DEPLOYED;

                    }
                    case DISMANTLE ->  {
                        Optional<NsoOscarsDismantle> maybeDismantle = this.nsoOscarsDismantle(connectionId);
                        if (maybeDismantle.isPresent()) {
                            NsoOscarsDismantle dismantle = maybeDismantle.get();
                            commands = dismantle.asCliCommands();
                            log.info("DISMANTLE cli \n"+commands);
                            dryRun = nsoProxy.dismantleDryRun(dismantle);
                            nsoProxy.deleteServices(dismantle);
                            newDepState = DeploymentState.UNDEPLOYED;

                        } else {
                            log.info("DISMANTLE "+connectionId+" - no NSO action required \n");
                            newDepState = DeploymentState.UNDEPLOYED;
                        }

                    }
                    case REDEPLOY ->  {
                        NsoServicesWrapper oscarsServices = this.nsoOscarsServices(conn);
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
        log.info("completed NSO task {} {} {}; new states {}/{}", conn.getConnectionId(), commandType, intent, newState, newDepState);

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
            Set<NsoLSP.Hop> nsoHops = new HashSet<>();
            Set<MplsHop> mplsHops;
            try {
                mplsHops = miscHelper.mplsHops(hops);
            } catch (PSSException e) {
                throw new NsoGenException(e.getMessage());
            }

            for (MplsHop hop : mplsHops) {
                nsoHops.add(NsoLSP.Hop.builder()
                        .ipv4(hop.getAddress())
                        .number(hop.getOrder())
                        .build());
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



    public Optional<NsoOscarsDismantle> nsoOscarsDismantle(String connectionId) throws NsoGenException {
        if (connectionId == null || connectionId.isEmpty()) {
            throw new NsoGenException("connectionId is null or empty");
        }
        OscarsNsoState nsoState = this.fetchNsoState();
        if (!nsoState.getServiceMap().containsKey(connectionId)) {
            return Optional.empty();
        } else {
            Pair<NsoVPLS, List<NsoLSP>> nsoServices = nsoState.getServiceMap().get(connectionId);
            NsoVPLS nsoVPLS = nsoServices.getFirst();
            List<NsoLSP> nsoLSPs = nsoServices.getSecond();

            List<String> lspInstanceKeys = new ArrayList<>();
            for (NsoLSP nsoLSP : nsoLSPs) {
                String instanceKey = nsoLSP.getName()+","+nsoLSP.getTarget();
                lspInstanceKeys.add(instanceKey);
            }
            return Optional.of(NsoOscarsDismantle.builder()
                    .connectionId(connectionId)
                    .vcId(nsoVPLS.getVcId())
                    .lspNsoKeys(lspInstanceKeys)
                    .build());

        }
    }

    public NsoServicesWrapper nsoOscarsServices(Connection conn) throws NsoGenException {
        log.info("making NSO services wrapper for "+conn.getConnectionId());
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

        String connectionId = conn.getConnectionId();
        OscarsNsoState nsoState = this.fetchNsoState();

        Integer vcId = getVplsVcId(connectionId, nsoState);

        Map<Long, Integer> sapQosIds = getSapQosIds(vcId, conn.getReserved().getCmp().getFixtures(), nsoState);

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
                        .endpoint(new HashSet<>())
                        .virtualIfces(new HashSet<>())
                        .build();
                vplsDeviceMap.put(deviceUrn, dc);
            }
            Integer sapQosId = sapQosIds.get(f.getId());

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
                    .qosId(sapQosId)
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
        Map<Long, Map<NsoVplsSdpPrecedence, Integer>> sdpIdMap = getSdpIds(conn.getReserved().getCmp().getPipes(), vcId, nsoState);

        Map<Long, Map<AZWithPrecedence, Integer>> sdpVcIdMap = getSdpVcIds(conn.getReserved().getCmp().getPipes(), vcId, nsoState);

        HashSet<NsoVPLS.SDP> sdps = new HashSet<>();
        for (VlanPipe pipe : conn.getReserved().getCmp().getPipes()) {
            Integer primarySdpId = sdpIdMap.get(pipe.getId()).get(NsoVplsSdpPrecedence.PRIMARY);
            Integer protectSdpId = sdpIdMap.get(pipe.getId()).get(NsoVplsSdpPrecedence.SECONDARY);;

            AZWithPrecedence priKey = new AZWithPrecedence(pipe.getA().getDeviceUrn(), pipe.getZ().getDeviceUrn(), NsoVplsSdpPrecedence.PRIMARY);
            AZWithPrecedence secKey = new AZWithPrecedence(pipe.getA().getDeviceUrn(), pipe.getZ().getDeviceUrn(), NsoVplsSdpPrecedence.SECONDARY);

            Integer primarySdpVcId = sdpVcIdMap.get(pipe.getId()).get(priKey);
            Integer protectSdpVcId = sdpVcIdMap.get(pipe.getId()).get(secKey);;


            if (primarySdpId == null) {
                throw new NsoGenException("could not locate primary SDP id");
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
                    .vcId(primarySdpVcId)
                    .mode(NsoVplsSdpMode.SPOKE)
                    .build();
            NsoVPLS.SDPMember z = NsoVPLS.SDPMember.builder()
                    .device(pipe.getZ().getDeviceUrn())
                    .lsp(zaLspName)
                    .vcId(primarySdpVcId)
                    .mode(NsoVplsSdpMode.SPOKE)
                    .build();
            NsoVPLS.SDP sdp = NsoVPLS.SDP.builder()
                    .description(conn.getConnectionId())
                    .precedence(NsoVplsSdpPrecedence.PRIMARY)
                    .a(a)
                    .z(z)
                    .sdpId(primarySdpId)
                    .build();
            sdps.add(sdp);

            if (pipe.getProtect()) {
                if (protectSdpId == null) {
                    throw new NsoGenException("could not locate protect SDP id");
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
                        .vcId(protectSdpVcId)
                        .mode(NsoVplsSdpMode.SPOKE)
                        .build();
                NsoVPLS.SDPMember protectZ = NsoVPLS.SDPMember.builder()
                        .device(pipe.getZ().getDeviceUrn())
                        .lsp(zaProtectLspName)
                        .mode(NsoVplsSdpMode.SPOKE)
                        .vcId(protectSdpVcId)
                        .build();

                NsoVPLS.SDP protectSdp = NsoVPLS.SDP.builder()
                        .description(conn.getConnectionId())
                        .precedence(NsoVplsSdpPrecedence.SECONDARY)
                        .a(protectA)
                        .z(protectZ)
                        .sdpId(protectSdpId)
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
                .name(VPLS_NAME_PREFIX+conn.getConnectionId())
                .qosMode(NsoVplsQosMode.GUARANTEED)
                .routingDomain(nsoProperties.getRoutingDomain())
                .vcId(vcId)
                .sdp(sdps)
                .device(new HashSet<>(vplsDeviceMap.values()))
                .build();

        List<NsoVPLS> vplsInstances = new ArrayList<>();
        vplsInstances.add(vpls);
        NsoProxy.logNsoObject(vpls);
        for (NsoLSP lsp : lspInstances) {
            NsoProxy.logNsoObject(lsp);
        }
        return NsoServicesWrapper.builder()
                .lspInstances(lspInstances)
                .vplsInstances(vplsInstances)
                .build();
    }



    // pull in state from NSO, map things to OSCARS connection ids
    public OscarsNsoState fetchNsoState() throws NsoGenException {
        Set<String> connectionIds = new HashSet<>();
        List<NsoVPLS> allVplsList;
        List<NsoLSP> allLspList;
        try {
            allVplsList = nsoProxy.getVpls().getNsoVpls();
            allLspList = nsoProxy.getLsps().getNsoLSPs();
        } catch (Exception ex) {
            throw new NsoGenException("error retrieving NSO state");
        }

        List<NsoVPLS> vplsList = allVplsList.stream()
                .filter(this::isOscarsManaged)
                .toList();
        Map<String, NsoVPLS> vplsMap = new HashMap<>();
        for (NsoVPLS nsoVPLS : vplsList) {
            String connectionId = oscarsConnectionId(nsoVPLS);
            connectionIds.add(connectionId);
            vplsMap.put(connectionId, nsoVPLS);
        }


        List<NsoLSP> lspList = allLspList.stream()
                .filter(this::isOscarsManaged)
                .toList();

        Map<String, List<NsoLSP>> lspMap = new HashMap<>();
        lspList.forEach(nsoLSP -> {
            this.oscarsConnectionId(nsoLSP).ifPresent(connectionId -> {
                if (!lspMap.containsKey(connectionId)) {
                    lspMap.put(connectionId, new ArrayList<>());
                }
                lspMap.get(connectionId).add(nsoLSP);
                connectionIds.add(connectionId);
            });
        });

        Map<String, Pair<NsoVPLS, List<NsoLSP>>> serviceMap = new HashMap<>();
        for (String connectionId : connectionIds) {
            serviceMap.put(connectionId, Pair.of(vplsMap.get(connectionId), lspMap.get(connectionId)));
        }

        return(OscarsNsoState.builder()
                .allLspList(allLspList)
                .lspList(lspList)
                .allVplsList(allVplsList)
                .vplsList(vplsList)
                .serviceMap(serviceMap)
                .build());
    }

    public boolean isOscarsManaged(NsoVPLS vpls) {
        Set<Integer> managedVcIds = singleSetFromExpr(nsoProperties.getVcIdRange());
        return managedVcIds.contains(vpls.getVcId());
    }

    public boolean isOscarsManaged(NsoLSP lsp) {
        String candidate = lsp.getOrchId();
        // try the orch-id first; only ones with OSCARS- as the prefix are ours
        if (candidate != null && !candidate.isEmpty()) {
            return candidate.startsWith(LSP_ORCHID_PREFIX);
        } else {
            // if there's no orch-id, see if the LSP name matches our pattern
            candidate = lsp.getName();
            Matcher matcher = oscarsManagedLspPattern.matcher(candidate);
            return matcher.find();
        }
    }


    public String oscarsConnectionId(NsoVPLS vpls) {
        String candidate = vpls.getOrchId();
        if (candidate == null || candidate.isEmpty()) {
            candidate = vpls.getName();
            return (candidate.replace(VPLS_NAME_PREFIX, ""));
        } else {
            return (candidate.replace(VPLS_ORCHID_PREFIX, ""));
        }

    }


    public Optional<String> oscarsConnectionId(NsoLSP lsp) {
        String candidate = lsp.getOrchId();
        // try the orch-id first. return the non "OSCARS-" bit
        if (candidate != null && !candidate.isEmpty()) {
            if (candidate.startsWith(LSP_ORCHID_PREFIX)) {
                return Optional.of(candidate.replace(LSP_ORCHID_PREFIX, ""));
            } else {
                return Optional.empty();
            }
        } else {
            // if there's no orch-id, see if the name matches our pattern
            Matcher matcher = oscarsManagedLspPattern.matcher(lsp.getName());
            if (matcher.find()) {
                return Optional.of(matcher.group(1));
            } else {
                return Optional.empty();
            }
        }
    }

    public Integer getVplsVcId(String connectionId, OscarsNsoState nsoState) throws NsoGenException {
        if (nsoState.getServiceMap().containsKey(connectionId)) {
            // we might already have a vcId for this VPLS, so return it
            NsoVPLS nsoVPLS = nsoState.getServiceMap().get(connectionId).getFirst();
            if (nsoVPLS != null) {
                return nsoVPLS.getVcId();
            }
        }
        // all
        Set<Integer> usedVcIds = nsoState.getAllVplsList().stream().map(NsoVPLS::getVcId).collect(Collectors.toSet());
        Set<Integer> availableVcIds = availableFromUsedSetAndAllowedString(usedVcIds, nsoProperties.getVcIdRange());
        if (availableVcIds.isEmpty()) {
            throw new NsoGenException("no VC id available");
        }
        return Collections.min(availableVcIds);
    }

    public Map<Long, Integer> getSapQosIds(Integer vcId, List<VlanFixture> fixtures, OscarsNsoState nsoState) throws NsoGenException {

        Map<String, Set<Integer>> inUseOnDevice = new HashMap<>();
        Map<String, Integer> inUseByExistingInstance = new HashMap<>();

        // collect all Qos ids in use
        nsoState.getAllVplsList()
                .forEach(nsoVPLS -> {
                    // if we find an existing instance, keep it
                    if (nsoVPLS.getVcId().equals(vcId)) {
                        nsoVPLS.getDevice().forEach(deviceContainer -> {
                            deviceContainer.getEndpoint().forEach(endpoint -> {
                                if (endpoint.getQos() != null) {
                                    String fixtureKey = deviceContainer.getDevice()+":"+endpoint.getIfce()+":"+endpoint.getVlanId();
                                    inUseByExistingInstance.put(fixtureKey, endpoint.getQos().getQosId());
                                }
                            });
                        });

                    } else {
                    // for all other instances, mark their QosIds as in-use
                        nsoVPLS.getDevice().forEach(device -> {
                            if (!inUseOnDevice.containsKey(device.getDevice())) {
                                inUseOnDevice.put(device.getDevice(), new HashSet<>());
                            }
                            device.getEndpoint().forEach(endpoint -> {
                                if (endpoint.getQos() != null) {
                                    inUseOnDevice.get(device.getDevice()).add(endpoint.getQos().getQosId());
                                }
                            });
                        });
                    }
                });

        Map<Long, Integer> result = new HashMap<>();
        for (VlanFixture fixture : fixtures) {
            Integer sapQosId;
            String deviceId = fixture.getJunction().getDeviceUrn();
            Set<Integer> usedSapQosIds = inUseOnDevice.get(deviceId);
            String fixtureKey = fixture.getPortUrn()+":"+fixture.getVlan().getVlanId();

            // if there was a QosId for that fixture, reuse it
            if (inUseByExistingInstance.containsKey(fixtureKey)) {
                sapQosId = inUseByExistingInstance.get(fixtureKey);
            } else {

                Set<Integer> availableSapQosIds = availableFromUsedSetAndAllowedString(usedSapQosIds, nsoProperties.getSapQosIdRange());
                if (availableSapQosIds.isEmpty()) {
                    throw new NsoGenException("no SAP QoS id available for device: " + deviceId);
                }
                // choose the lowest value
                sapQosId = Collections.min(availableSapQosIds);
            }

            // mark it as being used
            inUseOnDevice.get(deviceId).add(sapQosId);
            result.put(fixture.getId(), sapQosId);
        }

        return result;

    }

    public Map<Long, Map<NsoVplsSdpPrecedence, Integer>> getSdpIds(List<VlanPipe> pipes, Integer vcId, OscarsNsoState nsoState) throws NsoGenException {
        // we mark SDP ids as in use globally even though they just need to be unique on each device

        Set<Integer> allowedSdpIds = singleSetFromExpr(nsoProperties.getSdpIdRange());
        Set<Integer> inUseGlobally = new HashSet<>();
        Map<AZWithPrecedence, Integer> inUseByExistingInstance = new HashMap<>();

        nsoState.getAllVplsList()
                .forEach(nsoVPLS -> {
                    // if we find an existing instance, keep it around
                    if (nsoVPLS.getVcId().equals(vcId)) {
                        nsoVPLS.getSdp().forEach(sdp -> {
                            AZWithPrecedence pipeKey = new AZWithPrecedence(sdp.getA().getDevice(), sdp.getZ().getDevice(), sdp.getPrecedence());
                            inUseByExistingInstance.put(pipeKey, sdp.getSdpId());
                        });
                    } else {
                        // for all other instances, mark their SdpIds as in-use
                        nsoVPLS.getDevice().forEach(device -> {
                            nsoVPLS.getSdp().forEach(sdp -> {
                                inUseGlobally.add(sdp.getSdpId());
                            });
                        });
                    }
                });
        Map<Long, Map<NsoVplsSdpPrecedence, Integer>> result = new HashMap<>();
        for (VlanPipe pipe : pipes) {
            result.put(pipe.getId(), new HashMap<>());

            for (NsoVplsSdpPrecedence precedence : NsoVplsSdpPrecedence.values()) {
                // we don't do the work for SECONDARY if there's no protect on the pipe
                if (precedence.equals(NsoVplsSdpPrecedence.SECONDARY) && !pipe.getProtect()) {
                    continue;
                }

                Integer sdpId;
                AZWithPrecedence pipeKey = new AZWithPrecedence(pipe.getA().getDeviceUrn(), pipe.getZ().getDeviceUrn(), precedence);
                if (inUseByExistingInstance.containsKey(pipeKey)) {
                    // reuse the existing sdp id
                    sdpId = inUseByExistingInstance.get(pipeKey);

                } else {
                    Set<Integer> availableSdpIds = availableFromUsedAndAllowedSets(inUseGlobally, allowedSdpIds);
                    if (availableSdpIds.isEmpty()) {
                        throw new NsoGenException("no SDP id available ");
                    }
                    // choose the lowest value
                    sdpId = Collections.min(availableSdpIds);
                }
                inUseGlobally.add(sdpId);
                result.get(pipe.getId()).put(precedence, sdpId);
            }
        }
        return result;
    }

    public Map<Long, Map<AZWithPrecedence, Integer>> getSdpVcIds(List<VlanPipe> pipes, Integer vcId, OscarsNsoState nsoState) throws NsoGenException {
        // we mark SDP VC ids as in use globally even though they just need to be unique and match per (A, Z) pair

        Set<Integer> allowedSdpVcIds = singleSetFromExpr(nsoProperties.getVcIdRange());
        Set<Integer> inUseGlobally = new HashSet<>();

        Map<AZWithPrecedence, Integer> inUseByExistingInstance = new HashMap<>();

        nsoState.getAllVplsList()
                .forEach(nsoVPLS -> {
                    // if we find an existing instance, keep around what sdpVcIds it is currently using
                    if (nsoVPLS.getVcId().equals(vcId)) {
                        nsoVPLS.getSdp().forEach(sdp -> {
                            AZWithPrecedence pipeAzKey = new AZWithPrecedence(sdp.getA().getDevice(), sdp.getZ().getDevice(), sdp.getPrecedence());
                            inUseByExistingInstance.put(pipeAzKey, sdp.getA().getVcId());

                            AZWithPrecedence pipeZaKey = new AZWithPrecedence(sdp.getZ().getDevice(), sdp.getA().getDevice(), sdp.getPrecedence());
                            inUseByExistingInstance.put(pipeZaKey, sdp.getZ().getVcId());
                        });
                    } else {
                        // for all other instances, mark all their SdpVcIds as in-use
                        nsoVPLS.getDevice().forEach(device -> {
                            nsoVPLS.getSdp().forEach(sdp -> {
                                inUseGlobally.add(sdp.getA().getVcId());
                                inUseGlobally.add(sdp.getZ().getVcId());
                            });
                        });
                    }
                });

        Map<Long, Map<AZWithPrecedence, Integer>> result = new HashMap<>();
        for (VlanPipe pipe : pipes) {
            result.put(pipe.getId(), new HashMap<>());

            for (NsoVplsSdpPrecedence precedence : NsoVplsSdpPrecedence.values()) {
                // we don't do the work for SECONDARY if there's no protect on the pipe
                if (precedence.equals(NsoVplsSdpPrecedence.SECONDARY) && !pipe.getProtect()) {
                    continue;
                }
                Pair<String, String> az = Pair.of(pipe.getA().getDeviceUrn(), pipe.getZ().getDeviceUrn());
                Pair<String, String> za = Pair.of(pipe.getZ().getDeviceUrn(), pipe.getA().getDeviceUrn());
                List<Pair<String, String>> pairs = new ArrayList<>();
                pairs.add(az);
                pairs.add(za);

                for (Pair<String, String> pair : pairs) {
                    Integer sdpVcId;
                    AZWithPrecedence pipeKey = new AZWithPrecedence(pair.getFirst(), pair.getSecond(), precedence);
                    if (inUseByExistingInstance.containsKey(pipeKey)) {
                        // reuse the existing sdp id
                        sdpVcId = inUseByExistingInstance.get(pipeKey);
                    } else {
                        Set<Integer> availableSdpVcIds = availableFromUsedAndAllowedSets(inUseGlobally, allowedSdpVcIds);
                        if (availableSdpVcIds.isEmpty()) {
                            throw new NsoGenException("no SDP vc id available ");
                        }
                        // choose the lowest value
                        sdpVcId = Collections.min(availableSdpVcIds);
                    }
                    inUseGlobally.add(sdpVcId);
                    result.get(pipe.getId()).put(pipeKey, sdpVcId);
                }
            }
        }
        return result;
    }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OscarsNsoState {
        @Builder.Default
        public List<NsoLSP> lspList = new ArrayList<>();
        @Builder.Default
        public List<NsoLSP> allLspList = new ArrayList<>();

        @Builder.Default
        public List<NsoVPLS> allVplsList = new ArrayList<>();
        @Builder.Default
        public List<NsoVPLS> vplsList = new ArrayList<>();
        @Builder.Default
        public Map<String, Pair<NsoVPLS, List<NsoLSP>>> serviceMap = new HashMap<>();

    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AZWithPrecedence {
        String a;
        String z;
        NsoVplsSdpPrecedence precedence;
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
