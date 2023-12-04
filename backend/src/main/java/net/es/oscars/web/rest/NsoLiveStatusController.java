package net.es.oscars.web.rest;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import net.es.oscars.resv.enums.DeploymentState;
import net.es.oscars.resv.enums.State;
import net.es.oscars.web.beans.NsoLiveStatusRequest;
import net.es.oscars.web.beans.MacInfoResponse;
import net.es.oscars.sb.nso.rest.MacInfoResult;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.ent.VlanFixture;
import net.es.oscars.sb.nso.LiveStatusFdbCacheManager;
import net.es.oscars.sb.nso.db.NsoVcIdDAO;
import net.es.oscars.sb.nso.ent.NsoVcId;

import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@RestController
public class NsoLiveStatusController {

    private final LiveStatusFdbCacheManager fdbCacheManager;

    private final NsoVcIdDAO nsoVcIdDAO;

    private final ConnService connSvc;

    public MacInfoController(LiveStatusFdbCacheManager fdbCacheManager, NsoVcIdDAO nsoVcIdDAO, ConnService connSvc) {
        this.fdbCacheManager = fdbCacheManager;
        this.nsoVcIdDAO = nsoVcIdDAO;
        this.connSvc = connSvc;
    }

    @RequestMapping(value = "/api/mac/info", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public MacInfoResponse getMacInfo(@RequestBody NsoLiveStatusRequest request) {
        log.debug("MAC info request");
        log.debug("Request:" + request.toString());

        String connectionId = request.getConnectionId();
        if (connectionId == null) {
            log.info("MAC info request has no connection id!");
            throw new IllegalArgumentException();
        }

        HashSet<String> devicesFromId = new HashSet<String>();
        List<String> devicesFromRest = request.getDeviceIds();

        // get circuit vc-id
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
            devicesFromRest = new LinkedList<>(devicesFromId);
        }

        MacInfoResponse response = new MacInfoResponse();
        // the question is if we move this into the list with the results
        // since entries can have a different timestamp based on the if-older-than criteria
        response.setTimestamp(Instant.now());
        response.setConnectionId(request.getConnectionId()); // cp connId from request

        List<MacInfoResult> results = new LinkedList<>();


        log.debug("Run live-status request on devices");
        for (String device : devicesFromRest) {
            if (devicesFromId.contains(device)) {
                if (conn.getState().equals(State.ACTIVE) &&
                        conn.getDeploymentState().equals(DeploymentState.DEPLOYED)) {
                    log.debug("Fetch FDB from FDBCacheManager for " + device + " service id " + vcid);
                    results.add(fdbCacheManager
                            .get(device, vcid, request.getRefreshIfOlderThan()).getMacInfoResult());

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
        }

        response.setResults(results);
        return response;
    }



    @RequestMapping(value = "/api/operational-state/info", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public MacInfoResponse getOperationalStateInfo(@RequestBody NsoLiveStatusRequest request) {
        return null;
    }


}
