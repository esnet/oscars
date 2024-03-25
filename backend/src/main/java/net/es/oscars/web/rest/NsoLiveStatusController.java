package net.es.oscars.web.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.es.oscars.resv.enums.DeploymentState;
import net.es.oscars.resv.enums.State;
import net.es.oscars.sb.nso.db.NsoSdpIdDAO;
import net.es.oscars.sb.nso.ent.NsoSdpId;
import net.es.oscars.web.beans.NsoLiveStatusRequest;
import net.es.oscars.web.beans.OperationalState;
import net.es.oscars.web.beans.OperationalStateInfoResponse;
import net.es.oscars.web.beans.MacInfoResponse;
import net.es.oscars.sb.nso.rest.LiveStatusSapResult;
import net.es.oscars.sb.nso.rest.LiveStatusSdpResult;
import net.es.oscars.sb.nso.rest.MacInfoResult;
import net.es.oscars.sb.nso.rest.OperationalStateInfoResult;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.ent.VlanFixture;
import net.es.oscars.sb.nso.LiveStatusOperationalStateCacheManager;
import net.es.oscars.sb.nso.db.NsoVcIdDAO;
import net.es.oscars.sb.nso.ent.NsoVcId;

import net.es.topo.common.devel.DevelUtils;
import net.es.topo.common.dto.nso.enums.NsoVplsSdpPrecedence;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

import static net.es.oscars.web.beans.OperationalStateInfoResponse.UpDown.UP;
import static net.es.topo.common.dto.nso.enums.NsoVplsSdpPrecedence.PRIMARY;
import static net.es.topo.common.dto.nso.enums.NsoVplsSdpPrecedence.SECONDARY;

@Slf4j
@RestController
public class NsoLiveStatusController {

    private final LiveStatusOperationalStateCacheManager operationalStateCacheManager;
    private final NsoVcIdDAO nsoVcIdDAO;
    private final ConnService connSvc;
    private final NsoSdpIdDAO nsoSdpIdDAO;



    public NsoLiveStatusController(
            LiveStatusOperationalStateCacheManager operationalStateCacheManager,
            NsoVcIdDAO nsoVcIdDAO,
            ConnService connSvc, NsoSdpIdDAO nsoSdpIdDAO) {
        this.operationalStateCacheManager = operationalStateCacheManager;
        this.nsoVcIdDAO = nsoVcIdDAO;
        this.connSvc = connSvc;
        this.nsoSdpIdDAO = nsoSdpIdDAO;
    }

