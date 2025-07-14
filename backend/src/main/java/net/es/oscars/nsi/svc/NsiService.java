package net.es.oscars.nsi.svc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.ws.Holder;
import jakarta.xml.ws.WebServiceException;
import lombok.Data;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.*;
import net.es.nsi.lib.soap.gen.nsi_2_0.services.point2point.ObjectFactory;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.requester.ConnectionRequesterPort;
import net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType;
import net.es.nsi.lib.soap.gen.nsi_2_0.framework.types.TypeValuePairType;
import net.es.nsi.lib.soap.gen.nsi_2_0.services.point2point.P2PServiceBaseType;
import net.es.nsi.lib.soap.gen.nsi_2_0.services.types.OrderedStpType;
import net.es.oscars.app.exc.*;
import net.es.oscars.model.Interval;
import net.es.oscars.nsi.beans.*;
import net.es.oscars.nsi.ent.NsiConnectionEvent;
import net.es.oscars.nsi.ent.NsiMapping;
import net.es.oscars.nsi.ent.NsiRequesterNSA;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.enums.*;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.sb.SouthboundTaskResult;
import net.es.oscars.sb.nso.resv.NsoResvException;
import net.es.oscars.soap.NsiSoapClientUtil;
import net.es.oscars.web.beans.*;
import net.es.oscars.web.simple.*;
import net.es.topo.common.devel.DevelUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.StringWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ReservationStateEnumType.*;

@Component
@Slf4j
@Data
public class NsiService {

    public NsiQueries nsiQueries;
    @Value("${resv.timeout}")
    private Integer resvTimeout;

    @Value("${nsi.provider-nsa}")
    private String providerNsa;

    final public static String SERVICE_TYPE = "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE";
    final public static String NSI_TYPES = "http://schemas.ogf.org/nsi/2013/12/framework/types";

    public ConnectionRepository connRepo;
    public NsiNotifications nsiNotifications;
    public NsiRequestManager nsiRequestManager;
    public NsiHeaderUtils nsiHeaderUtils;
    public NsiStateEngine nsiStateEngine;
    public ConnService connSvc;
    public NsiMappingService nsiMappingService;
    public NsiSoapClientUtil nsiSoapClientUtil;
    public ObjectMapper jacksonObjectMapper;
    public NsiConnectionEventService nsiConnectionEventService;

    public NsiService(ConnectionRepository connRepo, NsiRequestManager nsiRequestManager, NsiHeaderUtils nsiHeaderUtils,
                      NsiStateEngine nsiStateEngine, ConnService connSvc, NsiMappingService nsiMappingService,
                      NsiSoapClientUtil nsiSoapClientUtil, ObjectMapper jacksonObjectMapper, NsiQueries nsiQueries,
                      NsiNotifications nsiNotifications, NsiConnectionEventService nsiConnectionEventService) {
        this.connRepo = connRepo;
        this.nsiRequestManager = nsiRequestManager;
        this.nsiHeaderUtils = nsiHeaderUtils;
        this.nsiStateEngine = nsiStateEngine;
        this.connSvc = connSvc;
        this.nsiMappingService = nsiMappingService;
        this.nsiSoapClientUtil = nsiSoapClientUtil;
        this.jacksonObjectMapper = jacksonObjectMapper;
        this.nsiQueries = nsiQueries;
        this.nsiNotifications = nsiNotifications;
        this.nsiConnectionEventService = nsiConnectionEventService;
    }

    /* async operations */

