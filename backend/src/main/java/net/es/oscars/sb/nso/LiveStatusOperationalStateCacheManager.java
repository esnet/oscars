package net.es.oscars.sb.nso;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


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
                result.setStatus(false);
            } else {
                result.setStatus(true);
                result.setFdbQueryResult(reply);
            }


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

    // SDP live status cache functions
    public LiveStatusSdpResult refreshSdp(String device, int serviceId) {
        log.info("Refresh SAP string for " + device + "service ID " + serviceId);

        Instant now = Instant.now();
        LiveStatusSdpResult result = new LiveStatusSdpResult();


        // create local key and query device
        DeviceServiceIdKeyPair key = new DeviceServiceIdKeyPair(device, serviceId);
        String reply = nsoProxy.getLiveStatusServiceSdp(device, serviceId);

        if (reply != null) {

            // check for erroror or regular result
            // MINOR: CLI Invalid service id "8000".
            // MINOR: CLI Invalid service "sdp".
            if (reply.contains("MINOR") || reply.contains("Invalid")) {
                log.error(reply);
                result.setStatus(false);
                result.setErrorMessage(reply);
            } else {
                /*
                    ===============================================================================
                    Services: Service Destination Points
                    ===============================================================================
                    SdpId            Type     Far End addr    Adm     Opr       I.Lbl     E.Lbl
                    -------------------------------------------------------------------------------
                    7002:7005        Spok     134.55.200.174  Up      Up        524108    524262
                    7003:7008        Spok     134.55.200.174  Up      Down      524101    None
                    -------------------------------------------------------------------------------
                    Number of SDPs : 2
                    -------------------------------------------------------------------------------
                    ===============================================================================
                 */


                // extract lines with SAPs
                ArrayList<String> sdpsList = new ArrayList();
                String[] lines = reply.split("\r\n");
                Pattern regex = Pattern.compile("[0-9]{1}");
                for (String line : lines) {
                    Matcher m = regex.matcher(line);
                    if (m.find()) {
                        sdpsList.add(line);
                    }
                }

                // extract SDP data

                for (int i = 0; i < sdpsList.size(); i++) {
                    result = new LiveStatusSdpResult();
                    result.setDevice(device);
                    // result.setServiceId(serviceId);
                    result.setTimestamp(now);

                    String line = sdpsList.get(i);
                    String[] sdpIdAndInfo = line.split(":");


                    if (sdpIdAndInfo.length != 2) {
                        // error
                    }

                    int sdpId = 0;
                    try {
                        sdpId = Integer.parseInt(sdpIdAndInfo[0]);
                    } catch (NumberFormatException error) {
                        log.info("");
                    }
                    result.setSdpId(sdpId);

                    String[] sdpInfo = sdpIdAndInfo[1].split(" ");

                    log.info("---> DEBUG: " + sdpInfo[1]);

                    // result.setType(sdpInfo[1]);
                    // result.setFarEnd(sdpInfo[2]);

                    // if (sdpInfo[3].equals("up")) {}
                        // result.setAdminState(true);
                    // } else {
                    //   // result.setAdminState(false);
                    // }

                    // if (sdpInfo[4].equals("up")) {}
                        // result.setOperationalState(true);
                    // } else {
                    //   // result.setOperationalState(false);
                    // }


                    result.setStatus(true);

                }


            }



        }

        return null;
    }





    // SAP
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
