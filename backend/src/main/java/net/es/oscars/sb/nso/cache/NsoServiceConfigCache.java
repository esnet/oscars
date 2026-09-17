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

    @Cacheable(value = "service-config", key = "#root.target.VPLS")
    public NsoVplsResponse getVpls() {
        FromNsoServiceConfig serviceConfig = nsoProxy.getNsoServiceConfig(NsoService.VPLS);
        if (serviceConfig == null || !serviceConfig.getSuccessful()) {
            return NsoVplsResponse.builder().build();
        }
        return new JsonMapper().readValue(serviceConfig.getConfig(), NsoVplsResponse.class);
    }


    @Cacheable(value = "service-config", key = "#root.target.LSP")
    public NsoLspResponse getLsps() {
        FromNsoServiceConfig serviceConfig = nsoProxy.getNsoServiceConfig(NsoService.LSP);
        if (serviceConfig == null || !serviceConfig.getSuccessful()) {
            return NsoLspResponse.builder().build();
        }
        return new JsonMapper().readValue(serviceConfig.getConfig(), NsoLspResponse.class);
    }


    @Cacheable(value = "service-config", key = "#root.target.PORT")
    public NsoPortResponse getPort() {
        FromNsoServiceConfig serviceConfig = nsoProxy.getNsoServiceConfig(NsoService.PORT);
        if (serviceConfig == null || !serviceConfig.getSuccessful()) {
            return NsoPortResponse.builder().build();
        }
        return new JsonMapper().readValue(serviceConfig.getConfig(), NsoPortResponse.class);
    }


    @Cacheable(value = "service-config", key = "#root.target.SYSTEM")
    public NsoSystemResponse getSystem() {
        FromNsoServiceConfig serviceConfig = nsoProxy.getNsoServiceConfig(NsoService.SYSTEM);
        if (serviceConfig == null || !serviceConfig.getSuccessful()) {
            return NsoSystemResponse.builder().build();
        }
        return new JsonMapper().readValue(serviceConfig.getConfig(), NsoSystemResponse.class);
    }


    @Cacheable(value = "service-config", key = "#root.target.BBL")
    public NsoBBLResponse getBBL() {
        FromNsoServiceConfig serviceConfig = nsoProxy.getNsoServiceConfig(NsoService.BBL);
        if (serviceConfig == null || !serviceConfig.getSuccessful()) {
            return NsoBBLResponse.builder().build();
        }
        return new JsonMapper().readValue(serviceConfig.getConfig(), NsoBBLResponse.class);
    }

    @Cacheable(value = "service-config", key = "#root.target.DEVICE_LIST")
    public FromNsoDeviceList getDeviceList() {
        return nsoProxy.getNsoDeviceList();
    }
}
