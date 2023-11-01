package net.es.oscars.web.rest;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.web.beans.MacInfoRequest;
import net.es.oscars.web.beans.MacInfoResponse;
import net.es.oscars.sb.nso.rest.MacInfoResult;
import net.es.oscars.sb.nso.rest.MacInfoServiceResult;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.ent.VlanFixture;
import net.es.oscars.sb.nso.LiveStatusFdbCacheManager;
import net.es.oscars.sb.nso.db.NsoVcIdDAO;
import net.es.oscars.sb.nso.ent.NsoVcId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
public class MacInfoController {

    @Autowired
    private LiveStatusFdbCacheManager fdbCacheManager;

    @Autowired
    private NsoVcIdDAO nsoVcIdDAO;

    @Autowired
    private ConnService connSvc;

    @RequestMapping(value = "/api/mac/info", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public MacInfoResponse getMacInfo(@RequestBody MacInfoRequest request) {
        log.debug("MAC info request");
        log.debug("Request:" + request.toString());

        String connectionId = request.getConnectionId();
        if(connectionId == null) {
            log.info("MAC info request has no connection id!");
            return null;
        }

        HashSet<String> devicesFromId = new HashSet<String>();
        List<String> devicesFromRest = request.getDeviceIds();

        // get circuit vc-id
        Optional<NsoVcId> optVcid = nsoVcIdDAO.findNsoVcIdByConnectionId(connectionId);
        Integer vcid = 0;
        if(optVcid.isPresent()) {
            vcid = optVcid.get().getVcId();
        } else {
            log.info("Couldn't find VC-ID for OSCARS circuit " + connectionId);
            return null;
        }

        // find devices in circuit
        Connection conn = connSvc.findConnection(connectionId);
        if(conn == null) {
            log.info("Couldn't find OSCARS circuit for connection id " + connectionId);
            return null;
        }
        for(VlanFixture f : conn.getReserved().getCmp().getFixtures()) {
            String deviceUrn = f.getJunction().getDeviceUrn();
            devicesFromId.add(deviceUrn);
            log.debug("Adding device: " + deviceUrn);
        }

        // if no devices are listed in the request we use all devices from the circuit
        if(devicesFromRest == null || devicesFromRest.size() == 0) {
            devicesFromRest = new LinkedList<String>();
            devicesFromRest.addAll(devicesFromId);
        }

        MacInfoResponse response = new MacInfoResponse();
        // the question is if we move this into the list with the results
        // since entries can have a different timestamp based on the if-older-than criteria
        response.setTimestamp(Instant.now());
        response.setConnectionId(request.getConnectionId()); // cp connId from request

        List<MacInfoResult> results = new LinkedList<MacInfoResult>();
        MacInfoServiceResult tmpResult;
        MacInfoResult tmp;

        log.debug("Run live-status request on devices");
        for(String device : devicesFromRest) {
            if(devicesFromId.contains(device)) {
                log.debug("Fetch FDB from FDBCacheManager for " + device + " service id " + vcid);
                tmpResult = fdbCacheManager.get(device,
                                vcid,
                                request.getRefreshIfOlderThan());
                tmp = tmpResult.getMacInfoResult();
                results.add(tmp);
            }
        }

        response.setResults(results);
        return response;
    }

}
