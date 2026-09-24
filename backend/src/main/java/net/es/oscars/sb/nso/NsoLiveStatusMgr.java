package net.es.oscars.sb.nso;

import java.time.Instant;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import lombok.extern.slf4j.Slf4j;

import net.es.oscars.sb.nso.cache.NsoLiveStatusCache;
import org.springframework.stereotype.Component;

import net.es.oscars.sb.nso.rest.DeviceServiceIdKeyPair;
import net.es.oscars.sb.nso.rest.LiveStatusSapResult;
import net.es.oscars.sb.nso.rest.LiveStatusSdpResult;
import net.es.oscars.sb.nso.rest.LiveStatusLspResult;
import net.es.oscars.sb.nso.rest.LiveStatusResult;
import net.es.oscars.sb.nso.rest.MacInfoServiceResult;

@Slf4j
@Component
public class NsoLiveStatusMgr {

    final private NsoLiveStatusCache liveStatusCache;

    public NsoLiveStatusMgr(NsoLiveStatusCache liveStatusCache) {
        this.liveStatusCache = liveStatusCache;
    }

    public MacInfoServiceResult getMacs(String device, int serviceId, Instant oldestAcceptableTimestamp) {

        MacInfoServiceResult result = new MacInfoServiceResult();
        Instant now = Instant.now();

        result.setDevice(device);
        result.setServiceId(serviceId);
        result.setTimestamp(now);

        String reply = liveStatusCache.getLiveStatusServiceMacs(device, serviceId);
        result.setFdbQueryResult(reply);

        if (!checkLiveStatusReply(reply)) {
            log.error("error refreshing FDB MAC table from " + device + " with service id " + serviceId);
            return (MacInfoServiceResult) createErrorResult(reply, now);
        }

        result.setStatus(true);
        return result;

    }

    // SDP live status cache functions
    public ArrayList<LiveStatusSdpResult> getSdp(String device, int serviceId, Instant olderThanTimestamp) {
        log.info("Get SDP info for " + device + " service ID " + serviceId);

        Instant now = Instant.now();
        ArrayList<LiveStatusSdpResult> resultList = new ArrayList<>();
        LiveStatusSdpResult result = new LiveStatusSdpResult();

        String reply = liveStatusCache.getLiveStatusServiceSdp(device, serviceId);
        //        log.info("sdp live status:\n"+ reply);

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

        return resultList;

    }

    // SAP
    public ArrayList<LiveStatusSapResult> getSap(String device, int serviceId) {

        log.info("Refresh SAP info for " + device + " service ID " + serviceId);

        Instant now = Instant.now();
        ArrayList<LiveStatusSapResult> resultList = new ArrayList<>();
        LiveStatusSapResult result;

        // create local key and query device
        String reply = liveStatusCache.getLiveStatusServiceSap(device, serviceId);
        // log.info("sap live status:\n"+ reply);

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

            /* this can look like this
===============================================================================
PortId                          SvcId      Ing.  Ing.    Egr.  Egr.   Adm  Opr
                                           QoS   Fltr    QoS   Fltr
-------------------------------------------------------------------------------
1/1/c3/1:3422                   7125       7010  none    7010  none   Up   Up
but, when vlan id = 0 untagged
2/1/c13/2                       7058       7003  none    7003  none   Up   Up
             */

            // split the line by whitespace
            String[] sapInfo = line.split("\\s+");
            int sapInfoElements = 8;
            if (sapInfo.length != sapInfoElements) {
                log.error("SAP data parsing error - line format error: "+sapInfo.length+" data arguments");
                break;
            }

            String port;
            String vlanStr;

            String sapAndVlan = sapInfo[0];
            if (sapAndVlan.contains(":")) {
                String[] sapAndVlanParts =  sapAndVlan.split(":");
                port = sapAndVlanParts[0];
                vlanStr = sapAndVlanParts[1];

            } else {
                // if there is no ":" character in the sap id, that means we have an untagged SAP
                port = sapAndVlan;
                vlanStr = "0";
            }

            result.setPort(port);

            int vlanId = 0;
            int ingresQos = 0;
            int egressQos = 0;
            try {
                vlanId = Integer.parseInt(vlanStr);
                ingresQos = Integer.parseInt(sapInfo[2]);
                egressQos = Integer.parseInt(sapInfo[4]);
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

        return resultList;
    }

    // LSP
    public ArrayList<LiveStatusLspResult> getLsp(String device, Instant olderThanTimestamp) {
        log.info("Refresh LSP info for " + device);

        Instant now = Instant.now();
        ArrayList<LiveStatusLspResult> resultList = new ArrayList<>();
        LiveStatusLspResult result = new LiveStatusLspResult();

        // query device
        String reply = liveStatusCache.getLiveStatusRouterMplsLsp(device);

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
        return resultList;

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
        String[] lines = input.split("\n");

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
        String[] lines = input.split("\n");
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