    @RequestMapping(value = "/api/mac/info", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public MacInfoResponse getMacInfo(@RequestBody NsoLiveStatusRequest request) {
        log.debug("MAC info request");

        // filter and extract request data
        RequestData requestData = getRequestData(request);
        if(requestData  == null) {
            log.info("Couldn't extract REST request data");
            throw new NoSuchElementException();
        }
        List<String> devices = requestData.getDevices();
        int serviceId = requestData.getServiceId();
        Connection conn = requestData.getConn();

        MacInfoResponse response = new MacInfoResponse();
        // the question is if we move this into the list with the results
        // since entries can have a different timestamp based on the if-older-than criteria
        response.setTimestamp(Instant.now());
        response.setConnectionId(request.getConnectionId()); // cp connId from request

        List<MacInfoResult> results = new LinkedList<>();

        log.debug("Run live-status request on devices");
        for (String device : devices) {
            if (conn.getState().equals(State.ACTIVE) &&
                    conn.getDeploymentState().equals(DeploymentState.DEPLOYED)) {
                log.debug("Fetch FDB from LiveStatusCacheManager for " + device + " service id " + serviceId);
                results.add(operationalStateCacheManager
                        .getMacs(device, serviceId, request.getRefreshIfOlderThan()).getMacInfoResult());
            } else {
                results.add(MacInfoResult.builder()
                                .device(device)
                                .errorMessage("Not deployed")
                                .status(false)
                                .timestamp(Instant.now())
                                .fdbQueryResult("Not deployed")
                        .build());
            }
        }
        response.setResults(results);
        return response;
    }

    @RequestMapping(value = "/api/operational-state/info", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public OperationalStateInfoResponse getOperationalStateInfo(@RequestBody NsoLiveStatusRequest request) {
        log.info("Operational state (SDPs, SAPs, LSPs) info request");

        // filter and extract request data
        RequestData requestData = getRequestData(request);
        if(requestData  == null) {
            log.info("Couldn't extract REST request data");
            throw new NoSuchElementException();
        }
        List<String> devices = requestData.getDevices();
        int serviceId = requestData.getServiceId();
        Connection conn = requestData.getConn();

        // this collects nsoSdpIds that oscars would set up, keyed off the device
        List<NsoSdpId> nsoSdpIds =  nsoSdpIdDAO.findNsoSdpIdByConnectionId(conn.getConnectionId());


        // start building REST return
        OperationalStateInfoResponse response = new OperationalStateInfoResponse();
        response.setTimestamp(Instant.now());
        response.setConnectionId(request.getConnectionId()); // cp connId from request
        Instant timestamp = request.getRefreshIfOlderThan();

        List<OperationalStateInfoResult> results = new ArrayList<>();
        ArrayList<LiveStatusSdpResult> allSdpsForAllDevices = new ArrayList<>();
        Map<String, ArrayList<LiveStatusSapResult>> allSapsForDevice = new HashMap<>();
        // Map<String, ArrayList<LiveStatusLspResult>> allLspsForDevice = new HashMap<>();


        log.debug("Run live-status request on devices and collect operational states");
        for (String device : devices) {
            if (conn.getState().equals(State.ACTIVE) &&
                    conn.getDeploymentState().equals(DeploymentState.DEPLOYED)) {
                log.info("Fetch SDPs, SAPs, and LSPs from LiveStatusCacheManager for " + device + " service id " + serviceId);
                List<LiveStatusSdpResult> sdpsOnDevice = operationalStateCacheManager.getSdp(device, serviceId, timestamp);

                // get SDPs, SAPs, and LSPs from cache manager
                allSdpsForAllDevices.addAll(sdpsOnDevice);
                allSapsForDevice.put(device, operationalStateCacheManager.getSap(device, serviceId, timestamp));
                // allLspsForDevice.put(device, operationalStateCacheManager.getLsp(device, timestamp));

                // this raw output is the same for all SDPs on the device
                String sdpRaw = "";
                for (LiveStatusSdpResult sdpResult : sdpsOnDevice) {
                    sdpRaw = sdpResult.getRaw();
                }

                String sapRaw = "";
                for (LiveStatusSapResult sapResult : allSapsForDevice.get(device)) {
                    sdpRaw = sapResult.getRaw();
                }

                OperationalStateInfoResult resultElement = OperationalStateInfoResult.builder()
                        .device(device)
                        .timestamp(timestamp)
                        .status(true)
                        .raw(sdpRaw+"\n"+sapRaw)
                        .build();

                        new OperationalStateInfoResult();

                results.add(resultElement);
            } else {
                results.add(OperationalStateInfoResult.builder()
                                .device(device)
                                .errorMessage("Not deployed")
                                .status(false)
                                .raw("")
                                .timestamp(timestamp)
                                .build());
            }
        }

        response.setResults(results);

        for (OperationalStateInfoResult result : response.getResults()) {
            String device = result.getDevice();

            // endpoints are simple to map:
            for (LiveStatusSapResult sapResult : allSapsForDevice.get(device)) {
                OperationalState endpointState = OperationalState.DOWN;
                if (sapResult.getOperationalState() && sapResult.getAdminState()) {
                    endpointState = OperationalState.UP;
                }
                OperationalStateInfoResponse.UpDown operState = sapResult.getOperationalState() ?
                        UP : OperationalStateInfoResponse.UpDown.DOWN;
                OperationalStateInfoResponse.UpDown adminState = sapResult.getAdminState() ?
                        UP : OperationalStateInfoResponse.UpDown.DOWN;
                response.getEndpoints().add(OperationalStateInfoResponse.EndpointOpInfo.builder()
                                .device(device)
                                .vlanId(sapResult.getVlan())
                                .port(sapResult.getPort())
                                .operState(operState)
                                .adminState(adminState)
                                .state(endpointState)
                                .build());
            }


            // mapping SDPs is slightly more complicated though
            dumpDebug(allSdpsForAllDevices);
            List<NsoSdpId> deviceSdpIds = nsoSdpIds.stream().filter(sdpId -> sdpId.getDevice().equals(device)).toList();

            // first we will see if the sdp id is up or down
            Map<String, Set<NsoSdpId>> byRemote = new HashMap<>();
            for (NsoSdpId nsoSdpId : deviceSdpIds) {
                if (!byRemote.containsKey(nsoSdpId.getTarget())) {
                    byRemote.put(nsoSdpId.getTarget(), new HashSet<>());
                }
                byRemote.get(nsoSdpId.getTarget()).add(nsoSdpId);
            }

            // for each far end, make a tunnel, then we have to figure out the health of the tunnel
            // each tunnel is composed of a primary SDP and maybe a secondary one as well.
            for (String remote : byRemote.keySet()) {
                Map<NsoVplsSdpPrecedence, Boolean> okByPrecedence = new HashMap<>();
                Set<OperationalStateInfoResponse.SdpOpInfo> sdpOpInfos = new HashSet<>();
                for (NsoSdpId nsoSdpId : byRemote.get(remote)) {
                    if (result.getSdps() != null) {
                        for (LiveStatusSdpResult sdpResult : result.getSdps()) {
                            if (sdpResult.getSdpId().equals(nsoSdpId.getSdpId())) {
                                OperationalStateInfoResponse.UpDown operState = sdpResult.getOperationalState() ?
                                        UP : OperationalStateInfoResponse.UpDown.DOWN;
                                OperationalStateInfoResponse.UpDown adminState = sdpResult.getAdminState() ?
                                        UP : OperationalStateInfoResponse.UpDown.DOWN;
                                String precedenceStr = nsoSdpId.getPrecedence();
                                NsoVplsSdpPrecedence precedence = PRIMARY;
                                if (precedenceStr.equals(SECONDARY.toString())) {
                                    precedence = SECONDARY;
                                }
                                if (operState.equals(UP) && adminState.equals(UP)) {
                                    okByPrecedence.put(precedence, true);
                                } else {
                                    okByPrecedence.put(precedence, false);
                                }
                                sdpOpInfos.add(OperationalStateInfoResponse.SdpOpInfo.builder()
                                        .sdpId(sdpResult.getSdpId())
                                        .vcId(sdpResult.getVcId())
                                        .operState(operState)
                                        .adminState(adminState)
                                        .precedence(precedence)
                                        .build());
                                break;
                            }
                        }
                    }

                }
                dumpDebug( sdpOpInfos);
                dumpDebug(okByPrecedence);

                // the rule is...
                // - if the primary SDP exists and is UP the tunnel is UP
                //    - otherwise, if the secondary exists and is UP the tunnel is DEGRADED
                //       - otherwise, the tunnel is DOWN
                OperationalState tunnelState = OperationalState.DOWN;
                if (okByPrecedence.containsKey(PRIMARY)) {
                    if (okByPrecedence.get(PRIMARY)) {
                        tunnelState = OperationalState.UP;
                    } else {
                        if (okByPrecedence.containsKey(NsoVplsSdpPrecedence.SECONDARY)) {
                            if (okByPrecedence.get(NsoVplsSdpPrecedence.SECONDARY)) {
                                tunnelState = OperationalState.DEGRADED;
                            }
                        }
                    }
                }
                response.getTunnels().add(OperationalStateInfoResponse.TunnelOpInfo.builder()
                                .state(tunnelState)
                                .sdps(sdpOpInfos.stream().toList())
                                .device(device)
                                .remote(remote)
                        .build());

            }
        }

        // overall state is...
        // UP, only if ALL endpoints and tunnels are UP
        // otherwise, DOWN only if any endpoint or tunnel is DOWN
        // otherwise, it is DEGRADED

        OperationalState overallState = OperationalState.UP;
        // endpoints can really only be UP or DOWN
        for (OperationalStateInfoResponse.EndpointOpInfo endpointOpInfo : response.getEndpoints()) {
            if (endpointOpInfo.getState().equals(OperationalState.DOWN)) {
                overallState = OperationalState.DOWN;
            }
        }

        // if all endpoints are up, then check tunnels
        boolean foundDown = false;
        boolean foundDegraded = false;
        if (overallState == OperationalState.UP) {
            for (OperationalStateInfoResponse.TunnelOpInfo tunnelOpInfo : response.getTunnels()) {
                if (tunnelOpInfo.getState().equals(OperationalState.DOWN)) {
                    foundDown = true;
                    break;
                } else if (tunnelOpInfo.getState().equals(OperationalState.DEGRADED)) {
                    foundDegraded = true;
                }
            }
            if (foundDown) {
                overallState = OperationalState.DOWN;
            } else if (foundDegraded) {
                overallState = OperationalState.DEGRADED;
            }
        }

        response.setState(overallState);


        return response;
    }

    // helper methods and POJOs

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class RequestData {
        private List<String> devices;
        private int serviceId;
        private Connection conn;
    }

    private RequestData getRequestData(NsoLiveStatusRequest request) {
        log.info("Request:" + request.toString());

        String connectionId = request.getConnectionId();
        if (connectionId == null) {
            log.info("REST request has no connection id!");
            throw new IllegalArgumentException();
        }

        HashSet<String> devicesFromId = new HashSet<String>();
        List<String> devicesFromRest = request.getDeviceIds();
        List<String> devices = new ArrayList<String>();

        // get circuit vc-id / service id
        Optional<NsoVcId> optVcid = nsoVcIdDAO.findNsoVcIdByConnectionId(connectionId);
        Integer vcid = 0;
        if (optVcid.isPresent()) {
            vcid = optVcid.get().getVcId();
        } else {
            log.info("Couldn't find VC-ID for OSCARS circuit " + connectionId);
            throw new NoSuchElementException();
        }

        // find devices in circuit
        Connection conn = connSvc.findConnection(connectionId);
        if (conn == null) {
            log.info("Couldn't find OSCARS circuit for connection id " + connectionId);
            throw new NoSuchElementException();
        }

        for (VlanFixture f : conn.getReserved().getCmp().getFixtures()) {
            String deviceUrn = f.getJunction().getDeviceUrn();
            devicesFromId.add(deviceUrn);
            log.debug("Adding device: " + deviceUrn);
        }

        // if no devices are listed in the request we use all devices from the circuit
        if (devicesFromRest == null || devicesFromRest.isEmpty()) {
            devicesFromRest = new ArrayList<>(devicesFromId);
        }

        // extract subset
        for(String device : devicesFromRest) {
            if(devicesFromId.contains(device)) {
                devices.add(device);
            }
        }

        return RequestData.builder()
                .devices(devices)
                .serviceId(vcid)
                .conn(conn)
                .build();
    }
    private void dumpDebug(Object o) {
        String pretty = null;

        try {
            pretty = (new ObjectMapper())
                    .registerModule(new JavaTimeModule())
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(o);
        } catch (JsonProcessingException ex) {
            log.error(ex.getMessage());
        }

        log.info(pretty);
    }

}
