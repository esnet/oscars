package net.es.oscars.web.rest;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.resv.enums.DeploymentState;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.enums.State;
import net.es.oscars.sb.nso.NsoAdapter;
import net.es.oscars.sb.nso.exc.NsoReadException;
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
import net.es.oscars.sb.nso.NsoLiveStatusMgr;

import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.NsoVPLS;
import net.es.topo.common.dto.nso.enums.NsoVplsSdpPrecedence;
import org.springframework.data.util.Pair;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

import static net.es.oscars.web.beans.OperationalStateInfoResponse.UpDown.DOWN;
import static net.es.oscars.web.beans.OperationalStateInfoResponse.UpDown.UP;
import static net.es.topo.common.dto.nso.enums.NsoVplsSdpPrecedence.PRIMARY;
import static net.es.topo.common.dto.nso.enums.NsoVplsSdpPrecedence.SECONDARY;

@Slf4j
@RestController
public class NsoLiveStatusController {
    private final NsoAdapter nsoAdapter;
    private final NsoLiveStatusMgr nsoLiveStatusMgr;
    private final ConnService connSvc;

    private final Startup startup;

    public NsoLiveStatusController(
            NsoAdapter nsoAdapter, NsoLiveStatusMgr nsoLiveStatusMgr,
            ConnService connSvc, Startup startup) {
        this.nsoAdapter = nsoAdapter;
        this.nsoLiveStatusMgr = nsoLiveStatusMgr;
        this.connSvc = connSvc;
        this.startup = startup;
    }