    public void reserve(CommonHeaderType header, NsiMapping mapping, ReserveType incomingRT)
            throws NsiInternalException, NsiStateException, NsiMappingException {
        log.info("reserve");
        log.info("starting reserve for {}", incomingRT.getConnectionId());

        String nsaId = header.getRequesterNSA();
        String nsiConnectionId  = incomingRT.getConnectionId();
        String nsiGri = incomingRT.getGlobalReservationId();
        boolean newReservation = false;
        try {
            // no mapping means this is a brand new reserve
            if (mapping == null) {
                newReservation = true;

                // this will throw an NsiMappingException if it fails and do an errCallback
                mapping = nsiMappingService.newMapping(nsiConnectionId, nsiGri, nsaId, incomingRT.getCriteria().getVersion(), false);
                nsiMappingService.save(mapping);
            } else {
                log.info("transitioning NSI state: RESV_CHECK {}", mapping.getNsiConnectionId());
                nsiStateEngine.reserve(NsiEvent.RESV_CHECK, mapping);
                nsiMappingService.save(mapping);
            }

            String errorMessage;
            NsiErrors errorCode;
            List<TypeValuePairType> tvps;
            // @TODO This doesn't actually validate anything yet.
//            NsiReserveResult validationResult = this.validateRT(incomingRT);

//            if (!validationResult.getSuccess()) {
//                log.error("bad validation, sending error callback for{}", mapping.getNsiConnectionId());
//                errorMessage = validationResult.getErrorMessage();
//                errorCode = validationResult.getErrorCode();
//                tvps = validationResult.getTvps();
//            } else {

                log.info("submitting hold for {}", mapping.getNsiConnectionId());
                try {
                    NsiReserveResult holdResult = this.hold(incomingRT, mapping);
                    if (holdResult.getSuccess()) {
                        log.info("successful reserve, updating state for "+ mapping.getNsiConnectionId());
                        nsiStateEngine.reserve(NsiEvent.RESV_CF, mapping);
                        nsiMappingService.save(mapping);
                        Instant timeout = Instant.now().plus(resvTimeout, ChronoUnit.SECONDS);
                        //
                        NsiRequest nsiRequest = NsiRequest.builder()
                                .nsiConnectionId(mapping.getNsiConnectionId())
                                .incoming(incomingRT)
                                .timeout(timeout)
                                .build();
                        nsiRequestManager.addInFlightRequest(nsiRequest);
                        log.info("sending reserveConfirmCallback for {}", mapping.getNsiConnectionId());
                        nsiConnectionEventService.save(NsiConnectionEvent.builder()
                                .type(NsiConnectionEventType.RESERVE_CONFIRM)
                                .timestamp(Instant.now())
                                .nsiConnectionId(nsiConnectionId)
                                .version(incomingRT.getCriteria().getVersion())
                                .build());


                        this.reserveConfirmCallback(mapping, header);
                        return;

                    } else {

                        log.error("unable to hold, sending error callback for {}", mapping.getNsiConnectionId());
                        errorMessage = holdResult.getErrorMessage();
                        errorCode = holdResult.getErrorCode();
                        tvps = holdResult.getTvps();
                    }
                } catch (NsiInternalException | NsiValidationException ex) {

                    log.error("error holding for {}", mapping.getNsiConnectionId(), ex);
                    errorMessage = ex.getMessage();
                    errorCode = ex.getError();
                    tvps = Collections.emptyList();
                }
//            }

            // we only ever reach this bit if we have had an error
            nsiConnectionEventService.save(NsiConnectionEvent.builder()
                    .type(NsiConnectionEventType.RESERVE_FAILED)
                    .timestamp(Instant.now())
                    .message(errorMessage)
                    .nsiConnectionId(nsiConnectionId)
                    .build());

            log.error("error reserving, sending errCallback with RESV_FL");
            nsiStateEngine.reserve(NsiEvent.RESV_FL, mapping);
            nsiMappingService.save(mapping);

            // if this was a brand new reservation, on failure we delete the mapping and forget all about it
            if (newReservation) {
                nsiMappingService.delete(mapping);
            }
            this.errCallback(NsiEvent.RESV_FL, nsaId, nsiConnectionId, mapping,
                    errorMessage, errorCode, tvps,
                    header.getCorrelationId());

        } catch (NsiStateException | NsiMappingException ex) {
            nsiConnectionEventService.save(NsiConnectionEvent.builder()
                    .type(NsiConnectionEventType.RESERVE_FAILED)
                    .timestamp(Instant.now())
                    .message(ex.getMessage())
                    .nsiConnectionId(nsiConnectionId)
                    .build());


            nsiStateEngine.reserve(NsiEvent.RESV_FL, mapping);
            log.error("Internal error: {}", ex.getMessage(), ex);
            this.errCallback(NsiEvent.RESV_FL, nsaId, nsiConnectionId, mapping,
                    "Internal error", NsiErrors.NRM_ERROR, new ArrayList<>(),
                    header.getCorrelationId());
        }
    }

    public void commit(CommonHeaderType header, NsiMapping mapping) {
        log.info("starting commit for {}", mapping.getNsiConnectionId());
        String nsaId = header.getRequesterNSA();
        String nsiConnectionId  = mapping.getNsiConnectionId();

        String errorMessage;
        NsiErrors errorCode;
        NsiRequest nsiRequest = nsiRequestManager.getInFlightRequest(mapping.getNsiConnectionId());

        if (!mapping.getReservationState().equals(RESERVE_HELD)) {
            errorCode = NsiErrors.RESV_ERROR;
            errorMessage = "connection not in RESERVE_HELD state; instead is "+mapping.getReservationState();

        } else if (nsiRequest == null) {
            errorCode = NsiErrors.RESV_ERROR;
            errorMessage = "unable to locate an in-flight reserve to commit";

        } else {
            // start the commit process
            try {
                nsiStateEngine.commit(NsiEvent.COMMIT_START, mapping);
                nsiMappingService.save(mapping);

                Connection c = nsiMappingService.getOscarsConnection(mapping);

                connSvc.commit(c);

                nsiStateEngine.commit(NsiEvent.COMMIT_CF, mapping);
                mapping.setDataplaneVersion(nsiRequest.getIncoming().getCriteria().getVersion());
                nsiMappingService.save(mapping);

                log.info("new dataplane version {}", mapping.getDataplaneVersion());

                nsiConnectionEventService.save(NsiConnectionEvent.builder()
                        .type(NsiConnectionEventType.RESERVE_COMMIT_CONFIRM)
                        .timestamp(Instant.now())
                        .version(mapping.getDataplaneVersion())
                        .nsiConnectionId(nsiConnectionId)
                        .build());

                nsiRequestManager.remove(mapping.getNsiConnectionId());
                this.okCallback(NsiEvent.COMMIT_CF, mapping, header);
                log.info("ending commit");
                return;

            } catch (PCEException | ConnException | NsoResvException ex) {
                log.error("commit failed: {}", ex.getMessage(), ex);
                errorCode = NsiErrors.RESV_ERROR;
                errorMessage = ex.getMessage();
            } catch (NsiStateException | NsiMappingException  ex  ) {
                log.error("commit failed: {}", ex.getMessage(), ex);
                errorCode = NsiErrors.NRM_ERROR;
                errorMessage = ex.getMessage();
            }
        }

        try {
            nsiConnectionEventService.save(NsiConnectionEvent.builder()
                    .type(NsiConnectionEventType.RESERVE_COMMIT_FAILED)
                    .timestamp(Instant.now())
                    .version(mapping.getDataplaneVersion())
                    .nsiConnectionId(nsiConnectionId)
                    .message(errorMessage)
                    .build());

            // remove the failed request
            nsiRequestManager.remove(mapping.getNsiConnectionId());
            log.info("un-holding {}", mapping.getOscarsConnectionId());
            connSvc.unhold(mapping.getOscarsConnectionId());

            nsiStateEngine.commit(NsiEvent.COMMIT_FL, mapping);


            nsiMappingService.save(mapping);
        } catch (NsiStateException ex  ) {
            log.error(ex.getMessage(), ex);
        }

        this.errCallback(NsiEvent.COMMIT_FL, nsaId, nsiConnectionId, mapping,
                errorMessage, errorCode, new ArrayList<>(),
                header.getCorrelationId());

    }

