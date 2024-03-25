package net.es.oscars.sb.nso;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import net.es.oscars.sb.nso.rest.DeviceServiceIdKeyPair;
import net.es.oscars.sb.nso.rest.LiveStatusSapResult;
import net.es.oscars.sb.nso.rest.LiveStatusSdpResult;
import net.es.oscars.sb.nso.rest.LiveStatusLspResult;
import net.es.oscars.sb.nso.rest.LiveStatusResult;
import net.es.oscars.sb.nso.rest.MacInfoServiceResult;

@Slf4j
@Component
public class LiveStatusOperationalStateCacheManager {

    final private NsoProxy nsoProxy;
    private Map<DeviceServiceIdKeyPair, MacInfoServiceResult> macCache;
    private Map<DeviceServiceIdKeyPair, ArrayList<LiveStatusSapResult>> sapCache;
    private Map<DeviceServiceIdKeyPair, ArrayList<LiveStatusSdpResult>> sdpCache;
    private Map<String, ArrayList<LiveStatusLspResult>> lspCache;


    public LiveStatusOperationalStateCacheManager(NsoProxy nsoProxy) {
        this.nsoProxy = nsoProxy;
        this.macCache = new HashMap<>();
        this.sapCache = new HashMap<>();
        this.sdpCache = new HashMap<>();
        this.lspCache = new HashMap<>();
    }


    public MacInfoServiceResult getMacs(String device, int serviceId, Instant oldestAcceptableTimestamp) {

        if (oldestAcceptableTimestamp == null) return null;

        DeviceServiceIdKeyPair key = new DeviceServiceIdKeyPair(device, serviceId);
        // check if already cached:
        if (macCache.containsKey(key) && macCache.get(key).getTimestamp().isAfter(oldestAcceptableTimestamp)) {
            return macCache.get(key);
        }

        // cache miss:

        log.info("Refresh FDB string for " + device + " service ID " + serviceId);

        MacInfoServiceResult result = new MacInfoServiceResult();
        Instant now = Instant.now();

        result.setDevice(device);
        result.setServiceId(serviceId);
        result.setTimestamp(now);

        String reply = nsoProxy.getLiveStatusServiceMacs(device, serviceId);
        result.setFdbQueryResult(reply);

        if (!checkLiveStatusReply(reply)) {
            log.error("error refreshing FDB MAC table from " + device + " with service id " + serviceId);
            return (MacInfoServiceResult) createErrorResult(reply, now);
        }

        result.setStatus(true);
        macCache.put(key, result);
        return result;

    }

    // SDP live status cache functions
    public ArrayList<LiveStatusSdpResult> refreshSdp(String device, int serviceId) {
        log.info("Refresh SDP info for " + device + " service ID " + serviceId);

        Instant now = Instant.now();
        ArrayList<LiveStatusSdpResult> resultList = new ArrayList<>();
        LiveStatusSdpResult result = new LiveStatusSdpResult();

        // create local key and query device
        DeviceServiceIdKeyPair key = new DeviceServiceIdKeyPair(device, serviceId);
        String reply = nsoProxy.getLiveStatusServiceSdp(device, serviceId);

        if (!checkLiveStatusReply(reply)) {
            result = (LiveStatusSdpResult) createErrorResult(reply, now);
            resultList.add(result);
            return resultList;
        }

        // extract lines with SDPs
        ArrayList<String> sdpsList = this.getDataLines(reply);

        // extract SDP data - simple line format parsing
        // incoming format:
        // 7141:7087        Spok     134.55.200.173  Up      Up        524100    524267
        // sdpId : vcId

        for (int i = 0; i < sdpsList.size(); i++) {
            result = new LiveStatusSdpResult();
            result.setDevice(device);
            result.setTimestamp(now);

            String line = sdpsList.get(i);
            log.debug("LINE :" + line);
            String[] sdpIdAndInfo = line.split(":");

            if (sdpIdAndInfo.length != 2) {
                log.error("SDP data parsing error - line format error: SDP ID and info");
                break;
            }

            // remaining part example -> "7005        Spok     134.55.200.174  Up      Up        524108    524262"
            String singleWhitespace = sdpIdAndInfo[1].replaceAll("\\s{2,}", " ");
            String[] sdpInfo = singleWhitespace.split(" ");

            int remainingElements = 7;
            if (sdpInfo.length != remainingElements) {
                log.error("SDP data parsing error - line format error: data arguments");
                break;
            }

            int sdpId = 0;
            int vcId = 0;
            try {
                sdpId = Integer.parseInt(sdpIdAndInfo[0]);
                vcId = Integer.parseInt(sdpInfo[0]);
            } catch (NumberFormatException error) {
                log.error("Couldn't parse VC/SDP ID from "+line+" : "+error.getMessage());
            }
            result.setVcId(vcId);
            result.setSdpId(sdpId);

            result.setType(sdpInfo[1]);
            result.setFarEndAddress(sdpInfo[2]);

            result.setAdminState(convertStatus(sdpInfo[3]));
            result.setOperationalState(convertStatus(sdpInfo[4]));
            result.setRaw(reply);

            result.setStatus(true);
            resultList.add(result);
        }

        this.sdpCache.put(key, resultList);
        return resultList;

    }

