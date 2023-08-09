package net.es.oscars.soap;

import gen.nsi_2_0.connection.types.*;
import lombok.extern.slf4j.Slf4j;
import gen.nsi_2_0.connection.ifce.Error;
import gen.nsi_2_0.connection.ifce.ServiceException;
import gen.nsi_2_0.connection.provider.ConnectionProviderPort;
import gen.nsi_2_0.framework.headers.CommonHeaderType;
import net.es.oscars.app.exc.NsiException;
import net.es.oscars.nsi.beans.NsiErrors;
import net.es.oscars.nsi.ent.NsiMapping;
import net.es.oscars.nsi.svc.NsiService;
import net.es.oscars.nsi.svc.NsiStateEngine;
import net.es.oscars.resv.svc.ConnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.ws.Holder;
import java.util.UUID;

@Slf4j
@Component
public class NsiProvider implements ConnectionProviderPort {

    private NsiService nsiService;
    private NsiStateEngine stateEngine;

    @Autowired
    private ConnService connSvc;

    @Autowired
    public NsiProvider(NsiService nsiService, NsiStateEngine stateEngine) {
        this.nsiService = nsiService;
        this.stateEngine = stateEngine;
    }

    @Override
    public GenericAcknowledgmentType provision(GenericRequestType parameters, Holder<CommonHeaderType> header) throws ServiceException {
        try {
            nsiService.processHeader(header.value);
            NsiMapping mapping = nsiService.getMapping(parameters.getConnectionId());
            nsiService.provision(header.value, mapping);
        } catch (NsiException ex) {
            log.error(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage());
        }

        nsiService.makeResponseHeader(header.value);
        return new GenericAcknowledgmentType();
    }

    @Override
    public GenericAcknowledgmentType reserveCommit(GenericRequestType parameters, Holder<CommonHeaderType> header) throws ServiceException {
        try {
            nsiService.processHeader(header.value);
            NsiMapping mapping = nsiService.getMapping(parameters.getConnectionId());
            nsiService.commit(header.value, mapping);
        } catch (NsiException ex) {
            log.error(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage());
        }

        nsiService.makeResponseHeader(header.value);
        return new GenericAcknowledgmentType();
    }

    @Override
    public GenericAcknowledgmentType terminate(GenericRequestType parameters, Holder<CommonHeaderType> header) throws ServiceException {
        try {
            nsiService.processHeader(header.value);
            NsiMapping mapping = nsiService.getMapping(parameters.getConnectionId());
            nsiService.terminate(header.value, mapping);
        } catch (NsiException ex) {
            log.error(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage());
        }


        nsiService.makeResponseHeader(header.value);
        return new GenericAcknowledgmentType();
    }

    @Override
    public ReserveResponseType reserve(ReserveType reserve, Holder<CommonHeaderType> header) throws ServiceException {
        try {
            nsiService.processHeader(header.value);
        } catch (NsiException ex) {
            log.error(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage());
        }
        if (reserve.getConnectionId() == null) {
            String nsiConnectionId = UUID.randomUUID().toString();
            reserve.setConnectionId(nsiConnectionId);
        }
        NsiMapping mapping = stateEngine.newMapping(
                reserve.getConnectionId(),
                reserve.getGlobalReservationId(),
                header.value.getRequesterNSA(),
                reserve.getCriteria().getVersion()
        );

        log.info("triggering async reserve");
        nsiService.reserve(header.value, reserve, mapping);
        log.info("returning reserve ack");

        ReserveResponseType rrt = new ReserveResponseType();
        rrt.setConnectionId(mapping.getNsiConnectionId());

        nsiService.makeResponseHeader(header.value);
        return rrt;
    }

    /*
    try {
        String pretty = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(reserve);
        log.debug(pretty);
         pretty = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(header);
        log.debug(pretty);

    } catch (JsonProcessingException ex) {
        ex.printStackTrace();
    }
    */

    @Override
    public GenericAcknowledgmentType release(GenericRequestType parameters, Holder<CommonHeaderType> header) throws ServiceException {
        try {
            nsiService.processHeader(header.value);
            NsiMapping mapping = nsiService.getMapping(parameters.getConnectionId());
            nsiService.release(header.value, mapping);
        } catch (NsiException ex) {
            log.error(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage());
        }

        nsiService.makeResponseHeader(header.value);
        return new GenericAcknowledgmentType();
    }

    @Override
    public GenericAcknowledgmentType reserveAbort(GenericRequestType parameters, Holder<CommonHeaderType> header) throws ServiceException {
        try {
            nsiService.processHeader(header.value);
            NsiMapping mapping = nsiService.getMapping(parameters.getConnectionId());
            nsiService.abort(header.value, mapping);
        } catch (NsiException ex) {
            log.error(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage());
        }

        nsiService.makeResponseHeader(header.value);
        return new GenericAcknowledgmentType();
    }

    /* queries */


    @Override
    public QuerySummaryConfirmedType querySummarySync(QueryType querySummary,
                                                      Holder<CommonHeaderType> header) throws Error {
        try {
            nsiService.processHeader(header.value);
        } catch (NsiException ex) {
            log.error(ex.getMessage(), ex);
            throw new Error(ex.getMessage());
        }

        log.info("starting sync query");
        try {
            QuerySummaryConfirmedType qsct = nsiService.querySummary(querySummary);
            nsiService.makeResponseHeader(header.value);
            return qsct;
        } catch (NsiException ex) {
            log.error(ex.getMessage(), ex);
            throw new Error(ex.getMessage());
        }

    }

    @Override
    public GenericAcknowledgmentType querySummary(QueryType querySummary,
                                                  Holder<CommonHeaderType> header) throws ServiceException {
        try {
            nsiService.processHeader(header.value);
        } catch (NsiException ex) {
            log.error(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage());
        }
        log.info("starting async query");
        nsiService.queryAsync(header.value, querySummary);


        nsiService.makeResponseHeader(header.value);
        return new GenericAcknowledgmentType();
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
    public GenericAcknowledgmentType queryRecursive(QueryType queryRecursive,
                                                    Holder<CommonHeaderType> header) throws ServiceException {
        try {
            nsiService.processHeader(header.value);
        } catch (NsiException ex) {
            log.error(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage());
        }
        log.info("starting recursive query");
        nsiService.queryRecursive(header.value, queryRecursive);

        nsiService.makeResponseHeader(header.value);
        return new GenericAcknowledgmentType();
    }

    @Override
    public QueryNotificationConfirmedType queryNotificationSync(QueryNotificationType queryNotificationSync,
                                                                Holder<CommonHeaderType> header) throws Error {
        throw new Error(NsiErrors.UNIMPLEMENTED + " - not implemented");

    }

}