    public void abort(CommonHeaderType header, NsiMapping mapping) {
        log.info("starting abort task for {}", mapping.getNsiConnectionId());

        NsiRequest nsiRequest = nsiRequestManager.getInFlightRequest(mapping.getNsiConnectionId());
        if (nsiRequest != null) {
            if (mapping.getReservationState().equals(RESERVE_HELD) || mapping.getReservationState().equals(RESERVE_FAILED)) {
                try {


                    nsiStateEngine.abort(NsiEvent.ABORT_START, mapping);
                    nsiMappingService.save(mapping);
                    // remove the request
                    nsiRequestManager.remove(mapping.getNsiConnectionId());
                    connSvc.unhold(mapping.getOscarsConnectionId());

                    nsiStateEngine.abort(NsiEvent.ABORT_CF, mapping);
                    nsiMappingService.save(mapping);
                    // TODO: this leaves the mapping around after the abort

                } catch (NsiStateException ex ) {
                    // we should never be catching this; we checked if we are at an acceptable resv state
                }
            }
        }

        nsiConnectionEventService.save(NsiConnectionEvent.builder()
                .type(NsiConnectionEventType.RESERVE_ABORT_CONFIRM)
                .timestamp(Instant.now())
                .version(mapping.getDataplaneVersion())
                .nsiConnectionId(mapping.getNsiConnectionId())
                .build());

        // we must always send back an ABORT_CF
        this.okCallback(NsiEvent.ABORT_CF, mapping, header);
    }

    public void provision(CommonHeaderType header, NsiMapping mapping) {
        log.info("starting provision task for {}", mapping.getNsiConnectionId());

        try {
            Connection c = nsiMappingService.getOscarsConnection(mapping);
            if (!c.getPhase().equals(Phase.RESERVED)) {
                log.error("cannot provision unless RESERVED");
                return;
            }

            nsiStateEngine.provision(NsiEvent.PROV_START, mapping);
            nsiMappingService.save(mapping);

            c.setMode(BuildMode.AUTOMATIC);
            connRepo.save(c);

            nsiStateEngine.provision(NsiEvent.PROV_CF, mapping);
            nsiMappingService.save(mapping);

            nsiConnectionEventService.save(NsiConnectionEvent.builder()
                    .type(NsiConnectionEventType.PROVISION_CONFIRM)
                    .timestamp(Instant.now())
                    .version(mapping.getDataplaneVersion())
                    .nsiConnectionId(mapping.getNsiConnectionId())
                    .build());

            this.okCallback(NsiEvent.PROV_CF, mapping, header);

        } catch (NsiMappingException | NsiStateException e) {
            nsiConnectionEventService.save(NsiConnectionEvent.builder()
                    .type(NsiConnectionEventType.PROVISION_FAILED)
                    .timestamp(Instant.now())
                    .version(mapping.getDataplaneVersion())
                    .nsiConnectionId(mapping.getNsiConnectionId())
                    .build());

            log.error(e.getMessage(), e);
        }

    }

    public void release(CommonHeaderType header, NsiMapping mapping) {
        log.info("starting release task for {}", mapping.getNsiConnectionId());

        try {
            Connection c = nsiMappingService.getOscarsConnection(mapping);
            if (!c.getPhase().equals(Phase.RESERVED)) {
                log.error("cannot release unless RESERVED");
                return;
            }

            nsiStateEngine.release(NsiEvent.REL_START, mapping);
            nsiMappingService.save(mapping);

            c.setMode(BuildMode.MANUAL);
            c.setDeploymentIntent(DeploymentIntent.SHOULD_BE_UNDEPLOYED);

            nsiStateEngine.release(NsiEvent.REL_CF, mapping);
            nsiMappingService.save(mapping);

            nsiConnectionEventService.save(NsiConnectionEvent.builder()
                    .type(NsiConnectionEventType.RELEASE_CONFIRM)
                    .timestamp(Instant.now())
                    .version(mapping.getDataplaneVersion())
                    .nsiConnectionId(mapping.getNsiConnectionId())
                    .build());


            this.okCallback(NsiEvent.REL_CF, mapping, header);

            log.info("completed release");

        } catch (NsiMappingException | NsiStateException ex) {
            nsiConnectionEventService.save(NsiConnectionEvent.builder()
                    .type(NsiConnectionEventType.RELEASE_FAILED)
                    .timestamp(Instant.now())
                    .version(mapping.getDataplaneVersion())
                    .nsiConnectionId(mapping.getNsiConnectionId())
                    .message(ex.getMessage())
                    .build());

            log.error("release internal error", ex);
        }
    }

