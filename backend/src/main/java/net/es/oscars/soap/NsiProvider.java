package net.es.oscars.soap;

import jakarta.xml.ws.Holder;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.*;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.Error;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.provider.ConnectionProviderPort;
import net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType;
import net.es.nsi.lib.soap.gen.nsi_2_0.framework.types.ServiceExceptionType;
import net.es.oscars.app.exc.NsiException;
import net.es.oscars.nsi.beans.NsiErrors;
import net.es.oscars.nsi.svc.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Component
public class NsiProvider implements ConnectionProviderPort {
    private final NsiAsyncQueue queue;
    private final NsiMappingService nsiMappingService;
    private final NsiHeaderUtils nsiHeaderUtils;
    private final NsiQueries nsiQueries;
    private final NsiReserve nsiReserve;

    public NsiProvider(NsiAsyncQueue queue, NsiMappingService nsiMappingService, NsiHeaderUtils nsiHeaderUtils, NsiQueries nsiQueries, NsiReserve nsiReserve) {
        this.queue = queue;
        this.nsiMappingService = nsiMappingService;
        this.nsiHeaderUtils = nsiHeaderUtils;
        this.nsiQueries = nsiQueries;
        this.nsiReserve = nsiReserve;
    }

/* ================================== RESERVE SECTION ==================================
   Only one function here, validates and puts item in the queue
*/

    @Override
    public ReserveResponseType reserve(ReserveType reserve, Holder<CommonHeaderType> header) throws ServiceException {

        ReserveResponseType rrt = new ReserveResponseType();
        // add our item to the work queue
        if (reserve.getConnectionId() == null) {
            String nsiConnectionId = UUID.randomUUID().toString();
            reserve.setConnectionId(nsiConnectionId);
        }

        try {
            // We validate the reserve
            nsiReserve.validateReserve(reserve);
            // then we process the header
            nsiHeaderUtils.processHeader(header.value);
        } catch (NsiException e) {
            String errMsg = e.getMessage();
            ServiceExceptionType sExcTpe = nsiHeaderUtils.makeSvcExcpType(e.getMessage(), e.getError(), new ArrayList<>(), reserve.getConnectionId());
            throw new ServiceException(errMsg, sExcTpe);
        }

        // add our item to the work queue
        NsiAsyncQueue.Reserve asyncItem = NsiAsyncQueue.Reserve.builder()
                .header(header.value)
                .reserve(reserve)
                .build();
        queue.add(asyncItem);

        nsiHeaderUtils.makeResponseHeader(header.value);
        return rrt;
    }

    /* ================================== QUERY SECTION ==================================
    provision(), release(), reserveCommit(), reserveAbort(), terminate() all pass to asyncGeneric()
    asyncGeneric() does validation and adds to queue
    */

    @Override
    public GenericAcknowledgmentType provision(GenericRequestType parameters, Holder<CommonHeaderType> header) throws ServiceException {
        this.asyncGeneric(parameters, header, NsiAsyncQueue.GenericOperation.PROVISION);
        return new GenericAcknowledgmentType();
    }

    @Override
    public GenericAcknowledgmentType release(GenericRequestType parameters, Holder<CommonHeaderType> header) throws ServiceException {
        this.asyncGeneric(parameters, header, NsiAsyncQueue.GenericOperation.RELEASE);
        return new GenericAcknowledgmentType();
    }

    @Override
    public GenericAcknowledgmentType reserveCommit(GenericRequestType parameters, Holder<CommonHeaderType> header) throws ServiceException {
        this.asyncGeneric(parameters, header, NsiAsyncQueue.GenericOperation.RESV_COMMIT);
        return new GenericAcknowledgmentType();
    }

    @Override
    public GenericAcknowledgmentType reserveAbort(GenericRequestType parameters, Holder<CommonHeaderType> header) throws ServiceException {
        this.asyncGeneric(parameters, header, NsiAsyncQueue.GenericOperation.RESV_ABORT);
        return new GenericAcknowledgmentType();
    }

    @Override
    public GenericAcknowledgmentType terminate(GenericRequestType parameters, Holder<CommonHeaderType> header) throws ServiceException {
        this.asyncGeneric(parameters, header, NsiAsyncQueue.GenericOperation.TERMINATE);
        return new GenericAcknowledgmentType();
    }