    public ArrayList<LiveStatusSdpResult> getRefreshedSdp(String device, int serviceId) {
        return refreshSdp(device, serviceId);
    }

    public ArrayList<LiveStatusSdpResult> getCachedSdp(String device, int serviceId) {
        return this.sdpCache.get(new DeviceServiceIdKeyPair(device, serviceId));
    }

    public ArrayList<LiveStatusSdpResult> getSdp(String device, int serviceId, Instant olderThanTimestamp) {
        if (olderThanTimestamp == null) return null;

        // get cached SDPs
        ArrayList<LiveStatusSdpResult> tmp = getCachedSdp(device, serviceId);
        if (tmp == null) {
            // nothing cached - get refreshed SDPs
            tmp = getRefreshedSdp(device, serviceId);
            if (tmp == null) return null;
            else return tmp;
        }

        // check all timestamps and update if one is older than olderThanTimestamp
        for (LiveStatusSdpResult element : tmp) {
            if (element.getTimestamp().isBefore(olderThanTimestamp)) {
                // if not get a refreshed one
                tmp = getRefreshedSdp(device, serviceId);
                break;
            }
        }
        return tmp;
    }


    // SAP
    public ArrayList<LiveStatusSapResult> refreshSap(String device, int serviceId) {

        log.info("Refresh SAP info for " + device + " service ID " + serviceId);

        Instant now = Instant.now();
        ArrayList<LiveStatusSapResult> resultList = new ArrayList<>();
        LiveStatusSapResult result = new LiveStatusSapResult();

        // create local key and query device
        DeviceServiceIdKeyPair key = new DeviceServiceIdKeyPair(device, serviceId);
        String reply = nsoProxy.getLiveStatusServiceSap(device, serviceId);

        if (!checkLiveStatusReply(reply)) {
            result = (LiveStatusSapResult) createErrorResult(reply, now);
            resultList.add(result);
            return resultList;
        }

        // extract lines with SAPs
        ArrayList<String> sapsList = this.getDataLines(reply);

        // extract SAP data - simple line format parsing
        for (int i = 0; i < sapsList.size(); i++) {

            result = new LiveStatusSapResult();
            result.setDevice(device);
            result.setTimestamp(now);

            String line = sapsList.get(i);
            log.debug("LINE :" + line);
            String[] sapIdAndInfo = line.split(":");

            if (sapIdAndInfo.length != 2) {
                log.error("SAP data parsing error - line format error: SAP ID and info");
                break;
            }

            result.setPort(sapIdAndInfo[0]);

            String singleWhitespace = sapIdAndInfo[1].replaceAll("\\s{2,}", " ");
            String[] sapInfo = singleWhitespace.split(" ");

            int remainingElements = 8;
            if (sapInfo.length != remainingElements) {
                log.error("SAP data parsing error - line format error: data arguments");
                break;
            }

            int vlanId = 0;
            int ingresQos = 0;
            int egressQos = 0;
            try {
                vlanId = Integer.parseInt(sapInfo[0]);
                ingresQos = Integer.parseInt(sapInfo[0]);
                egressQos = Integer.parseInt(sapInfo[0]);
            } catch (NumberFormatException error) {
                log.error("Couldn't parse SAP VLAN ID or ingress / egress QoS");
                error.printStackTrace();
            }
            result.setVlan(vlanId);
            result.setIngressQos(ingresQos);
            result.setEgressQos(egressQos);
            result.setRaw(reply);

            result.setAdminState(convertStatus(sapInfo[6]));
            result.setOperationalState(convertStatus(sapInfo[7]));

            result.setStatus(true);
            resultList.add(result);
        }

        this.sapCache.put(key, resultList);
        return resultList;
    }