    public void terminate(CommonHeaderType header, NsiMapping mapping) {
        log.info("starting terminate task for {}", mapping.getNsiConnectionId());

        try {
            // go from CREATED | PASSED_END_TIME | FAILED to TERMINATING
            nsiStateEngine.termStart(mapping);
            nsiMappingService.save(mapping);

            Optional<Connection> c = nsiMappingService.getMaybeOscarsConnection(mapping);
            c.ifPresent(connSvc::release);

            // go from TERMINATING to TERMINATED
            nsiStateEngine.termConfirm(mapping);
            nsiMappingService.save(mapping);

            nsiConnectionEventService.save(NsiConnectionEvent.builder()
                    .type(NsiConnectionEventType.TERMINATE_CONFIRM)
                    .timestamp(Instant.now())
                    .nsiConnectionId(mapping.getNsiConnectionId())
                    .build());


            log.info("completed terminate");
            this.okCallback(NsiEvent.TERM_CF, mapping, header);


        } catch (NsiStateException ex) {
            log.error("failed terminate, internal error");
            log.error(ex.getMessage(), ex);
        }

    }



    // triggered from ConnController.release(), called by the UI when the user
    // presses the "Release" button
    public void forcedEnd(NsiMapping mapping) {
        log.info("starting forcedEnd task for {}", mapping.getNsiConnectionId());
        try {
            // go from CREATED | FAILED directly to TERMINATED
            nsiStateEngine.forcedEnd(mapping);
            nsiMappingService.save(mapping);

            Connection c = nsiMappingService.getOscarsConnection(mapping);
            connSvc.release(c);

        } catch (NsiStateException |NsiMappingException ex) {
            log.error("failed forcedEnd, internal error");
            log.error(ex.getMessage(), ex);
        }
        nsiConnectionEventService.save(NsiConnectionEvent.builder()
                .type(NsiConnectionEventType.FORCED_END)
                .timestamp(Instant.now())
                .nsiConnectionId(mapping.getNsiConnectionId())
                .build());

        this.errorNotify(EventEnumType.FORCED_END, mapping);

    }

    public void queryAsync(CommonHeaderType header, QueryType query) {
        Executors.newCachedThreadPool().submit(() -> {
            try {
                log.info("starting async query task");
                String nsaId = header.getRequesterNSA();
                String corrId = header.getCorrelationId();

                Holder<CommonHeaderType> outHeader = nsiHeaderUtils.makeClientHeader(nsaId, corrId);
                NsiRequesterNSA requesterNSA = nsiHeaderUtils.getRequesterNsa(nsaId);

                ConnectionRequesterPort port = nsiSoapClientUtil.createRequesterClient(requesterNSA);
                QuerySummaryConfirmedType qsct = nsiQueries.querySummary(query, nsiMappingService.getInitialReserveMappings());
                try {
                    port.querySummaryConfirmed(qsct, outHeader);

                } catch (ServiceException | WebServiceException ex) {
                    log.error("could not perform queryAsync callback");
                    log.error(ex.getMessage(), ex);
                }
            } catch (RuntimeException ex) {
                log.error(ex.getMessage(), ex);
            }

            return null;
        });
    }


    public void queryRecursive(CommonHeaderType header, QueryType query) {
        Executors.newCachedThreadPool().submit(() -> {
            try {
                log.info("starting recursive query task");
                String nsaId = header.getRequesterNSA();
                String corrId = header.getCorrelationId();
                Holder<CommonHeaderType> outHeader = nsiHeaderUtils.makeClientHeader(nsaId, corrId);
                NsiRequesterNSA requesterNSA = nsiHeaderUtils.getRequesterNsa(nsaId);

                ConnectionRequesterPort port = nsiSoapClientUtil.createRequesterClient(requesterNSA);
                QueryRecursiveConfirmedType qrct = nsiQueries.queryRecursive(query);
                try {
                    port.queryRecursiveConfirmed(qrct, outHeader);

                } catch (ServiceException | WebServiceException ex) {
                    log.error("could not perform queryRecursive callback");
                    log.error(ex.getMessage(), ex);
                }
            } catch (RuntimeException ex) {
                log.error(ex.getMessage(), ex);
            }

            return null;
        });
    }


    /* triggered events from TransitionStates periodic tasks */

    // we call this when a reserve message timed outm
    public void resvTimedOut(NsiMapping mapping) {
        log.info("resv timeout for {} {}", mapping.getNsiConnectionId(), mapping.getOscarsConnectionId());
        try {
            nsiStateEngine.resvTimedOut(mapping);
            this.nsiMappingService.save(mapping);
        } catch (NsiStateException ex) {
            log.error("Internal error: {}", ex.getMessage(), ex);
        }
        this.reserveTimeoutCallback(mapping);


    }

    // we call this when a L2VPN goes past its end time
    public void pastEndTime(NsiMapping mapping) {
        log.info("past end time for {} {}", mapping.getNsiConnectionId(), mapping.getOscarsConnectionId());
        try {
            nsiStateEngine.pastEndTime(mapping);
            nsiMappingService.save(mapping);
        } catch (NsiStateException ex) {
            log.error("internal error", ex);
        }
    }


