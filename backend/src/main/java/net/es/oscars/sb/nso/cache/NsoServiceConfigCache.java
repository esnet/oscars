package net.es.oscars.sb.nso.cache;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.sb.nso.NsoProxy;
import net.es.oscars.sb.nso.dto.*;
import net.es.topo.common.dto.nso.FromNsoDeviceList;
import net.es.topo.common.dto.nso.FromNsoServiceConfig;
import net.es.topo.common.dto.nso.enums.NsoService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@Slf4j
public class NsoServiceConfigCache {

    private final NsoProxy nsoProxy;
    public static final String VPLS = "vpls";
    public static final String LSP = "lsp";

    public static final String PORT = "port";
    public static final String SYSTEM = "system";
    public static final String BBL = "bbl";
    public static final String DEVICE_LIST = "device-list";

    public NsoServiceConfigCache(NsoProxy nsoProxy) {
        this.nsoProxy = nsoProxy;
    }

    @CacheEvict(value = "service-config", key = "#cacheKey")
    public void evictSingleValue(String cacheKey) {}

    @CacheEvict(value = "service-config")
    public void evictAllServiceConfigs() {
    }

    @Cacheable(value = "service-config", key = VPLS)
    public NsoVplsResponse getVpls() {
        FromNsoServiceConfig serviceConfig = nsoProxy.getNsoServiceConfig(NsoService.VPLS);
        return new JsonMapper().readValue(serviceConfig.getConfig(), NsoVplsResponse.class);
    }


    @Cacheable(value = "service-config", key = LSP)
    public NsoLspResponse getLsps() {
        FromNsoServiceConfig serviceConfig = nsoProxy.getNsoServiceConfig(NsoService.LSP);
        return new JsonMapper().readValue(serviceConfig.getConfig(), NsoLspResponse.class);
    }


    @Cacheable(value = "service-config", key = PORT)
    public NsoPortResponse getPort() {
        FromNsoServiceConfig serviceConfig = nsoProxy.getNsoServiceConfig(NsoService.PORT);
        return new JsonMapper().readValue(serviceConfig.getConfig(), NsoPortResponse.class);
    }


    @Cacheable(value = "service-config", key = SYSTEM)
    public NsoSystemResponse getSystem() {
        FromNsoServiceConfig serviceConfig = nsoProxy.getNsoServiceConfig(NsoService.SYSTEM);
        return new JsonMapper().readValue(serviceConfig.getConfig(), NsoSystemResponse.class);
    }


    @Cacheable(value = "service-config", key = BBL)
    public NsoBBLResponse getBBL() {
        FromNsoServiceConfig serviceConfig = nsoProxy.getNsoServiceConfig(NsoService.BBL);
        return new JsonMapper().readValue(serviceConfig.getConfig(), NsoBBLResponse.class);
    }

    @Cacheable(value = "service-config", key = DEVICE_LIST)
    public FromNsoDeviceList getDeviceList() {
        return nsoProxy.getNsoDeviceList();
    }
}
