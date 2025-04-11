package net.es.oscars.soap;

import jakarta.xml.ws.Holder;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.*;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.Error;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.provider.ConnectionProviderPort;
import net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType;
import net.es.oscars.app.exc.NsiInternalException;
import net.es.oscars.app.exc.NsiMappingException;
import net.es.oscars.app.exc.NsiValidationException;
import net.es.oscars.nsi.beans.NsiErrors;
import net.es.oscars.nsi.ent.NsiMapping;
import net.es.oscars.nsi.svc.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class NsiProvider implements ConnectionProviderPort {

    private final NsiMappingService nsiMappingService;
    private final NsiService nsiService;
    private final NsiHeaderUtils nsiHeaderUtils;
    private final NsiQueries nsiQueries;


    @Autowired
    public NsiProvider(NsiService nsiService, NsiMappingService nsiMappingService, NsiHeaderUtils nsiHeaderUtils, NsiQueries nsiQueries) {
        this.nsiService = nsiService;
        this.nsiMappingService = nsiMappingService;
        this.nsiHeaderUtils = nsiHeaderUtils;
        this.nsiQueries = nsiQueries;
    }

    @Override
    public GenericAcknowledgmentType provision(GenericRequestType parameters, Holder<CommonHeaderType> header) throws ServiceException {
        try {
            nsiHeaderUtils.processHeader(header.value);
            NsiMapping mapping = nsiMappingService.getMapping(parameters.getConnectionId());
            nsiService.provision(header.value, mapping);
            nsiHeaderUtils.makeResponseHeader(header.value);
            return new GenericAcknowledgmentType();

        } catch (NsiMappingException | NsiValidationException | NsiInternalException ex) {
            log.error(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage());
        }

    }

    @Override
    public GenericAcknowledgmentType reserveCommit(GenericRequestType parameters, Holder<CommonHeaderType> header) throws ServiceException {
        try {
            nsiHeaderUtils.processHeader(header.value);
            NsiMapping mapping = nsiMappingService.getMapping(parameters.getConnectionId());
            nsiService.commit(header.value, mapping);
            nsiHeaderUtils.makeResponseHeader(header.value);
            return new GenericAcknowledgmentType();
        } catch (NsiValidationException | NsiInternalException | NsiMappingException ex) {
            log.error(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage());
        }
    }

    @Override
    public GenericAcknowledgmentType terminate(GenericRequestType parameters, Holder<CommonHeaderType> header) throws ServiceException {
        try {
            nsiHeaderUtils.processHeader(header.value);
            NsiMapping mapping = nsiMappingService.getMapping(parameters.getConnectionId());
            nsiService.terminate(header.value, mapping);
            nsiHeaderUtils.makeResponseHeader(header.value);
            return new GenericAcknowledgmentType();

        } catch (NsiMappingException | NsiValidationException | NsiInternalException ex) {
            log.error(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage());
        }
    }

    @Override
    public ReserveResponseType reserve(ReserveType reserve, Holder<CommonHeaderType> header) throws ServiceException {
        try {
            ReserveResponseType rrt = new ReserveResponseType();
            nsiHeaderUtils.processHeader(header.value);

            if (reserve.getConnectionId() == null) {
                String nsiConnectionId = UUID.randomUUID().toString();
                reserve.setConnectionId(nsiConnectionId);
            }
            NsiMapping mapping;
            if (nsiMappingService.hasNsiMapping(reserve.getConnectionId())) {
                mapping = nsiMappingService.getMapping(reserve.getConnectionId());

                log.info("found existing mapping, triggering a modify");
                // pass in the new version
                nsiService.modify(header.value, reserve, mapping, reserve.getCriteria().getVersion());
            } else {
                mapping = nsiMappingService.newMapping(
                        reserve.getConnectionId(),
                        reserve.getGlobalReservationId(),
                        header.value.getRequesterNSA(),
                        reserve.getCriteria().getVersion()
                );

                log.info("no existing mapping, triggering a reserve");
                nsiService.reserve(header.value, reserve, mapping);
            }

            log.info("returning reserve ack");
            rrt.setConnectionId(mapping.getNsiConnectionId());
            nsiHeaderUtils.makeResponseHeader(header.value);
            return rrt;

        } catch (NsiMappingException | NsiValidationException | NsiInternalException ex) {
            log.error(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage());
        }
    }

    @Override
    public GenericAcknowledgmentType release(GenericRequestType parameters, Holder<CommonHeaderType> header) throws ServiceException {
        try {
            nsiHeaderUtils.processHeader(header.value);
            NsiMapping mapping = nsiMappingService.getMapping(parameters.getConnectionId());
            nsiService.release(header.value, mapping);
            nsiHeaderUtils.makeResponseHeader(header.value);
            return new GenericAcknowledgmentType();

        } catch (NsiMappingException | NsiValidationException | NsiInternalException ex) {
            log.error(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage());
        }

    }

    @Override
    public GenericAcknowledgmentType reserveAbort(GenericRequestType parameters, Holder<CommonHeaderType> header) throws ServiceException {
        try {
            nsiHeaderUtils.processHeader(header.value);
            NsiMapping mapping = nsiMappingService.getMapping(parameters.getConnectionId());
            nsiService.abort(header.value, mapping);
            nsiHeaderUtils.makeResponseHeader(header.value);
            return new GenericAcknowledgmentType();

        } catch (NsiValidationException | NsiMappingException | NsiInternalException e) {
            throw new ServiceException(e.getMessage());
        }

    }

    /* queries */
    @Override
    public QuerySummaryConfirmedType querySummarySync(QueryType querySummary,
                                                      Holder<CommonHeaderType> header) throws Error {
        try {
            nsiHeaderUtils.processHeader(header.value);
            log.info("starting sync query");
            QuerySummaryConfirmedType qsct = nsiQueries.querySummary(querySummary);
            nsiHeaderUtils.makeResponseHeader(header.value);
            return qsct;

        } catch (NsiValidationException | NsiInternalException ex) {
            log.error(ex.getMessage(), ex);
            throw new Error(ex.getMessage());
        }
    }

    @Override
    public GenericAcknowledgmentType querySummary(QueryType querySummary,
                                                  Holder<CommonHeaderType> header) throws ServiceException {
        try {
            nsiHeaderUtils.processHeader(header.value);
            log.info("starting async query");
            nsiService.queryAsync(header.value, querySummary);


            nsiHeaderUtils.makeResponseHeader(header.value);
            return new GenericAcknowledgmentType();

        } catch (NsiValidationException | NsiInternalException ex) {
            log.error(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage());
        }
    }

    @Override
    public GenericAcknowledgmentType queryRecursive(QueryType queryRecursive,
                                                    Holder<CommonHeaderType> header) throws ServiceException {
        try {
            nsiHeaderUtils.processHeader(header.value);
            log.info("starting recursive query");
            nsiService.queryRecursive(header.value, queryRecursive);

            nsiHeaderUtils.makeResponseHeader(header.value);
            return new GenericAcknowledgmentType();

        } catch (NsiValidationException | NsiInternalException ex) {
            log.error(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage());
        }
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