    /* outbound SOAP calls  */
    public void errorNotify(EventEnumType event, NsiMapping mapping) {
        try {
            String nsaId = mapping.getNsaId();
            int notificationId = nsiMappingService.nextNotificationId(mapping);

            NsiRequesterNSA requesterNSA = this.nsiHeaderUtils.getRequesterNsa(nsaId);
            boolean performCallback = false;
            if (requesterNSA.getCallbackUrl().isEmpty()) {
                log.info("empty callback url, unable to errorNotify");
            } else {
                performCallback = true;
                log.info("errorNotify, NSA {} url: {}", requesterNSA.getNsaId(), requesterNSA.getCallbackUrl());
            }
            ConnectionRequesterPort port = nsiSoapClientUtil.createRequesterClient(requesterNSA);
            String corrId = nsiHeaderUtils.newCorrelationId();
            Holder<CommonHeaderType> outHeader = nsiHeaderUtils.makeClientHeader(nsaId, corrId);

            ErrorEventType eet = new ErrorEventType();
            eet.setOriginatingConnectionId(mapping.getNsiConnectionId());
            eet.setOriginatingNSA(this.providerNsa);

            eet.setTimeStamp(nsiMappingService.getCalendar(Instant.now()));
            eet.setEvent(event);
            eet.setNotificationId(notificationId);

            String xml = marshalToXml(eet);
            NsiNotification nsiNotification = NsiNotification.builder()
                    .connectionId(mapping.getNsiConnectionId())
                    .notificationId((long) notificationId)
                    .xml(xml)
                    .type(NsiNotificationType.ERROR_EVENT)
                    .build();
            nsiNotifications.save(nsiNotification);

            if (performCallback) {
                port.errorEvent(eet, outHeader);
            }


        } catch (Exception ex) {
            // maybe the notify worked, maybe not; we can't do anything
            log.error(ex.getMessage(), ex);
        }
    }


    public void housekeeping() {
        for (NsiMapping mapping : nsiMappingService.findAll()) {
            if (mapping.getLastModified() == null) {
                nsiMappingService.save(mapping);
            }

            // find mappings in a transitional state and time them out if they end up stale
            Set<ReservationStateEnumType> transitionalStates = Stream
                    .of(RESERVE_ABORTING, RESERVE_CHECKING, RESERVE_COMMITTING)
                    .collect(Collectors.toSet());

            if (transitionalStates.contains(mapping.getReservationState())) {
                if (mapping.getLastModified().isBefore(Instant.now().minus(10, ChronoUnit.MINUTES))) {
                    log.info("timing out a mapping stuck in transitional state {} {}", mapping.getNsiConnectionId(), mapping.getReservationState());
                    // release holds if they have any
                    connSvc.releaseHold(mapping.getOscarsConnectionId());
                    this.resvTimedOut(mapping);
                }
            }
            Optional<Connection> mc = nsiMappingService.getMaybeOscarsConnection(mapping);
            if (mc.isEmpty()) {
                if (mapping.getLastModified().isBefore(Instant.now().minus(10, ChronoUnit.MINUTES))) {
                    log.info("deleting a mapping without OSCARS connection {} {}", mapping.getNsiConnectionId(), mapping.getOscarsConnectionId());
                    nsiMappingService.delete(mapping);
                }
            }

        }
    }

    /* outbound SOAP calls  */
    public void reserveTimeoutCallback(NsiMapping mapping) {
        try {
            String nsaId = mapping.getNsaId();

            int notificationId = nsiMappingService.nextNotificationId(mapping);

            NsiRequesterNSA requesterNSA = this.nsiHeaderUtils.getRequesterNsa(nsaId);
            boolean performCallback = false;
            if (requesterNSA.getCallbackUrl().isEmpty()) {
                log.info("empty callback url, unable to reserveTimeout");
            } else {
                performCallback = true;
                log.info("reserveTimeoutCallback, NSA {} url: {}", requesterNSA.getNsaId(), requesterNSA.getCallbackUrl());
            }
            ConnectionRequesterPort port = nsiSoapClientUtil.createRequesterClient(requesterNSA);
            String corrId = nsiHeaderUtils.newCorrelationId();
            Holder<CommonHeaderType> outHeader = nsiHeaderUtils.makeClientHeader(nsaId, corrId);

            ReserveTimeoutRequestType rrt = new ReserveTimeoutRequestType();

            rrt.setOriginatingConnectionId(mapping.getNsiConnectionId());
            rrt.setConnectionId(mapping.getNsiConnectionId());
            rrt.setOriginatingNSA(this.providerNsa);
            rrt.setTimeStamp(nsiMappingService.getCalendar(Instant.now()));
            rrt.setTimeoutValue(resvTimeout);
            rrt.setNotificationId(notificationId);

            String xml = marshalToXml(rrt);
            NsiNotification nsiNotification = NsiNotification.builder()
                    .connectionId(mapping.getNsiConnectionId())
                    .notificationId((long) notificationId)
                    .xml(xml)
                    .type(NsiNotificationType.RESERVE_TIMEOUT)
                    .build();
            nsiNotifications.save(nsiNotification);

            nsiConnectionEventService.save(NsiConnectionEvent.builder()
                    .type(NsiConnectionEventType.RESERVE_TIMEOUT)
                    .timestamp(Instant.now())
                    .nsiConnectionId(mapping.getNsiConnectionId())
                    .build());

            if (performCallback) {
                port.reserveTimeout(rrt, outHeader);
            }

        } catch (Exception ex) {
            // maybe the callback worked, maybe not; we can't do anything
            log.error(ex.getMessage(), ex);
        }
    }