    // handles multiple async generic operations
    private void asyncGeneric(GenericRequestType parameters, Holder<CommonHeaderType> header, NsiAsyncQueue.GenericOperation operation)
            throws ServiceException {
        // first we check if we got a connectionId
        String nsiConnectionId = parameters.getConnectionId();
        if (nsiConnectionId == null || nsiConnectionId.isEmpty()) {
            String errMsg = "missing connection ID";
            ServiceExceptionType sExcTpe = nsiHeaderUtils.makeSvcExcpType(errMsg, NsiErrors.MISSING_PARAM_ERROR, new ArrayList<>(), nsiConnectionId);
            throw new ServiceException(errMsg, sExcTpe);
        }

        try {
            // then we check if it matches an NSI mapping; all our generic operations need to match
            nsiMappingService.getMapping(nsiConnectionId);
            // then we process the header
            nsiHeaderUtils.processHeader(header.value);
        } catch (NsiException e) {
            String errMsg = e.getMessage();
            ServiceExceptionType sExcTpe = nsiHeaderUtils.makeSvcExcpType(e.getMessage(), e.getError(), new ArrayList<>(), nsiConnectionId);
            throw new ServiceException(errMsg, sExcTpe);
        }

        // finally we add our item to the work queue
        NsiAsyncQueue.Generic asyncItem = NsiAsyncQueue.Generic.builder()
                .header(header.value)
                .operation(operation)
                .nsiConnectionId(parameters.getConnectionId())
                .build();
        queue.add(asyncItem);
    }


    /* ================================== QUERY SECTION ==================================
    querySummarySync() immediately runs the query,
    querySummary() and queryRecursive() pass parameters to asyncQuery()
    asyncQuery() validates and adds to queue
    */
    @Override
    public QuerySummaryConfirmedType querySummarySync(QueryType query,
                                                      Holder<CommonHeaderType> header) throws Error {
        try {
            nsiQueries.validateQuery(query);
            nsiHeaderUtils.processHeader(header.value);
            log.info("starting sync QuerySummary");
            QuerySummaryConfirmedType qsct = nsiQueries.querySummary(query);
            nsiHeaderUtils.makeResponseHeader(header.value);
            return qsct;

        } catch (NsiException ex) {
            log.error(ex.getMessage(), ex);
            throw new Error(ex.getMessage(), ex);
        }
    }

    @Override
    public GenericAcknowledgmentType querySummary(QueryType query, Holder<CommonHeaderType> header) throws ServiceException {
        this.asyncQuery(query, header);
        return new GenericAcknowledgmentType();
    }


    // handles async query operations
    private void asyncQuery(QueryType query, Holder<CommonHeaderType> header)
            throws ServiceException {

        try {
            // We validate the query
            nsiQueries.validateQuery(query);
            // then we process the header
            nsiHeaderUtils.processHeader(header.value);
        } catch (NsiException e) {
            String errMsg = e.getMessage();
            ServiceExceptionType sExcTpe = nsiHeaderUtils.makeSvcExcpType(e.getMessage(), e.getError(), new ArrayList<>(), "");
            throw new ServiceException(errMsg, sExcTpe);
        }

        // finally we add our item to the work queue
        NsiAsyncQueue.Query asyncItem = NsiAsyncQueue.Query.builder()
                .header(header.value)
                .query(query)
                .build();
        queue.add(asyncItem);
    }


    /* ================================== UNIMPLEMENTED SECTION ================================== */
    @Override
    public GenericAcknowledgmentType queryRecursive(QueryType query, Holder<CommonHeaderType> header) throws ServiceException {
        // do we even ever use recursive query?
        throw new ServiceException(NsiErrors.UNIMPLEMENTED + " - not implemented");
    }

    @Override
    public GenericAcknowledgmentType queryNotification(QueryNotificationType queryNotification,
                                                       Holder<CommonHeaderType> header) throws ServiceException {
        throw new ServiceException(NsiErrors.UNIMPLEMENTED + " - not implemented");
    }

    @Override
    public QueryResultConfirmedType queryResultSync(QueryResultType queryResultSync,
                                                    Holder<CommonHeaderType> header) throws Error {
        throw new Error(NsiErrors.UNIMPLEMENTED + " - not implemented");
    }

    @Override
    public GenericAcknowledgmentType queryResult(QueryResultType queryResult,
                                                 Holder<CommonHeaderType> header) throws ServiceException {
        throw new ServiceException(NsiErrors.UNIMPLEMENTED + " - not implemented");
    }


    @Override
    public QueryNotificationConfirmedType queryNotificationSync(QueryNotificationType queryNotificationSync,
                                                                Holder<CommonHeaderType> header) throws Error {
        throw new Error(NsiErrors.UNIMPLEMENTED + " - not implemented");

    }

}
