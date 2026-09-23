package net.es.oscars.sb.nso.cache;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.NsoProperties;
import net.es.oscars.sb.nso.NsoProxy;
import net.es.oscars.sb.nso.rest.LiveStatusMockData;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NsoLiveStatusCache {
    private final NsoProxy nsoProxy;
    private final NsoProperties props;

    public NsoLiveStatusCache(NsoProxy nsoProxy, NsoProperties props) {
        this.nsoProxy = nsoProxy;
        this.props = props;
    }



    @Cacheable(value = "live-status", key = "#root.methodName+'-'+#device+'-'+#serviceId")
    public String getLiveStatusServiceMacs(String device, int serviceId) {
        String args = "service id " + serviceId + " fdb detail";
        log.info("getLiveStatusServiceMacs " + args);
        if (props.isMockLiveShowCommands()) {
            return "This is mock data for 'show " + args + "'";
        }
        return nsoProxy.getLiveStatusShowArgs(device, args);
    }

    // SDP
    @Cacheable(value = "live-status", key = "#root.methodName+'-'+#device+'-'+#serviceId")
    public String getLiveStatusServiceSdp(String device, int serviceId) {
        String args = "service id " + serviceId + " sdp";
        if (props.isMockLiveShowCommands()) {
            return LiveStatusMockData.SDP_MOCK_DATA;
        }
        return nsoProxy.getLiveStatusShowArgs(device, args);
    }

    // SAP
    @Cacheable(value = "live-status", key = "#root.methodName+'-'+#device+'-'+#serviceId")
    public String getLiveStatusServiceSap(String device, int serviceId) {
        String args = "service id " + serviceId + " sap";
        if (props.isMockLiveShowCommands()) {
            return LiveStatusMockData.SAP_MOCK_DATA;
        }
        return nsoProxy.getLiveStatusShowArgs(device, args);
    }

    // LSP
    @Cacheable(value = "live-status", key = "#root.methodName+'-'+#device")
    public String getLiveStatusRouterMplsLsp(String device) {
        String args = "router mpls lsp";
        if (props.isMockLiveShowCommands()) {
            return LiveStatusMockData.LSP_MOCK_DATA;
        }
        return nsoProxy.getLiveStatusShowArgs(device, args);
    }
}