    public void reserveConfirmCallback(NsiMapping mapping, CommonHeaderType inHeader) throws NsiInternalException, NsiMappingException {
        String nsaId = mapping.getNsaId();
        NsiRequesterNSA requesterNSA = nsiHeaderUtils.getRequesterNsa(nsaId);
        if (requesterNSA.getCallbackUrl().isEmpty()) {
            log.info("empty callback url, unable to reserveConfirmCallback");
            return;
        } else {
            log.info("reserveConfirmCallback, NSA {} url: {}", requesterNSA.getNsaId(), requesterNSA.getCallbackUrl());
        }

        ConnectionRequesterPort port = nsiSoapClientUtil.createRequesterClient(requesterNSA);

        String corrId = inHeader.getCorrelationId();
        Holder<CommonHeaderType> outHeader = nsiHeaderUtils.makeClientHeader(nsaId, corrId);


        Connection c = nsiMappingService.getOscarsConnection(mapping);

        ReserveConfirmedType rct = new ReserveConfirmedType();

        rct.setConnectionId(mapping.getNsiConnectionId());
        rct.setGlobalReservationId(mapping.getNsiGri());
        rct.setDescription(c.getDescription());

        ReservationConfirmCriteriaType rcct = new ReservationConfirmCriteriaType();
        Schedule sch;
        Components cmp;
        if (c.getPhase().equals(Phase.HELD)) {
            sch = c.getHeld().getSchedule();
            cmp = c.getHeld().getCmp();
        } else {
            sch = c.getArchived().getSchedule();
            cmp = c.getArchived().getCmp();
        }
        ScheduleType st = nsiMappingService.oscarsToNsiSchedule(sch);

        rcct.setSchedule(st);
        rct.setCriteria(rcct);
        rcct.setServiceType(SERVICE_TYPE);
        rcct.setVersion(mapping.getDataplaneVersion());

        P2PServiceBaseType p2p = nsiMappingService.makeP2P(cmp, mapping);
        rcct.getAny().add(new ObjectFactory().createP2Ps(p2p));

        try {
            port.reserveConfirmed(rct, outHeader);
        } catch (Exception e) {
            // we don't care if the callback worked or not.
            log.error(e.getMessage(), e);
        }
    }

    public void updateDataplane(SouthboundTaskResult sb) {
        Optional<NsiMapping> optMapping = nsiMappingService.getMappingForOscarsId(sb.getConnectionId());
        if (optMapping.isPresent()) {
            NsiMapping mapping = optMapping.get();
            if (sb.getDeploymentState().equals(DeploymentState.DEPLOYED)) {
                mapping.setDeployedDataplaneVersion(mapping.getDataplaneVersion());
                nsiMappingService.save(mapping);
            }
            dataplaneCallback(mapping, sb.getState());
        }
        // no mapping present -> no need to do anything

    }

    // used to notify when the dataplane version is updated
    public void dataplaneCallback(NsiMapping mapping, State st) {
        try {
            log.info("dataplaneCallback");
            String nsaId = mapping.getNsaId();

            int notificationId = nsiMappingService.nextNotificationId(mapping);

            NsiRequesterNSA requesterNSA = this.nsiHeaderUtils.getRequesterNsa(nsaId);
            boolean performCallback = false;

            if (requesterNSA.getCallbackUrl().isEmpty()) {
                log.info("empty callback url, will not perform dataplaneCallback");
            } else {
                performCallback = true;
                log.info("dataplaneCallback, NSA {} url: {}", requesterNSA.getNsaId(), requesterNSA.getCallbackUrl());
            }

            ConnectionRequesterPort port = nsiSoapClientUtil.createRequesterClient(requesterNSA);
            net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ObjectFactory of =
                    new net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ObjectFactory();

            DataPlaneStateChangeRequestType dsrt = of.createDataPlaneStateChangeRequestType();
            DataPlaneStatusType dst = new DataPlaneStatusType();
            dsrt.setConnectionId(mapping.getNsiConnectionId());

            dsrt.setTimeStamp(nsiMappingService.getCalendar(Instant.now()));
            dst.setActive(false);
            if (st.equals(State.ACTIVE)) {
                dst.setActive(true);
                dst.setVersion(mapping.getDeployedDataplaneVersion());
                dst.setVersionConsistent(mapping.getDataplaneVersion().equals(mapping.getDeployedDataplaneVersion()));
            }

            dsrt.setDataPlaneStatus(dst);
            dsrt.setNotificationId(notificationId);

            String xml = marshalToXml(dsrt);
            NsiNotification nsiNotification = NsiNotification.builder()
                    .connectionId(mapping.getNsiConnectionId())
                    .notificationId((long) notificationId)
                    .xml(xml)
                    .type(NsiNotificationType.DATAPLANE_STATE_CHANGE)
                    .build();
            nsiNotifications.save(nsiNotification);

            String corrId = nsiHeaderUtils.newCorrelationId();
            Holder<CommonHeaderType> outHeader = nsiHeaderUtils.makeClientHeader(nsaId, corrId);
            if (performCallback) {
                port.dataPlaneStateChange(dsrt, outHeader);
            }

        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }

    }

    public void okCallback(NsiEvent event, NsiMapping mapping, CommonHeaderType inHeader) {
        try {
            log.info("OK callback for event {}", event);
            String nsaId = mapping.getNsaId();

            NsiRequesterNSA requesterNSA = this.nsiHeaderUtils.getRequesterNsa(nsaId);
            if (requesterNSA.getCallbackUrl().isEmpty()) {
                log.info("empty callback url, unable to okCallback");
                return;
            } else {
                log.info("okCallback, NSA {} url: {}", requesterNSA.getNsaId(), requesterNSA.getCallbackUrl());
            }

            ConnectionRequesterPort port = nsiSoapClientUtil.createRequesterClient(requesterNSA);

            GenericConfirmedType gct = new GenericConfirmedType();
            gct.setConnectionId(mapping.getNsiConnectionId());

            String corrId = inHeader.getCorrelationId();

            Holder<CommonHeaderType> outHeader = nsiHeaderUtils.makeClientHeader(nsaId, corrId);
            if (event.equals(NsiEvent.ABORT_CF)) {
                port.reserveAbortConfirmed(gct, outHeader);
            } else if (event.equals(NsiEvent.COMMIT_CF)) {
                port.reserveCommitConfirmed(gct, outHeader);
            } else if (event.equals(NsiEvent.TERM_CF)) {
                port.terminateConfirmed(gct, outHeader);
            } else if (event.equals(NsiEvent.PROV_CF)) {
                port.provisionConfirmed(gct, outHeader);
            } else if (event.equals(NsiEvent.REL_CF)) {
                port.releaseConfirmed(gct, outHeader);
            }
        } catch (Exception e) {
            // we do not care what happens to our callback, we let it fail
            log.error(e.getMessage(), e);
        }
    }

