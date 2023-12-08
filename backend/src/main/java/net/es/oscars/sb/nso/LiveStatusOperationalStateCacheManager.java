package net.es.oscars.sb.nso;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
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
    public ArrayList<LiveStatusSdpResult> refreshSdp(String device, int serviceId) {
        log.info("Refresh SDP info for " + device + "service ID " + serviceId);

        Instant now = Instant.now();
        ArrayList<LiveStatusSdpResult> resultList = new ArrayList<>();
        LiveStatusSdpResult result = new LiveStatusSdpResult();

        // create local key and query device
        DeviceServiceIdKeyPair key = new DeviceServiceIdKeyPair(device, serviceId);
        //String reply = nsoProxy.getLiveStatusServiceSdp(device, serviceId);

        // mock value
        String mockReply = "\n\r\n===============================================================================\r\nServices: Service Destination Points\r\n===============================================================================\r\nSdpId            Type     Far End addr    Adm     Opr       I.Lbl     E.Lbl\r\n-------------------------------------------------------------------------------\r\n7002:7005        Spok     134.55.200.174  Up      Up        524108    524262\r\n7003:7008        Spok     134.55.200.174  Up      Down      524101    None\r\n-------------------------------------------------------------------------------\r\nNumber of SDPs : 2\r\n-------------------------------------------------------------------------------\r\n===============================================================================\r\nA:star-cr6# ";
        String reply = mockReply;


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

        if (!checkLiveStatusReply(reply)) {
            result = (LiveStatusSdpResult) createErrorResult(reply, now);
            resultList.add(result);
            return resultList;
        }

        // extract lines with SDPs
        ArrayList<String> sdpsList = this.getDataLines(reply);

        // extract SDP data - simple line format parsing
        for (int i = 0; i < sdpsList.size(); i++) {
            result = new LiveStatusSdpResult();
            result.setDevice(device);
            result.setTimestamp(now);

            String line = sdpsList.get(i);
            log.info("LINE :" + line);
            String[] sdpIdAndInfo = line.split(":");

            if (sdpIdAndInfo.length != 2) {
                log.error("SDP data parsing error - line format error: SDP ID and info");
                break;
            }

            int sdpId = 0;
            try {
                sdpId = Integer.parseInt(sdpIdAndInfo[0]);
            } catch (NumberFormatException error) {
                log.error("Couldn't parse SDP ID");
                error.printStackTrace();
            }
            result.setSdpId(sdpId);

            // remaining part example -> "7005        Spok     134.55.200.174  Up      Up        524108    524262"
            String singleWhitespace = sdpIdAndInfo[1].replaceAll("\\s{2,}", " ");
            String[] sdpInfo = singleWhitespace.split(" ");

            int remainingElements = 7;
            if (sdpInfo.length != remainingElements) {
                log.error("SDP data parsing error - line format error: data arguments");
                break;
            }

            result.setType(sdpInfo[1]);
            result.setFarEndAddress(sdpInfo[2]);

            result.setAdminState(convertStatus(sdpInfo[3]));
            result.setOperationalState(convertStatus(sdpInfo[4]));

            result.setStatus(true);
            resultList.add(result);
        }

        this.sdpCache.put(key, resultList);
        return resultList;

    }


    // SAP
    public ArrayList<LiveStatusSapResult> refreshSap(String device, int serviceId) {

        log.info("Refresh SAP info for " + device + "service ID " + serviceId);

        Instant now = Instant.now();
        ArrayList<LiveStatusSapResult> resultList = new ArrayList<>();
        LiveStatusSapResult result = new LiveStatusSapResult();

        // create local key and query device
        DeviceServiceIdKeyPair key = new DeviceServiceIdKeyPair(device, serviceId);
        //String reply = nsoProxy.getLiveStatusServiceSap(device, serviceId);

        // mock value
        String mockReply = "\n\r\n===============================================================================\r\nSAP(Summary), Service 7005\r\n===============================================================================\r\nPortId                          SvcId      Ing.  Ing.    Egr.  Egr.   Adm  Opr\r\n                                           QoS   Fltr    QoS   Fltr        \r\n-------------------------------------------------------------------------------\r\n2/1/c5/1:1814                   7005       7001  none    7001  none   Up   Up\r\n-------------------------------------------------------------------------------\r\nNumber of SAPs : 1\r\n-------------------------------------------------------------------------------\r\n===============================================================================\r\nA:star-cr6# ";
        String reply = mockReply;

        // request devices device star-cr6 live-status exec show args service id 7005 sap
        /*
         *  ===============================================================================
            SAP(Summary), Service 7005
            ===============================================================================
            PortId                          SvcId      Ing.  Ing.    Egr.  Egr.   Adm  Opr
                                                       QoS   Fltr    QoS   Fltr
            -------------------------------------------------------------------------------
            2/1/c5/1:1814                   7005       7001  none    7001  none   Up   Up
            -------------------------------------------------------------------------------
            Number of SAPs : 1
            -------------------------------------------------------------------------------
            ===============================================================================
         */

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
            log.info("LINE :" + line);
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
                log.error("SDP data parsing error - line format error: data arguments");
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

            result.setAdminState(convertStatus(sapInfo[6]));
            result.setOperationalState(convertStatus(sapInfo[7]));

            result.setStatus(true);
            resultList.add(result);
        }

        this.sapCache.put(key, resultList);
        return resultList;
    }

    public ArrayList<LiveStatusSapResult> getRefreshedSap(String device, int serviceId) {
        return null;
    }

    public LiveStatusSapResult getCachedSap(String device, int serviceId) {
        return null;
    }

    public LiveStatusSapResult getSap(String device, int serviceId, Instant olderThanTimestamp) {
        return null;
    }

    public ArrayList<LiveStatusLspResult> refreshLsp(String device) {

        /*
            ===============================================================================
            MPLS LSPs (Originating)
            ===============================================================================
            LSP Name                                            Tun     Fastfail  Adm  Opr
            To                                                Id      Config
            -------------------------------------------------------------------------------
            doe-in-vpls_albq-cr6                                1       No        Up   Up
            134.55.200.169
            6999---srs70344a-cr6                                2       No        Up   Up
            134.55.200.230
            6999---pantex-cr6                                   50      No        Up   Up
            134.55.200.216
            JMTF-WRK-anl541b-cr6                                53      No        Up   Up
            134.55.200.174
            JMTF-PRT-anl541b-cr6                                54      No        Up   Up
            134.55.200.174
         */

        log.info("Refresh SAP info for " + device);

        Instant now = Instant.now();
        ArrayList<LiveStatusLspResult> resultList = new ArrayList<>();
        LiveStatusLspResult result = new LiveStatusLspResult();

        // create local key and query device
        //String reply = nsoProxy.getLiveStatusRouterMplsLsp(device);
        String mockReply = "\n\r\n===============================================================================\r\nMPLS LSPs (Originating)\r\n===============================================================================\r\nLSP Name                                            Tun     Fastfail  Adm  Opr\r\n  To                                                Id      Config         \r\n-------------------------------------------------------------------------------\r\ndoe-in-vpls_albq-cr6                                1       No        Up   Up\r\n  134.55.200.169                                                           \r\n6999---srs70344a-cr6                                2       No        Up   Up\r\n  134.55.200.230                                                           \r\n6999---pantex-cr6                                   50      No        Up   Up\r\n  134.55.200.216                                                           \r\nJMTF-WRK-anl541b-cr6                                53      No        Up   Up\r\n  134.55.200.174                                                           \r\nJMTF-PRT-anl541b-cr6                                54      No        Up   Up\r\n  134.55.200.174                                                           \r\n-------------------------------------------------------------------------------\r\nLSPs : 100\r\n===============================================================================\r\nA:star-cr6# ";

        String reply = mockReply;

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
            log.info("LINE :" + line);

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

            if(lspInfo[2].contains("No")) {
                result.setFastFailConf(false);
            } else {
                result.setFastFailConf(true);
            }

            result.setAdminState(convertStatus(lspInfo[3]));
            result.setOperationalState(convertStatus(lspInfo[4]));
            result.setTo(lspInfo[5]);

            result.setStatus(true);
            resultList.add(result);
        }

        this.lspCache.put(device, resultList);
        return resultList;

    }



    @Scheduled(fixedDelayString = "5", timeUnit = TimeUnit.SECONDS)
    public void debug() {

        ArrayList<LiveStatusSdpResult> resultListSdp = refreshSdp("testDevice", 7000);
        for(LiveStatusSdpResult result : resultListSdp) {
            log.info("DEBUG - SDP: " + result.toString());
        }

        ArrayList<LiveStatusSapResult> resultListSap = refreshSap("testDevice", 7000);
        for(LiveStatusSapResult result : resultListSap) {
            log.info("DEBUG - SAP: " + result.toString());
        }

        ArrayList<LiveStatusLspResult> resultListLsp = refreshLsp("testDevice");
        for(LiveStatusLspResult result : resultListLsp) {
            log.info("DEBUG - LSP: " + result.toString());
        }

    } // DEBUG - TEST - DEBUG - TEST


    // auxilery methods

    /**
     * Extracts live status output lines with the actual data from an SDP / SAP query
     * @param input the live status reply
     * @return an array of strings with the data
     */
    public ArrayList<String> getDataLines(String input) {
        if(input == null) return null;
        ArrayList<String> data = new ArrayList<>();
        String[] lines = input.split("\r\n");
        Pattern regex = Pattern.compile("^[0-9]{1}");
        for (String line : lines) {
            Matcher m = regex.matcher(line);
            if (m.find()) {
                data.add(line);
            }
        }
        return data;
    }

    /**
     * Extracts live status output lines with the actual data from an LSP query
     * @param input the live status reply
     * @return an array of strings with the data
     */
    public ArrayList<String> getLspDataLines(String input) {
        if(input == null) return null;
        ArrayList<String> data = new ArrayList<>();
        String[] lines = input.split("\r\n");
        for (int i = 0; i < lines.length-1; i++) {
            String line = lines[i];
            if (line.contains("Up") || line.contains("Down")) {
                String tmp = line + " " + lines[i+1];
                data.add(tmp);
            }
        }
        return data;
    }

    /**
     * Checks for live status error key words
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
     * @param input the error
     * @param instant the time the error occurred
     * @return a live status object containing the error
     */
    public LiveStatusResult createErrorResult(String input, Instant instant) {
        String msg = "Live status request returned null";
        LiveStatusResult ret = new LiveStatusResult();
        ret.setStatus(false);
        ret.setTimestamp(instant);
        if (input != null) {
            msg = input;
        }
        log.info(input);
        ret.setErrorMessage(input);
        return ret;
    }


    /**
     * Converts the live status status values Up / Down into boolean true / false
     * @param status String with the status value
     * @return true if status is Up otherwise false
     */
    public boolean convertStatus(String status) {
        if (status == null) return false;
        if (status.equals("Up")) return true;
        return false;
    }


}
