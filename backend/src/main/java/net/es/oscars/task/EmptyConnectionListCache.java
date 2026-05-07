package net.es.oscars.task;

import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Component
public class EmptyConnectionListCache {

    @Scheduled(fixedDelayString = "${resv.list-cache-max-age}")
    @Transactional
    @CacheEvict(cacheNames="connection_list", allEntries=true)
    public void emptyTheCache() {
        // just there to run the cacheevict annotation
    }

}