    // we use this to notify of errors, during reserve or commit
    public void errCallback(NsiEvent event, String nsaId, String nsiConnectionId, NsiMapping mapping,
                            String error, NsiErrors errNum, List<TypeValuePairType> tvps, String corrId) {
        try {

            NsiRequesterNSA requesterNSA = this.nsiHeaderUtils.getRequesterNsa(nsaId);
            boolean performCallback = false;
            if (requesterNSA.getCallbackUrl().isEmpty()) {
                log.info("empty callback url, unable to errCallback");
            } else {
                performCallback = true;
                log.info("errCallback, NSA {} url: {}", requesterNSA.getNsaId(), requesterNSA.getCallbackUrl());
            }

            ConnectionRequesterPort port = nsiSoapClientUtil.createRequesterClient(requesterNSA);

            Holder<CommonHeaderType> outHeader = this.nsiHeaderUtils.makeClientHeader(nsaId, corrId);

            GenericFailedType gft = new GenericFailedType();

            gft.setConnectionId(nsiConnectionId);
            gft.setServiceException(nsiHeaderUtils.makeSvcExcpType(error, errNum, tvps, nsiConnectionId));

            ConnectionStatesType cst;
            try {
                Connection c = nsiMappingService.getOscarsConnection(mapping);
                cst = nsiMappingService.makeConnectionStates(mapping, c);
            } catch (NsiMappingException ex) {
                // in case there's no oscars connection associated or we have a null mapping
                cst = nsiMappingService.makeConnectionStates(mapping, null);
            }

            gft.setConnectionStates(cst);
            if (performCallback) {
                if (event.equals(NsiEvent.RESV_FL)) {
                    port.reserveFailed(gft, outHeader);

                } else if (event.equals(NsiEvent.COMMIT_FL)) {
                    port.reserveCommitFailed(gft, outHeader);
                }
            }
        } catch (Exception ex) {
            // we do not care what happens to our callback, we let it fail
            log.error(ex.getMessage(), ex);
        }
    }


    /* utility / shared funcs */

