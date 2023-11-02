package net.es.nsi.lib.soap.gen.nsi_2_0.connection.provider;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.bind.annotation.XmlSeeAlso;

/**
 * This class was generated by Apache CXF 4.0.3
 * 2023-10-30T12:56:21.810-07:00
 * Generated source version: 4.0.3
 *
 */
@WebService(targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/provider", name = "ConnectionProviderPort")
@XmlSeeAlso({net.es.nsi.lib.soap.gen.nsi_2_0.framework.types.ObjectFactory.class, net.es.nsi.lib.soap.gen.saml.assertion.ObjectFactory.class, net.es.nsi.lib.soap.gen.xmlenc.ObjectFactory.class, net.es.nsi.lib.soap.gen.xmldsig.ObjectFactory.class, net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ObjectFactory.class, net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.ObjectFactory.class})
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface ConnectionProviderPort {

    /**
     * The provision message is sent from a Requester NSA to a Provider
     * NSA when an existing reservation is to be transitioned into a
     * provisioned state. The provisionACK indicates that the Provider
     * NSA has accepted the provision request for processing. A
     * provisionConfirmed message will be sent asynchronously to the
     * Requester NSA when provision processing has completed.  There is
     * no associated Failed message for this operation.
     *             
     */
    @WebMethod(action = "http://schemas.ogf.org/nsi/2013/12/connection/service/provision")
    @WebResult(name = "acknowledgment", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", partName = "acknowledgment")
    public net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericAcknowledgmentType provision(

        @WebParam(partName = "provision", name = "provision", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
        net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericRequestType provision,
        @WebParam(partName = "header", mode = WebParam.Mode.INOUT, name = "nsiHeader", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/framework/headers", header = true)
        jakarta.xml.ws.Holder<net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType> header
    ) throws net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException;

    /**
     * The querySummarySync message can be sent from a Requester NSA
     * to determine the status of existing reservations on the Provider
     * NSA. The querySummarySync is a synchronous operation that will
     * block until the results of the query operation have been
     * collected.  These results will be returned in the SOAP
     * response.
     *             
     */
    @WebMethod(action = "http://schemas.ogf.org/nsi/2013/12/connection/service/querySummarySync")
    @WebResult(name = "querySummarySyncConfirmed", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", partName = "querySummarySyncConfirmed")
    public net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QuerySummaryConfirmedType querySummarySync(

        @WebParam(partName = "querySummarySync", name = "querySummarySync", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
        net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QueryType querySummarySync,
        @WebParam(partName = "header", mode = WebParam.Mode.INOUT, name = "nsiHeader", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/framework/headers", header = true)
        jakarta.xml.ws.Holder<net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType> header
    ) throws net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.Error;

    /**
     * The queryRecursive message can be sent from either a Provider or
     * Requester NSA to determine the status of existing reservations.
     * The queryRecursiveACK indicates that the target NSA has accepted
     * the queryRecursive request for processing. A queryRecursiveConfirmed
     * or queryRecursiveFailed message will be sent asynchronously to the
     * requesting NSA when query processing has completed.
     *             
     */
    @WebMethod(action = "http://schemas.ogf.org/nsi/2013/12/connection/service/queryRecursive")
    @WebResult(name = "acknowledgment", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", partName = "acknowledgment")
    public net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericAcknowledgmentType queryRecursive(

        @WebParam(partName = "queryRecursive", name = "queryRecursive", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
        net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QueryType queryRecursive,
        @WebParam(partName = "header", mode = WebParam.Mode.INOUT, name = "nsiHeader", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/framework/headers", header = true)
        jakarta.xml.ws.Holder<net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType> header
    ) throws net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException;

    /**
     * The reserveCommit message is sent from a Requester NSA to a
     * Provider NSA when a reservation or modification to an existing
     * reservation is being committed. The reserveCommitACK indicates
     * that the Provider NSA has accepted the modify request for
     * processing. A reserveCommitConfirmed or reserveCommitFailed message
     * will be sent asynchronously to the Requester NSA when reserve
     * or modify processing has completed.
     *             
     */
    @WebMethod(action = "http://schemas.ogf.org/nsi/2013/12/connection/service/reserveCommit")
    @WebResult(name = "acknowledgment", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", partName = "acknowledgment")
    public net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericAcknowledgmentType reserveCommit(

        @WebParam(partName = "reserveCommit", name = "reserveCommit", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
        net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericRequestType reserveCommit,
        @WebParam(partName = "header", mode = WebParam.Mode.INOUT, name = "nsiHeader", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/framework/headers", header = true)
        jakarta.xml.ws.Holder<net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType> header
    ) throws net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException;

    /**
     * The queryNotification message is sent from a Requester NSA
     * to a Provider NSA to retrieve notifications against an existing
     * reservation residing on the Provider NSA. QueryNotification is an
     * asynchronous operation that will return results of the operation
     * to the Requester NSA's SOAP endpoint specified in the NSI header
     * replyTo field.
     *             
     */
    @WebMethod(action = "http://schemas.ogf.org/nsi/2013/12/connection/service/queryNotification")
    @WebResult(name = "acknowledgment", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", partName = "acknowledgment")
    public net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericAcknowledgmentType queryNotification(

        @WebParam(partName = "queryNotification", name = "queryNotification", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
        net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QueryNotificationType queryNotification,
        @WebParam(partName = "header", mode = WebParam.Mode.INOUT, name = "nsiHeader", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/framework/headers", header = true)
        jakarta.xml.ws.Holder<net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType> header
    ) throws net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException;

    /**
     * The terminate message is sent from a Requester NSA to a Provider
     * NSA when an existing reservation is to be terminated. The
     * terminateACK indicates that the Provider NSA has accepted the
     * terminate request for processing. A terminateConfirmed or
     * terminateFailed message will be sent asynchronously to the Requester
     * NSA when terminate processing has completed.
     *             
     */
    @WebMethod(action = "http://schemas.ogf.org/nsi/2013/12/connection/service/terminate")
    @WebResult(name = "acknowledgment", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", partName = "acknowledgment")
    public net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericAcknowledgmentType terminate(

        @WebParam(partName = "parameters", name = "terminate", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
        net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericRequestType parameters,
        @WebParam(partName = "header", mode = WebParam.Mode.INOUT, name = "nsiHeader", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/framework/headers", header = true)
        jakarta.xml.ws.Holder<net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType> header
    ) throws net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException;

    /**
     * The reserve message is sent from a Requester NSA to a Provider
     * NSA when a new reservation is being requested, or a modification
     * to an existing reservation is required. The reserveResponse
     * indicates that the Provider NSA has accepted the reservation
     * request for processing and has assigned it the returned
     * connectionId. A reserveConfirmed or reserveFailed message will
     * be sent asynchronously to the Requester NSA when reserve 
     * operation has completed processing.
     *             
     */
    @WebMethod(action = "http://schemas.ogf.org/nsi/2013/12/connection/service/reserve")
    @WebResult(name = "reserveResponse", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", partName = "reserveResponse")
    public net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ReserveResponseType reserve(

        @WebParam(partName = "reserve", name = "reserve", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
        net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ReserveType reserve,
        @WebParam(partName = "header", mode = WebParam.Mode.INOUT, name = "nsiHeader", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/framework/headers", header = true)
        jakarta.xml.ws.Holder<net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType> header
    ) throws net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException;

    /**
     * The queryResultSync message can be sent from a Requester NSA
     * to a Provider NASA to retrieve operation results against an
     * existing reservation on the Provider NSA. The queryResultSync
     * is a synchronous operation that will block until the results
     * of the query operation have been collected.  These results
     * will be returned in the SOAP response.
     *             
     */
    @WebMethod(action = "http://schemas.ogf.org/nsi/2013/12/connection/service/queryResultSync")
    @WebResult(name = "queryResultSyncConfirmed", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", partName = "queryResultSyncConfirmed")
    public net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QueryResultConfirmedType queryResultSync(

        @WebParam(partName = "queryResultSync", name = "queryResultSync", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
        net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QueryResultType queryResultSync,
        @WebParam(partName = "header", mode = WebParam.Mode.INOUT, name = "nsiHeader", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/framework/headers", header = true)
        jakarta.xml.ws.Holder<net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType> header
    ) throws net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.Error;

    /**
     * The release message is sent from a Requester NSA to a Provider
     * NSA when an existing reservation is to be transitioned into a
     * released state. The releaseACK indicates that the Provider NSA
     * has accepted the release request for processing. A
     * releaseConfirmed message will be sent asynchronously to the
     * Requester NSA when release processing has completed.  There is
     * no associated Failed message for this operation.
     *             
     */
    @WebMethod(action = "http://schemas.ogf.org/nsi/2013/12/connection/service/release")
    @WebResult(name = "acknowledgment", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", partName = "acknowledgment")
    public net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericAcknowledgmentType release(

        @WebParam(partName = "release", name = "release", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
        net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericRequestType release,
        @WebParam(partName = "header", mode = WebParam.Mode.INOUT, name = "nsiHeader", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/framework/headers", header = true)
        jakarta.xml.ws.Holder<net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType> header
    ) throws net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException;

    /**
     * The reserveAbort message is sent from a Requester NSA to a
     * Provider NSA when a cancellation to an existing reserve or
     * modify operation is being requested. The reserveAbortACK
     * indicates that the Provider NSA has accepted the reserveAbort
     * request for processing. A reserveAbortConfirmed or
     * reserveAbortFailed message will be sent asynchronously to the
     * Requester NSA when reserveAbort processing has completed.
     *             
     */
    @WebMethod(action = "http://schemas.ogf.org/nsi/2013/12/connection/service/reserveAbort")
    @WebResult(name = "acknowledgment", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", partName = "acknowledgment")
    public net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericAcknowledgmentType reserveAbort(

        @WebParam(partName = "reserveAbort", name = "reserveAbort", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
        net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericRequestType reserveAbort,
        @WebParam(partName = "header", mode = WebParam.Mode.INOUT, name = "nsiHeader", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/framework/headers", header = true)
        jakarta.xml.ws.Holder<net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType> header
    ) throws net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException;

    /**
     * The querySummary message is sent from a Requester NSA to a
     * Provider NSA to determine the status of existing reservations.
     * The querySummaryACK indicates that the target NSA has
     * accepted the querySummary request for processing. A
     * querySummaryConfirmed or querySummaryFailed message will be
     * sent asynchronously to the requesting NSA when querySummary
     * processing has completed.
     *             
     */
    @WebMethod(action = "http://schemas.ogf.org/nsi/2013/12/connection/service/querySummary")
    @WebResult(name = "acknowledgment", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", partName = "acknowledgment")
    public net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericAcknowledgmentType querySummary(

        @WebParam(partName = "querySummary", name = "querySummary", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
        net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QueryType querySummary,
        @WebParam(partName = "header", mode = WebParam.Mode.INOUT, name = "nsiHeader", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/framework/headers", header = true)
        jakarta.xml.ws.Holder<net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType> header
    ) throws net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException;

    /**
     * The queryResult message is sent from a Requester NSA to a Provider
     * NSA to retrieve operation results against an existing reservation
     * residing on the Provider NSA. QueryResult is an asynchronous
     * operation that will return results of the operation to the Requester
     * NSA's SOAP endpoint specified in the NSI header replyTo field.
     *             
     */
    @WebMethod(action = "http://schemas.ogf.org/nsi/2013/12/connection/service/queryResult")
    @WebResult(name = "acknowledgment", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", partName = "acknowledgment")
    public net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericAcknowledgmentType queryResult(

        @WebParam(partName = "queryResult", name = "queryResult", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
        net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QueryResultType queryResult,
        @WebParam(partName = "header", mode = WebParam.Mode.INOUT, name = "nsiHeader", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/framework/headers", header = true)
        jakarta.xml.ws.Holder<net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType> header
    ) throws net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException;

    /**
     * The queryNotificationSync message can be sent from a Requester NSA
     * to notifications against an existing reservations on the Provider
     * NSA. The queryNotificationSync is a synchronous operation that
     * will block until the results of the query operation have been
     * collected.  These results will be returned in the SOAP response.
     *             
     */
    @WebMethod(action = "http://schemas.ogf.org/nsi/2013/12/connection/service/queryNotificationSync")
    @WebResult(name = "queryNotificationSyncConfirmed", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", partName = "queryNotificationSyncConfirmed")
    public net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QueryNotificationConfirmedType queryNotificationSync(

        @WebParam(partName = "queryNotificationSync", name = "queryNotificationSync", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
        net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QueryNotificationType queryNotificationSync,
        @WebParam(partName = "header", mode = WebParam.Mode.INOUT, name = "nsiHeader", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/framework/headers", header = true)
        jakarta.xml.ws.Holder<net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType> header
    ) throws net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.Error;
}