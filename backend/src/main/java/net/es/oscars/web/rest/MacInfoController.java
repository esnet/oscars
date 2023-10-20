package net.es.oscars.web.rest;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.web.beans.MacInfoRequest;
import net.es.oscars.web.beans.MacInfoResponse;
import net.es.oscars.sb.nso.rest.MacInfoResult;
import net.es.oscars.sb.nso.LiveStatusFdbCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
public class MacInfoController {

    @Autowired
    private LiveStatusFdbCacheManager fdbCacheManager;

    @RequestMapping(value = "/api/mac/info", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public MacInfoResponse getMacInfo(@RequestBody MacInfoRequest request) {
        log.debug("Request:" + request.toString());

        String connectionId = request.getConnectionId();

        // get DB / connection Manager entry via connectionId
            // get fixtures (VlanFixture) . getDeviceUrn ???

        Set<String> devicesFromId = new HashSet<>();
        List<String> devicesFromRest = request.getDeviceIds();

        // MOCK
        int serviceId = 7000;
        String device1 = "llnl-cr6";
        String device2 = "blub";
        devicesFromId.add(device1);
        devicesFromId.add(device2);

        // List<String> fixturesToCheck = new LinkedList<String>();
        if(devicesFromRest == null) {
            devicesFromRest = new LinkedList<String>();
            devicesFromRest.addAll(devicesFromId);
        }

        MacInfoResponse response = new MacInfoResponse();
        // the question is if we move this into the list with the results
        // since entries can have a different timestamp based on the if-older-than criteria
        response.setTimestamp(Instant.now());
        response.setConnectionId(request.getConnectionId()); // cp connId from request

        List<MacInfoResult> results = new LinkedList<MacInfoResult>();
        MacInfoResult tmp;

        for(String device : devicesFromRest) {
            log.debug("Check Devices");
            if(devicesFromId.contains(device)) {
                log.debug("Fetch FDB from FDBCacheManager for " + device);
                tmp = fdbCacheManager.get(device, serviceId, request.getRefreshIfOlderThan())
                                        .getMacInfoResult();

                results.add(tmp);
            }
        }

        response.setResults(results);
        return response;
    }

}
