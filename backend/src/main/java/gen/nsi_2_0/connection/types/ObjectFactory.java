
package gen.nsi_2_0.connection.types;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import gen.nsi_2_0.framework.types.ServiceExceptionType;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the net.es.nsi.lib.soap.gen.nsi_2_0.connection.types package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Reserve_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "reserve");
    private final static QName _ReserveResponse_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "reserveResponse");
    private final static QName _ReserveConfirmed_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "reserveConfirmed");
    private final static QName _ReserveFailed_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "reserveFailed");
    private final static QName _ReserveCommit_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "reserveCommit");
    private final static QName _ReserveCommitConfirmed_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "reserveCommitConfirmed");
    private final static QName _ReserveCommitFailed_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "reserveCommitFailed");
    private final static QName _ReserveAbort_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "reserveAbort");
    private final static QName _ReserveAbortConfirmed_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "reserveAbortConfirmed");
    private final static QName _Provision_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "provision");
    private final static QName _ProvisionConfirmed_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "provisionConfirmed");
    private final static QName _Release_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "release");
    private final static QName _ReleaseConfirmed_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "releaseConfirmed");
    private final static QName _Terminate_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "terminate");
    private final static QName _TerminateConfirmed_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "terminateConfirmed");
    private final static QName _QuerySummary_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "querySummary");
    private final static QName _QuerySummaryConfirmed_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "querySummaryConfirmed");
    private final static QName _QueryRecursive_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "queryRecursive");
    private final static QName _QueryRecursiveConfirmed_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "queryRecursiveConfirmed");
    private final static QName _QuerySummarySync_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "querySummarySync");
    private final static QName _QuerySummarySyncConfirmed_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "querySummarySyncConfirmed");
    private final static QName _QueryNotification_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "queryNotification");
    private final static QName _QueryNotificationConfirmed_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "queryNotificationConfirmed");
    private final static QName _QueryNotificationSync_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "queryNotificationSync");
    private final static QName _QueryNotificationSyncConfirmed_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "queryNotificationSyncConfirmed");
    private final static QName _QueryResult_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "queryResult");
    private final static QName _QueryResultConfirmed_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "queryResultConfirmed");
    private final static QName _QueryResultSync_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "queryResultSync");
    private final static QName _QueryResultSyncConfirmed_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "queryResultSyncConfirmed");
    private final static QName _Error_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "error");
    private final static QName _ErrorEvent_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "errorEvent");
    private final static QName _ReserveTimeout_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "reserveTimeout");
    private final static QName _DataPlaneStateChange_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "dataPlaneStateChange");
    private final static QName _MessageDeliveryTimeout_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "messageDeliveryTimeout");
    private final static QName _Acknowledgment_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "acknowledgment");
    private final static QName _ServiceException_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "serviceException");
    private final static QName _ScheduleTypeStartTime_QNAME = new QName("", "startTime");
    private final static QName _ScheduleTypeEndTime_QNAME = new QName("", "endTime");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: net.es.nsi.lib.soap.gen.nsi_2_0.connection.types
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ReserveType }
     * 
     */
    public ReserveType createReserveType() {
        return new ReserveType();
    }

    /**
     * Create an instance of {@link ReserveResponseType }
     * 
     */
    public ReserveResponseType createReserveResponseType() {
        return new ReserveResponseType();
    }

    /**
     * Create an instance of {@link ReserveConfirmedType }
     * 
     */
    public ReserveConfirmedType createReserveConfirmedType() {
        return new ReserveConfirmedType();
    }

    /**
     * Create an instance of {@link GenericFailedType }
     * 
     */
    public GenericFailedType createGenericFailedType() {
        return new GenericFailedType();
    }

    /**
     * Create an instance of {@link GenericRequestType }
     * 
     */
    public GenericRequestType createGenericRequestType() {
        return new GenericRequestType();
    }

    /**
     * Create an instance of {@link GenericConfirmedType }
     * 
     */
    public GenericConfirmedType createGenericConfirmedType() {
        return new GenericConfirmedType();
    }

    /**
     * Create an instance of {@link QueryType }
     * 
     */
    public QueryType createQueryType() {
        return new QueryType();
    }

    /**
     * Create an instance of {@link QuerySummaryConfirmedType }
     * 
     */
    public QuerySummaryConfirmedType createQuerySummaryConfirmedType() {
        return new QuerySummaryConfirmedType();
    }

    /**
     * Create an instance of {@link QueryRecursiveConfirmedType }
     * 
     */
    public QueryRecursiveConfirmedType createQueryRecursiveConfirmedType() {
        return new QueryRecursiveConfirmedType();
    }

    /**
     * Create an instance of {@link QueryNotificationType }
     * 
     */
    public QueryNotificationType createQueryNotificationType() {
        return new QueryNotificationType();
    }

    /**
     * Create an instance of {@link QueryNotificationConfirmedType }
     * 
     */
    public QueryNotificationConfirmedType createQueryNotificationConfirmedType() {
        return new QueryNotificationConfirmedType();
    }

    /**
     * Create an instance of {@link QueryResultType }
     * 
     */
    public QueryResultType createQueryResultType() {
        return new QueryResultType();
    }

    /**
     * Create an instance of {@link QueryResultConfirmedType }
     * 
     */
    public QueryResultConfirmedType createQueryResultConfirmedType() {
        return new QueryResultConfirmedType();
    }

    /**
     * Create an instance of {@link GenericErrorType }
     * 
     */
    public GenericErrorType createGenericErrorType() {
        return new GenericErrorType();
    }

    /**
     * Create an instance of {@link ErrorEventType }
     * 
     */
    public ErrorEventType createErrorEventType() {
        return new ErrorEventType();
    }

    /**
     * Create an instance of {@link ReserveTimeoutRequestType }
     * 
     */
    public ReserveTimeoutRequestType createReserveTimeoutRequestType() {
        return new ReserveTimeoutRequestType();
    }

    /**
     * Create an instance of {@link DataPlaneStateChangeRequestType }
     * 
     */
    public DataPlaneStateChangeRequestType createDataPlaneStateChangeRequestType() {
        return new DataPlaneStateChangeRequestType();
    }

    /**
     * Create an instance of {@link MessageDeliveryTimeoutRequestType }
     * 
     */
    public MessageDeliveryTimeoutRequestType createMessageDeliveryTimeoutRequestType() {
        return new MessageDeliveryTimeoutRequestType();
    }

    /**
     * Create an instance of {@link GenericAcknowledgmentType }
     * 
     */
    public GenericAcknowledgmentType createGenericAcknowledgmentType() {
        return new GenericAcknowledgmentType();
    }

    /**
     * Create an instance of {@link NotificationBaseType }
     * 
     */
    public NotificationBaseType createNotificationBaseType() {
        return new NotificationBaseType();
    }

    /**
     * Create an instance of {@link ReservationRequestCriteriaType }
     * 
     */
    public ReservationRequestCriteriaType createReservationRequestCriteriaType() {
        return new ReservationRequestCriteriaType();
    }

    /**
     * Create an instance of {@link ReservationConfirmCriteriaType }
     * 
     */
    public ReservationConfirmCriteriaType createReservationConfirmCriteriaType() {
        return new ReservationConfirmCriteriaType();
    }

    /**
     * Create an instance of {@link QuerySummaryResultType }
     * 
     */
    public QuerySummaryResultType createQuerySummaryResultType() {
        return new QuerySummaryResultType();
    }

    /**
     * Create an instance of {@link QuerySummaryResultCriteriaType }
     * 
     */
    public QuerySummaryResultCriteriaType createQuerySummaryResultCriteriaType() {
        return new QuerySummaryResultCriteriaType();
    }

    /**
     * Create an instance of {@link ChildSummaryListType }
     * 
     */
    public ChildSummaryListType createChildSummaryListType() {
        return new ChildSummaryListType();
    }

    /**
     * Create an instance of {@link ChildSummaryType }
     * 
     */
    public ChildSummaryType createChildSummaryType() {
        return new ChildSummaryType();
    }

    /**
     * Create an instance of {@link QueryRecursiveResultType }
     * 
     */
    public QueryRecursiveResultType createQueryRecursiveResultType() {
        return new QueryRecursiveResultType();
    }

    /**
     * Create an instance of {@link QueryRecursiveResultCriteriaType }
     * 
     */
    public QueryRecursiveResultCriteriaType createQueryRecursiveResultCriteriaType() {
        return new QueryRecursiveResultCriteriaType();
    }

    /**
     * Create an instance of {@link ChildRecursiveListType }
     * 
     */
    public ChildRecursiveListType createChildRecursiveListType() {
        return new ChildRecursiveListType();
    }

    /**
     * Create an instance of {@link ChildRecursiveType }
     * 
     */
    public ChildRecursiveType createChildRecursiveType() {
        return new ChildRecursiveType();
    }

    /**
     * Create an instance of {@link QueryResultResponseType }
     * 
     */
    public QueryResultResponseType createQueryResultResponseType() {
        return new QueryResultResponseType();
    }

    /**
     * Create an instance of {@link ScheduleType }
     * 
     */
    public ScheduleType createScheduleType() {
        return new ScheduleType();
    }

    /**
     * Create an instance of {@link ConnectionStatesType }
     * 
     */
    public ConnectionStatesType createConnectionStatesType() {
        return new ConnectionStatesType();
    }

    /**
     * Create an instance of {@link DataPlaneStatusType }
     * 
     */
    public DataPlaneStatusType createDataPlaneStatusType() {
        return new DataPlaneStatusType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReserveType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "reserve")
    public JAXBElement<ReserveType> createReserve(ReserveType value) {
        return new JAXBElement<ReserveType>(_Reserve_QNAME, ReserveType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReserveResponseType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "reserveResponse")
    public JAXBElement<ReserveResponseType> createReserveResponse(ReserveResponseType value) {
        return new JAXBElement<ReserveResponseType>(_ReserveResponse_QNAME, ReserveResponseType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReserveConfirmedType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "reserveConfirmed")
    public JAXBElement<ReserveConfirmedType> createReserveConfirmed(ReserveConfirmedType value) {
        return new JAXBElement<ReserveConfirmedType>(_ReserveConfirmed_QNAME, ReserveConfirmedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenericFailedType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "reserveFailed")
    public JAXBElement<GenericFailedType> createReserveFailed(GenericFailedType value) {
        return new JAXBElement<GenericFailedType>(_ReserveFailed_QNAME, GenericFailedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenericRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "reserveCommit")
    public JAXBElement<GenericRequestType> createReserveCommit(GenericRequestType value) {
        return new JAXBElement<GenericRequestType>(_ReserveCommit_QNAME, GenericRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenericConfirmedType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "reserveCommitConfirmed")
    public JAXBElement<GenericConfirmedType> createReserveCommitConfirmed(GenericConfirmedType value) {
        return new JAXBElement<GenericConfirmedType>(_ReserveCommitConfirmed_QNAME, GenericConfirmedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenericFailedType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "reserveCommitFailed")
    public JAXBElement<GenericFailedType> createReserveCommitFailed(GenericFailedType value) {
        return new JAXBElement<GenericFailedType>(_ReserveCommitFailed_QNAME, GenericFailedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenericRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "reserveAbort")
    public JAXBElement<GenericRequestType> createReserveAbort(GenericRequestType value) {
        return new JAXBElement<GenericRequestType>(_ReserveAbort_QNAME, GenericRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenericConfirmedType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "reserveAbortConfirmed")
    public JAXBElement<GenericConfirmedType> createReserveAbortConfirmed(GenericConfirmedType value) {
        return new JAXBElement<GenericConfirmedType>(_ReserveAbortConfirmed_QNAME, GenericConfirmedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenericRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "provision")
    public JAXBElement<GenericRequestType> createProvision(GenericRequestType value) {
        return new JAXBElement<GenericRequestType>(_Provision_QNAME, GenericRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenericConfirmedType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "provisionConfirmed")
    public JAXBElement<GenericConfirmedType> createProvisionConfirmed(GenericConfirmedType value) {
        return new JAXBElement<GenericConfirmedType>(_ProvisionConfirmed_QNAME, GenericConfirmedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenericRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "release")
    public JAXBElement<GenericRequestType> createRelease(GenericRequestType value) {
        return new JAXBElement<GenericRequestType>(_Release_QNAME, GenericRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenericConfirmedType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "releaseConfirmed")
    public JAXBElement<GenericConfirmedType> createReleaseConfirmed(GenericConfirmedType value) {
        return new JAXBElement<GenericConfirmedType>(_ReleaseConfirmed_QNAME, GenericConfirmedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenericRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "terminate")
    public JAXBElement<GenericRequestType> createTerminate(GenericRequestType value) {
        return new JAXBElement<GenericRequestType>(_Terminate_QNAME, GenericRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenericConfirmedType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "terminateConfirmed")
    public JAXBElement<GenericConfirmedType> createTerminateConfirmed(GenericConfirmedType value) {
        return new JAXBElement<GenericConfirmedType>(_TerminateConfirmed_QNAME, GenericConfirmedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QueryType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "querySummary")
    public JAXBElement<QueryType> createQuerySummary(QueryType value) {
        return new JAXBElement<QueryType>(_QuerySummary_QNAME, QueryType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QuerySummaryConfirmedType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "querySummaryConfirmed")
    public JAXBElement<QuerySummaryConfirmedType> createQuerySummaryConfirmed(QuerySummaryConfirmedType value) {
        return new JAXBElement<QuerySummaryConfirmedType>(_QuerySummaryConfirmed_QNAME, QuerySummaryConfirmedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QueryType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "queryRecursive")
    public JAXBElement<QueryType> createQueryRecursive(QueryType value) {
        return new JAXBElement<QueryType>(_QueryRecursive_QNAME, QueryType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QueryRecursiveConfirmedType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "queryRecursiveConfirmed")
    public JAXBElement<QueryRecursiveConfirmedType> createQueryRecursiveConfirmed(QueryRecursiveConfirmedType value) {
        return new JAXBElement<QueryRecursiveConfirmedType>(_QueryRecursiveConfirmed_QNAME, QueryRecursiveConfirmedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QueryType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "querySummarySync")
    public JAXBElement<QueryType> createQuerySummarySync(QueryType value) {
        return new JAXBElement<QueryType>(_QuerySummarySync_QNAME, QueryType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QuerySummaryConfirmedType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "querySummarySyncConfirmed")
    public JAXBElement<QuerySummaryConfirmedType> createQuerySummarySyncConfirmed(QuerySummaryConfirmedType value) {
        return new JAXBElement<QuerySummaryConfirmedType>(_QuerySummarySyncConfirmed_QNAME, QuerySummaryConfirmedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QueryNotificationType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "queryNotification")
    public JAXBElement<QueryNotificationType> createQueryNotification(QueryNotificationType value) {
        return new JAXBElement<QueryNotificationType>(_QueryNotification_QNAME, QueryNotificationType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QueryNotificationConfirmedType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "queryNotificationConfirmed")
    public JAXBElement<QueryNotificationConfirmedType> createQueryNotificationConfirmed(QueryNotificationConfirmedType value) {
        return new JAXBElement<QueryNotificationConfirmedType>(_QueryNotificationConfirmed_QNAME, QueryNotificationConfirmedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QueryNotificationType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "queryNotificationSync")
    public JAXBElement<QueryNotificationType> createQueryNotificationSync(QueryNotificationType value) {
        return new JAXBElement<QueryNotificationType>(_QueryNotificationSync_QNAME, QueryNotificationType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QueryNotificationConfirmedType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "queryNotificationSyncConfirmed")
    public JAXBElement<QueryNotificationConfirmedType> createQueryNotificationSyncConfirmed(QueryNotificationConfirmedType value) {
        return new JAXBElement<QueryNotificationConfirmedType>(_QueryNotificationSyncConfirmed_QNAME, QueryNotificationConfirmedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QueryResultType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "queryResult")
    public JAXBElement<QueryResultType> createQueryResult(QueryResultType value) {
        return new JAXBElement<QueryResultType>(_QueryResult_QNAME, QueryResultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QueryResultConfirmedType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "queryResultConfirmed")
    public JAXBElement<QueryResultConfirmedType> createQueryResultConfirmed(QueryResultConfirmedType value) {
        return new JAXBElement<QueryResultConfirmedType>(_QueryResultConfirmed_QNAME, QueryResultConfirmedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QueryResultType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "queryResultSync")
    public JAXBElement<QueryResultType> createQueryResultSync(QueryResultType value) {
        return new JAXBElement<QueryResultType>(_QueryResultSync_QNAME, QueryResultType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QueryResultConfirmedType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "queryResultSyncConfirmed")
    public JAXBElement<QueryResultConfirmedType> createQueryResultSyncConfirmed(QueryResultConfirmedType value) {
        return new JAXBElement<QueryResultConfirmedType>(_QueryResultSyncConfirmed_QNAME, QueryResultConfirmedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenericErrorType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "error")
    public JAXBElement<GenericErrorType> createError(GenericErrorType value) {
        return new JAXBElement<GenericErrorType>(_Error_QNAME, GenericErrorType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ErrorEventType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "errorEvent")
    public JAXBElement<ErrorEventType> createErrorEvent(ErrorEventType value) {
        return new JAXBElement<ErrorEventType>(_ErrorEvent_QNAME, ErrorEventType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReserveTimeoutRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "reserveTimeout")
    public JAXBElement<ReserveTimeoutRequestType> createReserveTimeout(ReserveTimeoutRequestType value) {
        return new JAXBElement<ReserveTimeoutRequestType>(_ReserveTimeout_QNAME, ReserveTimeoutRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DataPlaneStateChangeRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "dataPlaneStateChange")
    public JAXBElement<DataPlaneStateChangeRequestType> createDataPlaneStateChange(DataPlaneStateChangeRequestType value) {
        return new JAXBElement<DataPlaneStateChangeRequestType>(_DataPlaneStateChange_QNAME, DataPlaneStateChangeRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MessageDeliveryTimeoutRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "messageDeliveryTimeout")
    public JAXBElement<MessageDeliveryTimeoutRequestType> createMessageDeliveryTimeout(MessageDeliveryTimeoutRequestType value) {
        return new JAXBElement<MessageDeliveryTimeoutRequestType>(_MessageDeliveryTimeout_QNAME, MessageDeliveryTimeoutRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GenericAcknowledgmentType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "acknowledgment")
    public JAXBElement<GenericAcknowledgmentType> createAcknowledgment(GenericAcknowledgmentType value) {
        return new JAXBElement<GenericAcknowledgmentType>(_Acknowledgment_QNAME, GenericAcknowledgmentType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ServiceExceptionType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", name = "serviceException")
    public JAXBElement<ServiceExceptionType> createServiceException(ServiceExceptionType value) {
        return new JAXBElement<ServiceExceptionType>(_ServiceException_QNAME, ServiceExceptionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "startTime", scope = ScheduleType.class)
    public JAXBElement<XMLGregorianCalendar> createScheduleTypeStartTime(XMLGregorianCalendar value) {
        return new JAXBElement<XMLGregorianCalendar>(_ScheduleTypeStartTime_QNAME, XMLGregorianCalendar.class, ScheduleType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "endTime", scope = ScheduleType.class)
    public JAXBElement<XMLGregorianCalendar> createScheduleTypeEndTime(XMLGregorianCalendar value) {
        return new JAXBElement<XMLGregorianCalendar>(_ScheduleTypeEndTime_QNAME, XMLGregorianCalendar.class, ScheduleType.class, value);
    }

}