    public ArrayList<LiveStatusSapResult> getRefreshedSap(String device, int serviceId) {
        return refreshSap(device, serviceId);
    }

    public ArrayList<LiveStatusSapResult> getCachedSap(String device, int serviceId) {
        return this.sapCache.get(new DeviceServiceIdKeyPair(device, serviceId));
    }

    public ArrayList<LiveStatusSapResult> getSap(String device, int serviceId, Instant olderThanTimestamp) {
        if (olderThanTimestamp == null) return null;

        // get cached SAPs
        ArrayList<LiveStatusSapResult> tmp = getCachedSap(device, serviceId);
        if (tmp == null) {
            // nothing cached - get refreshed SAPs
            return getRefreshedSap(device, serviceId);
        }

        // check all timestamps and update if one is older than olderThanTimestamp
        for (LiveStatusSapResult element : tmp) {
            if (element.getTimestamp().isBefore(olderThanTimestamp)) {
                // if not get a refreshed one
                tmp = getRefreshedSap(device, serviceId);
                break;
            }
        }
        return tmp;
    }

    // LSP
    public ArrayList<LiveStatusLspResult> refreshLsp(String device) {
        log.info("Refresh LSP info for " + device);

        Instant now = Instant.now();
        ArrayList<LiveStatusLspResult> resultList = new ArrayList<>();
        LiveStatusLspResult result = new LiveStatusLspResult();

        // query device
        String reply = nsoProxy.getLiveStatusRouterMplsLsp(device);

        if (!checkLiveStatusReply(reply)) {
            result = (LiveStatusLspResult) createErrorResult(reply, now);
            resultList.add(result);
            return resultList;
        }

        // extract lines with LSPs
        ArrayList<String> lspsList = this.getLspDataLines(reply);

        // extract LSP data - simple line format parsing
        for (int i = 0; i < lspsList.size(); i++) {

            result = new LiveStatusLspResult();
            result.setDevice(device);
            result.setTimestamp(now);

            String line = lspsList.get(i);
            log.debug("LINE :" + line);

            String singleWhitespace = line.replaceAll("\\s{2,}", " ");
            String[] lspInfo = singleWhitespace.split(" ");

            int elements = 6;
            if (lspInfo.length != elements) {
                log.error("LSP data parsing error - line format error: data arguments");
                break;
            }

            result.setName(lspInfo[0]);

            int tunnelId = 0;
            try {
                tunnelId = Integer.parseInt(lspInfo[1]);
            } catch (NumberFormatException error) {
                log.error("Couldn't parse LSP Tunnel ID");
                error.printStackTrace();
            }
            result.setTunnelId(tunnelId);

            if (lspInfo[2].contains("No")) {
                result.setFastFailConf(false);
            } else {
                result.setFastFailConf(true);
            }

            result.setAdminState(convertStatus(lspInfo[3]));
            result.setOperationalState(convertStatus(lspInfo[4]));
            result.setTo(lspInfo[5]);
            result.setRaw(reply);

            result.setStatus(true);
            resultList.add(result);
        }

        this.lspCache.put(device, resultList);
        return resultList;

    }

