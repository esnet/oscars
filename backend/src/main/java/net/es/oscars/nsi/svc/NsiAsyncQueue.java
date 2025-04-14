package net.es.oscars.nsi.svc;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QueryType;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ReserveType;
import net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType;
import net.es.oscars.app.exc.NsiException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Component
public class NsiAsyncQueue {

    public ConcurrentLinkedQueue<AsyncItem> queue = new ConcurrentLinkedQueue<>();
    public void add(AsyncItem item) {
        queue.add(item);
    }

    public void processQueue() {
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

            // add failed items back to the queue
            Future<Results> resultsFuture = executorService.submit(pollTask);
            queue.addAll(resultsFuture.get().getFailed());
        } catch (ExecutionException | InterruptedException e) {
            log.error("error while polling queue", e);
            throw new RuntimeException(e);
        }

    }

    public void processGeneric(Generic item) throws NsiException {

    }
    public void processQuery(Query item) throws NsiException {

    }
    public void processReserve(Reserve item) throws NsiException {

    }



    /*
                nsiHeaderUtils.processHeader(header.value);
            NsiMapping mapping = nsiMappingService.getMapping(parameters.getConnectionId());
            nsiService.terminate(header.value, mapping);
            nsiHeaderUtils.makeResponseHeader(header.value);

     */

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
        QueryOperation operation;
    }

    public enum QueryOperation {
        SUMMARY, RECURSIVE;
    }


    public enum GenericOperation {
        PROVISION, RELEASE, RESV_COMMIT, RESV_ABORT, TERMINATE;
    }
}
