package net.es.oscars.sb.nso;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.sb.nso.rest.DeviceServiceIdKeyPair;
import net.es.oscars.sb.nso.rest.MacInfoServiceResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;


import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@EnableAsync
public class LiveStatusFdbCacheManager {

    final private NsoProxy nsoProxy;
    private Map<DeviceServiceIdKeyPair, MacInfoServiceResult> cache;

    public LiveStatusFdbCacheManager(NsoProxy nsoProxy) {
        this.nsoProxy = nsoProxy;
        this.cache = new ConcurrentHashMap<>();
    }

    public MacInfoServiceResult refresh(String device, int serviceId) {
        log.info("Refresh FDb string for " + device + " service ID " + serviceId);

        MacInfoServiceResult result = new MacInfoServiceResult();
        result.setDevice(device);
        result.setServiceId(serviceId);
        result.setTimestamp(Instant.now());

        DeviceServiceIdKeyPair key = new DeviceServiceIdKeyPair(device, serviceId);

        String reply = nsoProxy.getLiveStatusServiceMacs(device, serviceId);

        if (reply != null) {
            // check for invalid service ID
            // Error message example:
            // - wrong service ID: "\nMINOR: CLI Invalid service id \"7001\".\r\nA:llnl-cr6# "
            String invalidServiceId = "Invalid service id";
            if (reply.contains(invalidServiceId)) {
                String error = invalidServiceId + " " + serviceId;
                log.error(error);
                result.setErrorMessage(error);
            }
            result.setStatus(true);
            result.setFdbQueryResult(reply);

        } else {
            String error = "error refreshing FDB MAC table from " + device + " with service id " + serviceId;
            log.error(error);
            result.setErrorMessage(error);
        }

        cache.put(key, result);
        return result;
    }

    public MacInfoServiceResult getRefreshed(String device, int serviceId) {
        return refresh(device, serviceId);
    }

    public MacInfoServiceResult getCached(String device, int serviceId) {
        return cache.get(new DeviceServiceIdKeyPair(device, serviceId));
    }

    public MacInfoServiceResult get(String device, int serviceId, Instant olderThanTimestamp) {
        if (olderThanTimestamp == null) return null;

        MacInfoServiceResult tmp = getCached(device, serviceId);
        if (tmp == null) {
            // nothing cached
            tmp = getRefreshed(device, serviceId);
            if (tmp == null || tmp.getTimestamp() == null) return null;
            else return tmp;
        }

        if (tmp.getTimestamp().isBefore(olderThanTimestamp)) {
            // if not get a refreshed one
            tmp = getRefreshed(device, serviceId);
        }
        return tmp;
    }

}