    /* submit hold */
    public NsiReserveResult hold(ReserveType incomingRT, NsiMapping mapping) throws NsiInternalException, NsiValidationException {
        log.info("hold");
        log.info("hold for {} {}", mapping.getNsiConnectionId(), mapping.getOscarsConnectionId());
        String oscarsConnectionId = mapping.getOscarsConnectionId();

        //
        Optional<Connection> optC = connSvc.findConnection(mapping.getOscarsConnectionId());
        ReservationRequestCriteriaType crit = incomingRT.getCriteria();

        DevelUtils.dumpDebug("incomingRT", incomingRT);

        Optional<P2PServiceBaseType> op2ps = nsiMappingService.getP2PService(incomingRT);

        if (op2ps.isEmpty()) {
            throw new NsiValidationException("missing p2ps for reserve ", NsiErrors.MSG_PAYLOAD_ERROR);
        }

        P2PServiceBaseType p2ps = op2ps.get();
        // we always get capacity (hopefully)
        int mbps = (int) p2ps.getCapacity();
        DevelUtils.dumpDebug("p2ps", p2ps);

        log.info("capacity: {}", mbps);
        long begin;
        long end;
        Interval interval;
        List<Fixture> fixtures;
        List<Junction> junctions;
        List<Pipe> pipes;

        List<String> include = new ArrayList<>();
        ConnectionMode connectionMode = ConnectionMode.NEW;

        if (optC.isPresent()) {
            Connection c = optC.get();
            connectionMode = ConnectionMode.MODIFY;
            if (crit.getSchedule() != null) {
                interval = nsiMappingService.nsiToOscarsSchedule(crit.getSchedule());
                begin = interval.getBeginning().getEpochSecond();
                end = interval.getEnding().getEpochSecond();
            } else {
                begin = c.getReserved().getSchedule().getBeginning().getEpochSecond();
                end = c.getReserved().getSchedule().getEnding().getEpochSecond();
                interval = Interval.builder()
                        .beginning(c.getReserved().getSchedule().getBeginning())
                        .ending(c.getReserved().getSchedule().getEnding())
                        .build();
            }
            // recreate f, j, p based on reserved w modified mbps if applicable
            Pair<List<Fixture>, List<Junction>> fjp = nsiMappingService.simpleComponents(c, mbps);
            fixtures = fjp.getLeft();
            junctions = fjp.getRight();
            pipes = nsiMappingService.pipesFor(interval, mbps, junctions, include);

        } else {
            // a new reserve
            log.info("got p2p for new reserve");
            if (p2ps.getSourceSTP() != null) {
                mapping.setSrc(p2ps.getSourceSTP());
            }
            if (p2ps.getDestSTP() != null) {
                mapping.setDst(p2ps.getDestSTP());
            }

            interval = nsiMappingService.nsiToOscarsSchedule(crit.getSchedule());
            begin = interval.getBeginning().getEpochSecond();
            end = interval.getEnding().getEpochSecond();

            Pair<List<Fixture>, List<Junction>> fjs = nsiMappingService.fixturesAndJunctionsFor(p2ps, interval, oscarsConnectionId);
            fixtures = fjs.getLeft();
            junctions = fjs.getRight();

            pipes = nsiMappingService.pipesFor(interval, mbps, junctions, include);

            if (p2ps.getEro() != null) {
                for (OrderedStpType stp : p2ps.getEro().getOrderedSTP()) {
                    String urn = nsiMappingService.internalUrnFromStp(stp.getStp());
                    include.add(urn);
                }
            }
        }

        Instant exp = Instant.now().plus(resvTimeout, ChronoUnit.SECONDS);
        long expSecs = exp.toEpochMilli() / 1000L;
        log.info("got schedule and bw");

        List<SimpleTag> tags = new ArrayList<>();
        tags.add(SimpleTag.builder().category("nsi").contents("").build());

        List<TypeValuePairType> tvps = new ArrayList<>();

        log.info("making fixtures and junctions");
        try {
            SimpleConnection simpleConnection = SimpleConnection.builder()
                    .connectionId(mapping.getOscarsConnectionId())
                    .description(incomingRT.getDescription())
                    .heldUntil((int) expSecs)
                    .phase(Phase.HELD)
                    .state(State.WAITING)
                    .mode(BuildMode.MANUAL)
                    .begin((int) begin)
                    .end((int) end)
                    .fixtures(fixtures)
                    .junctions(junctions)
                    .serviceId(null)
                    .pipes(pipes)
                    .tags(tags)
                    .connection_mtu(9000)
                    .username("nsi")
                    .build();
            try {
                String pretty = jacksonObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(simpleConnection);
                log.debug("simple conn: \n{}", pretty);

            } catch (JsonProcessingException ex) {
                log.error(ex.getMessage(), ex);
            }
            // add a validity check
            try {

                Validity v = connSvc.validate(simpleConnection, connectionMode);

                if (!v.isValid()) {
                    return NsiReserveResult.builder()
                            .errorCode(NsiErrors.RESV_ERROR)
                            .success(false)
                            .errorMessage(v.getMessage())
                            .tvps(tvps)
                            .build();
                }

            } catch (ConnException ex) {
                TypeValuePairType tvp = new TypeValuePairType();
                tvp.setNamespace(NSI_TYPES);
                tvp.setType("connectionId");
                tvps.add(tvp);

                return NsiReserveResult.builder()
                        .errorCode(NsiErrors.MSG_PAYLOAD_ERROR)
                        .success(false)
                        .errorMessage("No connection id")
                        .tvps(tvps)
                        .build();
            }

            Connection c;
            Pair<SimpleConnection, Connection> results = connSvc.holdConnection(simpleConnection);
            if (results.getLeft().getValidity().isValid()) {
                 c = results.getRight();
            } else {
                return NsiReserveResult.builder()
                        .errorCode(NsiErrors.RESV_ERROR)
                        .success(false)
                        .errorMessage(results.getLeft().getValidity().getMessage())
                        .tvps(tvps)
                        .build();
            }

            try {
                String pretty = jacksonObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(c);
                log.debug("full conn: \n{}", pretty);

            } catch (JsonProcessingException jpe) {
                // do nothing, this is just for debugging
            }

            mapping.setOscarsConnectionId(c.getConnectionId());
            nsiMappingService.save(mapping);

            return NsiReserveResult.builder()
                    .errorCode(NsiErrors.OK)
                    .success(true)
                    .errorMessage("")
                    .tvps(tvps)
                    .build();

        } catch (ConnException ex) {
            return NsiReserveResult.builder()
                    .errorCode(NsiErrors.RESV_ERROR)
                    .success(false)
                    .errorMessage(ex.getMessage())
                    .tvps(tvps)
                    .build();
        }
    }

    // TODO: actually validate
    public NsiReserveResult validateRT(ReserveType rt) {
        return NsiReserveResult.builder()
                .errorCode(null)
                .success(true)
                .errorMessage("")
                .tvps(new ArrayList<>())
                .build();
    }

    public String marshalToXml(ErrorEventType eet) throws JAXBException {
        StringWriter sw = new StringWriter();
        JAXBContext context=JAXBContext.newInstance(ErrorEventType.class);
        Marshaller marshaller = context.createMarshaller();
        JAXBElement<ErrorEventType> jaxbElement
                = new JAXBElement<>( new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "errorEvent"), ErrorEventType.class, eet);

        marshaller.marshal(jaxbElement, sw);
        return sw.toString();
    }

    public String marshalToXml(ReserveTimeoutRequestType rtrt) throws JAXBException {
        StringWriter sw = new StringWriter();
        JAXBContext context=JAXBContext.newInstance(ReserveTimeoutRequestType.class);
        Marshaller marshaller = context.createMarshaller();
        JAXBElement<ReserveTimeoutRequestType> jaxbElement
                = new JAXBElement<>( new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "reserveTimeout"), ReserveTimeoutRequestType.class, rtrt);

        marshaller.marshal(jaxbElement, sw);
        return sw.toString();
    }

    public String marshalToXml(DataPlaneStateChangeRequestType dpscrt) throws JAXBException {
        StringWriter sw = new StringWriter();
        JAXBContext context=JAXBContext.newInstance(DataPlaneStateChangeRequestType.class);
        Marshaller marshaller = context.createMarshaller();
        JAXBElement<DataPlaneStateChangeRequestType> jaxbElement
                = new JAXBElement<>( new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "dataPlaneStateChange"), DataPlaneStateChangeRequestType.class, dpscrt);

        marshaller.marshal(jaxbElement, sw);
        return sw.toString();
    }

}
