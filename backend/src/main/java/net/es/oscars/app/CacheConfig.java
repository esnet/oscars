package net.es.oscars.app;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCache liveStatusCache = buildCache("live-status", 1);
        CaffeineCache serviceConfigCache = buildCache("service-config", 5);
        CaffeineCache esdbDataCache = buildCache("esdb-data", 5);
        CaffeineCache topologyCache = buildCache("topology", 5);
        CaffeineCache connectionListCache = buildCache("connection-list", 5);
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.asList(liveStatusCache, serviceConfigCache, connectionListCache, esdbDataCache, topologyCache));
        return manager;
    }

    private CaffeineCache buildCache(String name, int minutesToExpire) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(minutesToExpire, TimeUnit.MINUTES)
                .build());
    }

}