    public ArrayList<LiveStatusLspResult> getRefreshedLsp(String device) {
        return refreshLsp(device);
    }

    public ArrayList<LiveStatusLspResult> getCachedLsp(String device) {
        return this.lspCache.get(device);
    }

    public ArrayList<LiveStatusLspResult> getLsp(String device, Instant olderThanTimestamp) {
        if (olderThanTimestamp == null) return null;

        // get cached SAPs
        ArrayList<LiveStatusLspResult> tmp = getCachedLsp(device);
        if (tmp == null) {
            // nothing cached - get refreshed SAPs
            return getRefreshedLsp(device);
        }

        // check all timestamps and update if one is older than olderThanTimestamp
        for (LiveStatusLspResult element : tmp) {
            if (element.getTimestamp().isBefore(olderThanTimestamp)) {
                // if not get a refreshed one
                tmp = getRefreshedLsp(device);
                break;
            }
        }
        return tmp;
    }


    // auxiliary methods

    /**
     * Extracts live status output lines with the actual data from an SDP / SAP query
     *
     * @param input the live status reply
     * @return an array of strings with the data
     */
    public ArrayList<String> getDataLines(String input) {
        if (input == null) return null;
        ArrayList<String> data = new ArrayList<>();
        String[] lines = input.split("\r\n");

        // starts with a number, i.e. for something like
        // 1/1/c13/1:2012                  7072       7001  none    7001  none   Up   Up
        Pattern startsWithNumber = Pattern.compile("^[0-9]{1}");

        // some SAP lines look different and we want those too
        // lag-50:3603                     7072       7005  none    7005  none   Up   Up
        Pattern startsWithLag = Pattern.compile("^lag");

        for (String line : lines) {
            Matcher numberMatch = startsWithNumber.matcher(line);
            Matcher lagMatch = startsWithLag.matcher(line);
            if (numberMatch.find() || lagMatch.find()) {
                data.add(line);
            }
        }
        return data;
    }

    /**
     * Extracts live status output lines with the actual data from an LSP query
     *
     * @param input the live status reply
     * @return an array of strings with the data
     */
    public ArrayList<String> getLspDataLines(String input) {
        if (input == null) return null;
        ArrayList<String> data = new ArrayList<>();
        String[] lines = input.split("\r\n");
        for (int i = 0; i < lines.length - 1; i++) {
            String line = lines[i];
            if (line.contains("Up") || line.contains("Down")) {
                String tmp = line + " " + lines[i + 1];
                data.add(tmp);
            }
        }
        return data;
    }

    /**
     * Checks for live status error key words
     *
     * @param input the live status reply
     * @return true if valid input and false if an error was detected
     */
    public boolean checkLiveStatusReply(String input) {
        // check for error or regular result
        // MINOR: CLI Invalid service id "8000".
        // MINOR: CLI Invalid service "sdp".
        if (input == null) {
            log.error("Live status request returned null");
            return false;
        }
        if (input.contains("MINOR") || input.contains("Invalid")) {
            log.error(input);
            return false;
        }
        return true;
    }

    /**
     * Create a lve status error
     *
     * @param input   the error
     * @param instant the time the error occurred
     * @return a live status object containing the error
     */
    public LiveStatusResult createErrorResult(String input, Instant instant) {
        log.info(input);

        String msg;
        if (input == null) {
            msg = "Live status request returned null";
        } else {
            msg = "Device error: " + input;
        }
        return LiveStatusResult.builder()
                .status(false)
                .timestamp(instant)
                .errorMessage(msg)
                .build();
    }

    /**
     * Converts the live status values Up / Down into boolean true / false
     *
     * @param status String with the status value
     * @return true if status is Up otherwise false
     */
    public boolean convertStatus(String status) {
        if (status == null) return false;
        if (status.equals("Up")) return true;
        return false;
    }

}
