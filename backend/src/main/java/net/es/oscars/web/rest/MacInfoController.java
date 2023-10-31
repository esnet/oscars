package net.es.oscars.web.rest;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.web.beans.MacInfoRequest;
import net.es.oscars.web.beans.MacInfoResponse;
import net.es.oscars.sb.nso.rest.MacInfoResult;
import net.es.oscars.sb.nso.rest.MacInfoServiceResult;
import net.es.oscars.sb.nso.LiveStatusFdbCacheManager;
import net.es.oscars.sb.nso.db.NsoSdpIdDAO;
import net.es.oscars.sb.nso.ent.NsoSdpId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
public class MacInfoController {

    @Autowired
    private LiveStatusFdbCacheManager fdbCacheManager;

    @Autowired
    private NsoSdpIdDAO nsoSdpIdDAO;

    @RequestMapping(value = "/api/mac/info", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public MacInfoResponse getMacInfo(@RequestBody MacInfoRequest request) {
        log.debug("Request:" + request.toString());

        String connectionId = request.getConnectionId();
        if(connectionId == null) {
            log.info("MAC info request has no connection id!");
            return null;
        }

        HashMap<String, NsoSdpId> devicesFromId = new HashMap<String, NsoSdpId>();
        List<String> devicesFromRest = request.getDeviceIds();

        // get SAPs from local DB
        List<NsoSdpId> saps = nsoSdpIdDAO.findNsoSdpIdByConnectionId(connectionId);
        for(NsoSdpId sap : saps) {
            devicesFromId.put(sap.getDevice(), sap);
            log.info("SAP: " + sap.getDevice() + " " + sap.getSdpId());
        }

        // if no devices are listed in the request we use all devices from the circuit
        if(devicesFromRest == null) {
            devicesFromRest = new LinkedList<String>();
            devicesFromRest.addAll(devicesFromId.keySet());
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
            if(devicesFromId.containsKey(device)) {
                log.debug("Fetch FDB from FDBCacheManager for " + device);
                tmpResult = fdbCacheManager.get(device,
                                devicesFromId.get(device).getSdpId(),
                                request.getRefreshIfOlderThan());
                tmp = result.getMacInfoResult();
                results.add(tmp);
            }
        }

        response.setResults(results);
        return response;
    }

}
