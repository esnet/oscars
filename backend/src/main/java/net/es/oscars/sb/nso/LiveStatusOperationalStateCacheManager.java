package net.es.oscars.sb.nso;

import java.time.Instant;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import net.es.oscars.sb.nso.rest.DeviceServiceIdKeyPair;
import net.es.oscars.sb.nso.rest.LiveStatusSapResult;
import net.es.oscars.sb.nso.rest.LiveStatusSdpResult;
import net.es.oscars.sb.nso.rest.LiveStatusLspResult;

import net.es.oscars.sb.nso.rest.MacInfoServiceResult;

@Slf4j
@Component
public class LiveStatusOperationalStateCacheManager {

    final private NsoProxy nsoProxy;
    private Map<DeviceServiceIdKeyPair, MacInfoServiceResult> macCache;

    private Map<DeviceServiceIdKeyPair, LiveStatusSapResult> sapCache;
    private Map<DeviceServiceIdKeyPair, LiveStatusSdpResult> sdpCache;
    private Map<DeviceServiceIdKeyPair, LiveStatusLspResult> lspCache;


    public LiveStatusOperationalStateCacheManager(NsoProxy nsoProxy) {
        this.nsoProxy = nsoProxy;
    }

    // learned MAC address live status cache functions
    public MacInfoServiceResult refreshMacs(String device, int serviceId) {
        log.info("Refresh FDB string for " + device + "service ID " + serviceId);

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

        macCache.put(key, result);
        return result;
    }

    public MacInfoServiceResult getRefreshedMacs(String device, int serviceId) {
        return refreshMacs(device, serviceId);
    }

    public MacInfoServiceResult getCachedMacs(String device, int serviceId) {
        return macCache.get(new DeviceServiceIdKeyPair(device, serviceId));
    }

    public MacInfoServiceResult getMacs(String device, int serviceId, Instant olderThanTimestamp) {
        if (olderThanTimestamp == null) return null;

        MacInfoServiceResult tmp = getCachedMacs(device, serviceId);
        if (tmp == null) {
            // nothing cached
            tmp = getRefreshedMacs(device, serviceId);
            if (tmp == null || tmp.getTimestamp() == null) return null;
            else return tmp;
        }

        if (tmp.getTimestamp().isBefore(olderThanTimestamp)) {
            // if not get a refreshed one
            tmp = getRefreshedMacs(device, serviceId);
        }
        return tmp;
    }

    // SAP live status cache functions
    public LiveStatusSapResult refreshSap(String device, int serviceId) {
        log.info("Refresh SAP string for " + device + "service ID " + serviceId);

        LiveStatusSapResult result = new LiveStatusSapResult();
        result.setDevice(device);
        // result.setServiceId(serviceId);
        result.setTimestamp(Instant.now());

        return null;
    }

    public LiveStatusSapResult getRefreshedSap(String device, int serviceId) {
        return null;
    }

    public LiveStatusSapResult getCachedSap(String device, int serviceId) {
        return null;
    }

    public LiveStatusSapResult getSap(String device, int serviceId, Instant olderThanTimestamp) {
        return null;
    }


}