    @RequestMapping(value = "/api/mac/info", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public MacInfoResponse getMacInfo(@RequestBody NsoLiveStatusRequest request) throws StartupException, NsoReadException {
        log.debug("MAC info request");
        startup.startupCheck();

        Connection conn = connSvc.findConnection(request.getConnectionId()).orElseThrow(NoSuchElementException::new);

        // start building REST return
        MacInfoResponse response = new MacInfoResponse();
        response.setConnectionId(conn.getConnectionId());
        response.setTimestamp(Instant.now());

        response.setTimestamp(Instant.now());
        response.setConnectionId(request.getConnectionId()); // cp connId from request

        boolean canGetLiveStatus = false;
        String message = "connection not in RESERVED phase";
        if (conn.getPhase().equals(Phase.RESERVED)) {
            message = "connection RESERVED but not ACTIVE";
            if (conn.getState().equals(State.ACTIVE)) {
                message = "connection RESERVED and ACTIVE but not DEPLOYED";
                if (conn.getDeploymentState().equals(DeploymentState.DEPLOYED)) {
                    canGetLiveStatus = true;
                    message = "Connection RESERVED, ACTIVE, and DEPLOYED";
                }
            }
        }
        if (!canGetLiveStatus) {
            response.setResults(Collections.emptyList());
            response.setMessage(message);
            return response;
        }

        // filter and extract request data
        RequestData requestData = requestData = getRequestData(request, conn);

        // the question is if we move this into the list with the results
        // since entries can have a different timestamp based on the if-older-than criteria

        List<MacInfoResult> results = new LinkedList<>();
        log.debug("Run live-status request on devices");
        for (String device : requestData.getDevices()) {
            log.debug("Fetch FDB from LiveStatusCacheManager for {} service id {}", device, requestData.getServiceId());
            results.add(nsoLiveStatusMgr
                    .getMacs(device, requestData.getServiceId(), request.getRefreshIfOlderThan())
                    .getMacInfoResult());
        }
        response.setResults(results);
        return response;
    }

    @RequestMapping(value = "/api/operational-state/info", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public OperationalStateInfoResponse getOperationalStateInfo(@RequestBody NsoLiveStatusRequest request) throws StartupException, NsoReadException {
        log.info("Operational state (SDPs, SAPs, LSPs) info request");
        startup.startupCheck();

        String connectionId = request.getConnectionId();

        // find devices in circuit
        Connection conn = connSvc.findConnection(connectionId).orElseThrow(NoSuchElementException::new);

        // start building REST return
        OperationalStateInfoResponse response = new OperationalStateInfoResponse();
        response.setTimestamp(Instant.now());
        response.setConnectionId(request.getConnectionId()); // cp connId from request
        boolean canGetLiveStatus = false;
        String message = "connection not in RESERVED phase";
        if (conn.getPhase().equals(Phase.RESERVED)) {
            message = "connection RESERVED but not ACTIVE";
            if (conn.getState().equals(State.ACTIVE)) {
                message = "connection RESERVED and ACTIVE but not DEPLOYED";
                if (conn.getDeploymentState().equals(DeploymentState.DEPLOYED)) {
                    canGetLiveStatus = true;
                    message = "Connection RESERVED, ACTIVE, and DEPLOYED";
                }
            }
        }
        if (!canGetLiveStatus) {
            response.setResults(Collections.emptyList());
            response.setMessage(message);
            response.setState(OperationalState.DOWN);
            return response;
        }


        // filter and extract request data
        RequestData requestData = getRequestData(request, conn);
        if (requestData == null) {
            log.info("Couldn't extract REST request data");
            throw new NoSuchElementException();
        }
        List<String> devices = requestData.getDevices();
        int serviceId = requestData.getServiceId();


        // if the user tells us when to refresh then use their timestamp,
        // otherwise we cache operational info for up to 60 seconds
        Instant timestamp = request.getRefreshIfOlderThan();
        if (timestamp == null) {
            timestamp = Instant.now().minusSeconds(60);
        }

        List<OperationalStateInfoResult> results = new ArrayList<>();
        ArrayList<LiveStatusSdpResult> allSdpsForAllDevices = new ArrayList<>();
        Map<String, List<LiveStatusSdpResult>> sdpsByDevice =  new HashMap<>();
        Map<String, ArrayList<LiveStatusSapResult>> allSapsForDevice = new HashMap<>();
        // Map<String, ArrayList<LiveStatusLspResult>> allLspsForDevice = new HashMap<>();

        log.debug("Run live-status request on devices and collect operational states");
        for (String device : devices) {
            if (conn.getState().equals(State.ACTIVE) &&
                    conn.getDeploymentState().equals(DeploymentState.DEPLOYED)) {
                log.info("Fetch SDPs, SAPs, and LSPs from LiveStatusCacheManager for " + device + " service id " + serviceId);
                List<LiveStatusSdpResult> sdpsOnDevice = nsoLiveStatusMgr.getSdp(device, serviceId, timestamp);
                List<LiveStatusSapResult> sapsOnDevice = nsoLiveStatusMgr.getSap(device, serviceId);

                // get SDPs, SAPs, and LSPs from cache manager
                allSdpsForAllDevices.addAll(sdpsOnDevice);
                sdpsByDevice.put(device, sdpsOnDevice);
                allSapsForDevice.put(device, new ArrayList<>(sapsOnDevice));
                // allLspsForDevice.put(device, operationalStateCacheManager.getLsp(device, timestamp));

                // this raw output is the same for all SDPs on the device
                String sdpRaw = "";
                for (LiveStatusSdpResult sdpResult : sdpsOnDevice) {
                    sdpRaw = sdpResult.getRaw();
                }

                String sapRaw = "";
                for (LiveStatusSapResult sapResult : allSapsForDevice.get(device)) {
                    sapRaw = sapResult.getRaw();
                }

                OperationalStateInfoResult resultElement = OperationalStateInfoResult.builder()
                        .device(device)
                        .timestamp(timestamp)
                        .status(true)
                        .sdps(sdpsOnDevice)
                        .saps(sapsOnDevice)
                        .raw(sdpRaw + "\n" + sapRaw)
                        .build();

                new OperationalStateInfoResult();

                results.add(resultElement);
            } else {
                results.add(OperationalStateInfoResult.builder()
                        .device(device)
                        .errorMessage("Not deployed")
                        .status(false)
                        .sdps(new ArrayList<>())
                        .saps(new ArrayList<>())
                        .raw("")
                        .timestamp(timestamp)
                        .build());
            }
        }

        response.setResults(results);
        // dumpDebug("allsdps", allSdpsForAllDevices);
        // dumpDebug("nsoSdpIds", nsoSdpIds);

        // collect the NSO configuration state
        NsoAdapter.OscarsNsoState nsoState = nsoAdapter.fetchNsoState(false);
        Pair<NsoVPLS, List<NsoLSP>> nsoServices = nsoState.getServiceMap().get(connectionId);
        NsoVPLS nsoVpls = nsoServices.getFirst();

        // now we can start generating the live status

        // Define our tunnels. Each tunnel...
        // - goes FROM a "device" TO a "remote" (device) - that pair is the key to the hashmap
        // - can contain multiple SDPs with different precedences
        //
        Map<Pair<String, String>, OperationalStateInfoResponse.TunnelOpInfo> tunnelMap = new HashMap<>();
        nsoVpls.getSdp().forEach(sdp -> {
            String aDevice = sdp.getA().getDevice();
            String zDevice = sdp.getZ().getDevice();

            Set<Pair<String, String>> keys = new HashSet<>();
            keys.add(Pair.of(aDevice, zDevice));
            keys.add(Pair.of(zDevice, aDevice));
            for (Pair<String, String> key : keys) {
                if (!tunnelMap.containsKey(key)) {
                    tunnelMap.put(key, OperationalStateInfoResponse.TunnelOpInfo.builder()
                            .state(OperationalState.DOWN)
                            .device(key.getFirst())
                            .remote(key.getSecond())
                            .sdps(new ArrayList<>())
                            .build());
                }

                OperationalStateInfoResponse.UpDown sdpAdminState = DOWN;
                OperationalStateInfoResponse.UpDown sdpOperState = DOWN;

                for (LiveStatusSdpResult sdpResult : sdpsByDevice.get(key.getFirst())) {
                    if (sdpResult.getSdpId().equals(sdp.getSdpId())) {
                        sdpAdminState = sdpResult.getAdminState() ? UP : OperationalStateInfoResponse.UpDown.DOWN;
                        sdpOperState = sdpResult.getOperationalState() ? UP : OperationalStateInfoResponse.UpDown.DOWN;
                    }
                }

                tunnelMap.get(key).getSdps().add(OperationalStateInfoResponse.SdpOpInfo.builder()
                        .sdpId(sdp.getSdpId())
                        .vcId(sdp.getA().getVcId())
                        .precedence(sdp.getPrecedence())
                        .adminState(sdpAdminState)
                        .operState(sdpOperState)
                        .build());
            }
        });

        // we should have all the tunnels with their SDPs and the SDP states at this point
        // we will decide the overall tunnel state:
        // - the tunnel is DOWN , unless...
        // - a secondary SDP exists _and_ is UP, the tunnel is DEGRADED, unless...
        // - the primary SDP exists _and_ is UP : the tunnel is UP

        for (Pair<String, String> key : tunnelMap.keySet()) {
            Map<NsoVplsSdpPrecedence, OperationalStateInfoResponse.UpDown> sdpStates = new HashMap<>();
            tunnelMap.get(key).getSdps().forEach(sdp -> {
                sdpStates.put(sdp.getPrecedence(), sdp.getOperState());
            });

            OperationalState tunnelState = OperationalState.DOWN;
            if (sdpStates.containsKey(SECONDARY)) {
                if (sdpStates.get(SECONDARY).equals(UP)) {
                    tunnelState = OperationalState.DEGRADED;
                }
            }
            if (sdpStates.containsKey(PRIMARY)) {
                if (sdpStates.get(PRIMARY).equals(UP)) {
                    tunnelState = OperationalState.UP;
                }
            }
            tunnelMap.get(key).setState(tunnelState);

            response.getTunnels().add(tunnelMap.get(key));
        }

        // next, go device by device and populate SAP results
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
    }

    private RequestData getRequestData(NsoLiveStatusRequest request, Connection conn) throws NsoReadException {
        log.info("Request:" + request.toString());

        String connectionId = request.getConnectionId();
        if (connectionId == null) {
            log.info("REST request has no connection id!");
            throw new IllegalArgumentException();
        }

        HashSet<String> devicesFromId = new HashSet<String>();
        List<String> devicesFromRest = request.getDeviceIds();
        List<String> devices = new ArrayList<String>();

        NsoAdapter.OscarsNsoState nsoState = nsoAdapter.fetchNsoState(false);
        Pair<NsoVPLS, List<NsoLSP>> nsoServices = nsoState.getServiceMap().get(connectionId);
        if (nsoServices == null) {
            log.info("Couldn't find NSO config for " + connectionId);
            throw new NoSuchElementException();
        }

        NsoVPLS nsoVpls = nsoServices.getFirst();
        Integer vcid = nsoVpls.getVcId();

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
        for (String device : devicesFromRest) {
            if (devicesFromId.contains(device)) {
                devices.add(device);
            }
        }

        return RequestData.builder()
                .devices(devices)
                .serviceId(vcid)
                .build();
    }

}
