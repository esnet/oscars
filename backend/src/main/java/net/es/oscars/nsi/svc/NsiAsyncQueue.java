package net.es.oscars.nsi.svc;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QueryType;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ReserveType;
import net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.NsiException;
import net.es.oscars.app.exc.NsiMappingException;
import net.es.oscars.nsi.ent.NsiMapping;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Component
public class NsiAsyncQueue {
    private final NsiService nsiService;
    private final NsiMappingService nsiMappingService;
    private final Startup startup;

    public ConcurrentLinkedQueue<AsyncItem> queue = new ConcurrentLinkedQueue<>();

    public NsiAsyncQueue(NsiService nsiService, NsiMappingService nsiMappingService, Startup startup) {
        this.nsiService = nsiService;
        this.nsiMappingService = nsiMappingService;
        this.startup = startup;
    }

    public void add(AsyncItem item) {
        queue.add(item);
    }

    @Scheduled(fixedDelayString = "${nsi.queue-interval-millisec}")
    public void processQueue() {
        if (startup.isInStartup() || startup.isInShutdown()) {
            // log.info("application in startup or shutdown; skipping queue processing");
            return;
        }

        try (ExecutorService executorService = Executors.newFixedThreadPool(1)) {

            Callable<Results> pollTask = () -> {
                Results results = Results.builder().build();
                while (queue.peek() != null) {
                    AsyncItem item = queue.poll();
                    try {
                        switch (item) {
                            case Generic generic -> this.processGeneric(generic);
                            case Query query -> this.processQuery(query);
                            case Reserve reserve -> this.processReserve(reserve);
                            default -> {}
                        }
                        results.getSucceeded().add(item);
                    } catch (NsiException ex) {
                        results.getFailed().add(item);
                    }
                }
                return results;
            };
        }

    }

    public void processGeneric(Generic item) throws NsiException {
        NsiMapping mapping = nsiMappingService.getMapping(item.getNsiConnectionId());
        switch (item.getOperation()) {
            case RELEASE -> nsiService.release(item.getHeader(), mapping);
            case PROVISION -> nsiService.provision(item.getHeader(), mapping);
            case TERMINATE ->  nsiService.terminate(item.getHeader(), mapping);
            case RESV_ABORT -> nsiService.abort(item.getHeader(), mapping);
            case RESV_COMMIT -> nsiService.commit(item.getHeader(), mapping);
        }

    }
    public void processQuery(Query item) throws NsiException {
        nsiService.queryAsync(item.getHeader(), item.getQuery());

    }
    public void processReserve(Reserve item) throws NsiException {
        NsiMapping mapping = null;
        try {
            mapping = nsiMappingService.getMapping(item.getReserve().getConnectionId());
        } catch (NsiMappingException ex) {
            // if there's no existing mapping, leave it null and it will be created
        }
        nsiService.reserve(item.header, mapping, item.getReserve());
    }



    @Data
    @Builder
    public static class Results {
        @Builder.Default
        List<AsyncItem> succeeded = new ArrayList<>();
        @Builder.Default
        List<AsyncItem> failed = new ArrayList<>();
    }


    @Data
    @SuperBuilder(toBuilder = true)
    public static class AsyncItem {
        CommonHeaderType header;
    }


    @EqualsAndHashCode(callSuper = true)
    @Data
    @SuperBuilder(toBuilder = true)
    public static class Generic extends AsyncItem {
        GenericOperation operation;
        String nsiConnectionId;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @SuperBuilder(toBuilder = true)
    public static class Reserve extends AsyncItem {
        ReserveType reserve;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @SuperBuilder(toBuilder = true)
    public static class Query extends AsyncItem {
        QueryType query;
    }



    public enum GenericOperation {
        PROVISION, RELEASE, RESV_COMMIT, RESV_ABORT, TERMINATE;
    }
}
