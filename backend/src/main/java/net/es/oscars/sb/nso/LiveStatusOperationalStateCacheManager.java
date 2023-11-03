package net.es.oscars.sb.nso;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import net.es.oscars.sb.nso.rest.DeviceServiceIdKeyPair;
import net.es.oscars.sb.nso.rest.MacInfoServiceResult;

@Slf4j
@Component
public class LiveStatusOperationalStateCacheManager {

    final private NsoProxy nsoProxy;
    private Map<DeviceServiceIdKeyPair, MacInfoServiceResult> cache;


    public LiveStatusOperationalStateCacheManager(NsoProxy nsoProxy) {
        this.nsoProxy = nsoProxy;
    }


}
