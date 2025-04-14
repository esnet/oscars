package net.es.oscars.nsi.svc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.ws.Holder;
import jakarta.xml.ws.WebServiceException;
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
import net.es.oscars.dto.pss.cmd.CommandType;
import net.es.oscars.model.Interval;
import net.es.oscars.nsi.beans.NsiErrors;
import net.es.oscars.nsi.beans.NsiEvent;
import net.es.oscars.nsi.beans.NsiHoldResult;
import net.es.oscars.nsi.beans.NsiModify;
import net.es.oscars.nsi.db.NsiRequesterNSARepository;
import net.es.oscars.nsi.ent.NsiMapping;
import net.es.oscars.nsi.ent.NsiRequesterNSA;
import net.es.oscars.pce.PceService;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.sb.SouthboundQueuer;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.BuildMode;
import net.es.oscars.resv.enums.ConnectionMode;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.enums.State;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.soap.NsiSoapClientUtil;
import net.es.oscars.web.beans.*;
import net.es.oscars.web.simple.*;
import net.es.topo.common.devel.DevelUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class NsiService {

    private final NsiQueries nsiQueries;
    @Value("${nsi.resv-timeout}")
    private Integer nsiResvTimeout;

    @Value("${nsi.provider-nsa}")
    private String providerNsa;

    final public static String SERVICE_TYPE = "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE";
    final public static String NSI_TYPES = "http://schemas.ogf.org/nsi/2013/12/framework/types";

    private final ConnectionRepository connRepo;
    private final NsiRequestManager nsiRequestManager;
    private final NsiHeaderUtils nsiHeaderUtils;
    private final NsiStateEngine nsiStateEngine;
    private final ConnService connSvc;
    private final NsiMappingService nsiMappingService;
    private final NsiSoapClientUtil nsiSoapClientUtil;
    private final SouthboundQueuer southboundQueuer;
    private final ObjectMapper jacksonObjectMapper;

    public NsiService(ConnectionRepository connRepo, NsiRequesterNSARepository requesterNsaRepo, NsiRequestManager nsiRequestManager, NsiHeaderUtils nsiHeaderUtils, NsiStateEngine nsiStateEngine, PceService pceService, ConnService connSvc, NsiMappingService nsiMappingService, NsiSoapClientUtil nsiSoapClientUtil, SouthboundQueuer southboundQueuer, ObjectMapper jacksonObjectMapper, NsiQueries nsiQueries) {
        this.connRepo = connRepo;
        this.requesterNsaRepo = requesterNsaRepo;
        this.nsiRequestManager = nsiRequestManager;
        this.nsiHeaderUtils = nsiHeaderUtils;
        this.nsiStateEngine = nsiStateEngine;
        this.pceService = pceService;
        this.connSvc = connSvc;
        this.nsiMappingService = nsiMappingService;
        this.nsiSoapClientUtil = nsiSoapClientUtil;
        this.southboundQueuer = southboundQueuer;
        this.jacksonObjectMapper = jacksonObjectMapper;
        this.nsiQueries = nsiQueries;
    }

    /* async operations */
    public void reserve(CommonHeaderType header, ReserveType rt, NsiMapping mapping) {
        Executors.newCachedThreadPool().submit(() -> {
            log.info("starting NSI reserve");
            try {
                try {
                    log.info("transitioning state: RESV_START");
                    nsiStateEngine.reserve(NsiEvent.RESV_START, mapping);
                    log.info("submitting hold");
                    NsiHoldResult result = this.submitHold(rt, mapping);

                    // submit hold succeeded means we are good to go
                    if (result.getSuccess()) {
                        log.info("successful reserve, updating state");
                        nsiStateEngine.reserve(NsiEvent.RESV_CF, mapping);
                        nsiMappingService.save(mapping);
                        log.info("sending reserveConfirmCallback");
                        this.reserveConfirmCallback(mapping, header);

                    } else {
                        // we don't save the mapping
                        log.error("error reserving, sending errCallback with RESV_FL");
                        this.errCallback(NsiEvent.RESV_FL, mapping,
                                result.getErrorMessage(),
                                result.getErrorCode().toString(),
                                result.getTvps(),
                                header.getCorrelationId());
                    }

                } catch (NsiStateException ex) {
                    log.error("State transition error: " + ex.getMessage(), ex);
                    this.errCallback(NsiEvent.RESV_FL, mapping,
                            "State transition error",
                            NsiErrors.NRM_ERROR.toString(),
                            new ArrayList<>(),
                            header.getCorrelationId());

                } catch (NsiInternalException ex) {
                    log.error("Internal error: " + ex.getMessage(), ex);
                    this.errCallback(NsiEvent.RESV_FL, mapping,
                            "Internal error",
                            NsiErrors.NRM_ERROR.toString(),
                            new ArrayList<>(),
                            header.getCorrelationId());
                }
            } catch (ServiceException cex) {
                log.error("remote error from errCallback", cex);
            }
            log.info("ending reserve");
        });
    }

    public void modify(CommonHeaderType header, ReserveType rt, NsiMapping mapping, int newVersion) {
        Executors.newCachedThreadPool().submit(() -> {
            log.info("starting modify task");
            DevelUtils.dumpDebug("modifyRT", rt);
            try {
                log.info("transitioning NSI state: RESV_CHECK");
                nsiStateEngine.reserve(NsiEvent.RESV_CHECK, mapping);

                nsiMappingService.save(mapping);
                // now NSI state is RESERVE_CHECKING
                Connection c = connSvc.findConnection(mapping.getOscarsConnectionId());

                Instant initialBeginning = c.getReserved().getSchedule().getBeginning();
                Instant initialEnding = c.getReserved().getSchedule().getEnding();
                int initialDataplaneVersion = mapping.getDataplaneVersion();

                int initialBandwidth = 0;
                for (VlanFixture f : c.getReserved().getCmp().getFixtures()) {
                    if (f.getIngressBandwidth() > initialBandwidth) {
                        initialBandwidth = f.getIngressBandwidth();
                    }
                    if (f.getEgressBandwidth() > initialBandwidth) {
                        initialBandwidth = f.getIngressBandwidth();
                    }
                }

                for (VlanPipe p : c.getReserved().getCmp().getPipes()) {
                    if (p.getAzBandwidth() > initialBandwidth) {
                        initialBandwidth = p.getAzBandwidth();
                    }
                    if (p.getZaBandwidth() > initialBandwidth) {
                        initialBandwidth = p.getZaBandwidth();
                    }
                }

                Instant newBeginning = initialBeginning;
                Instant newEnding = initialEnding;
                int newBandwidth = this.getModifyCapacity(rt).intValue();

                ReservationRequestCriteriaType crit = rt.getCriteria();
                if (crit.getSchedule().getStartTime() != null) {
                    log.info("got updated beginning");
                    XMLGregorianCalendar xst = crit.getSchedule().getStartTime().getValue();
                    newBeginning = xst.toGregorianCalendar().toInstant();
                }

                if (crit.getSchedule().getEndTime() != null) {
                    log.info("got updated ending");
                    XMLGregorianCalendar xet = crit.getSchedule().getEndTime().getValue();
                    newEnding = xet.toGregorianCalendar().toInstant();
                }

                // we update the dataplane version
                mapping.setDataplaneVersion(newVersion);

                Validity v = connSvc.modifyNsi(c, newBandwidth, newBeginning, newEnding);
                if (v.isValid()) {
                    // At this point we know that the modification is valid
                    //
                    // We have already committed the resources inside OSCARS;
                    // - regular reserve does the "normal" INITIAL -> HELD -> RESERVED OSCARS state diagram
                    // - modify goes from RESERVED -> RESERVED.. this needs a major cleanup in the future.

                    // first we set our own state to RESERVE_HELD
                    nsiStateEngine.reserve(NsiEvent.RESV_CF, mapping);
                    // and save the new state in our mapping
                    nsiMappingService.save(mapping);

                    // We call back to confirm the modify message; this is the same behavior as reserve()

                    // It is possible now for
                    // - the callback to fail - we will just log items and keep trucking along

                    // - the customer to..
                    //    - commit the modification (good path), or
                    //    - abort the modify / fail to commit in time (bad path)

                    // The NsiRequestManager will keep track of in-flight modify requests.

                    Instant timeout = Instant.now().plus(nsiResvTimeout, ChronoUnit.SECONDS);

                    NsiModify modify = NsiModify.builder()
                            .initial(NsiModify.Spec.builder()
                                    .bandwidth(initialBandwidth)
                                    .beginning(initialBeginning)
                                    .ending(initialEnding)
                                    .dataplaneVersion(initialDataplaneVersion)
                                    .build())
                            .modified(NsiModify.Spec.builder()
                                    .bandwidth(newBandwidth)
                                    .beginning(newBeginning)
                                    .ending(newEnding)
                                    .dataplaneVersion(newVersion)
                                    .build())
                            .timeout(timeout)
                            .nsiConnectionId(mapping.getNsiConnectionId())
                            .build();

                    this.nsiRequestManager.addInFlightModify(modify);

                    try {
                        this.reserveConfirmCallback(mapping, header);
                    } catch (WebServiceException | ServiceException cex) {
                        log.error("reserve succeeded: then callback failed", cex);
                    }
                } else {
                    // if this failed, set our own state to RESERVE_FAILED; we should get an ABORT to clean things
                    // up, ... or it will eventually expire and be cleaned up
                    nsiStateEngine.reserve(NsiEvent.RESV_FL, mapping);
                    nsiMappingService.save(mapping);

                    try {
                        this.errCallback(NsiEvent.RESV_FL, mapping,
                                v.getMessage(),
                                NsiErrors.RESV_ERROR.toString(),
                                new ArrayList<>(),
                                header.getCorrelationId());
                    } catch (WebServiceException | ServiceException cex) {
                        log.error("reserve failed: then errCallback failed", cex);
                    }
                }
            } catch (NsiStateException ex) {
                this.errCallback(NsiEvent.RESV_FL, mapping,
                        ex.getMessage(),
                        NsiErrors.RESV_ERROR.toString(),
                        new ArrayList<>(),
                        header.getCorrelationId());
            } catch (NsiInternalException ex) {
                log.error("Internal error: " + ex.getMessage(), ex);
                this.errCallback(NsiEvent.RESV_FL, mapping,
                        "OSCARS internal error in modify",
                        NsiErrors.NRM_ERROR.toString(),
                        new ArrayList<>(),
                        header.getCorrelationId());
            } catch (WebServiceException | ServiceException cex) {
                log.error("modify failed: then errCallback failed", cex);
            } catch (ModifyException e) {
                throw new RuntimeException(e);
            }
            log.info("ending modify");
        });
    }

    public void commit(CommonHeaderType header, NsiMapping mapping) {

        Executors.newCachedThreadPool().submit(() -> {
            log.info("starting commit for "+mapping.getNsiConnectionId());
            try {
                nsiStateEngine.commit(NsiEvent.COMMIT_START, mapping);
                Connection c = nsiMappingService.getOscarsConnection(mapping);

                // The commit could be either a commit for the initial commit flow or it can
                // be for an in-flight modify.

                //
                if (nsiRequestManager.getInFlightModify(mapping.getNsiConnectionId()) == null) {
                    log.info("commit for initial reserve");
                    if (!c.getPhase().equals(Phase.HELD)) {
                        throw new NsiException("Invalid connection phase", NsiErrors.TRANS_ERROR);
                    }
                    connSvc.commit(c);

                } else {
                    // tell the request manager that this committed OK
                    log.info("committing modify " + mapping.getNsiConnectionId());

                    NsiModify modify = nsiRequestManager.getInFlightModify(mapping.getNsiConnectionId());
                    mapping.setDataplaneVersion(modify.getModified().getDataplaneVersion());
                    nsiMappingService.save(mapping);
                    log.info("new dataplane version " + mapping.getDataplaneVersion());

                    nsiRequestManager.commit(mapping.getNsiConnectionId());
                }

                nsiStateEngine.commit(NsiEvent.COMMIT_CF, mapping);

                log.info("completed commit");
                try {
                    this.okCallback(NsiEvent.COMMIT_CF, mapping, header);
                    log.info("sent commit confirmed callback");
                } catch (WebServiceException | ServiceException ex) {
                    log.error("commit confirmed callback failed", ex);
                }
            } catch (PCEException | NsiException ex) {
                log.error("failed commit");
                log.error(ex.getMessage(), ex);
                try {
                    nsiStateEngine.commit(NsiEvent.COMMIT_FL, mapping);
                    this.errCallback(NsiEvent.COMMIT_FL, mapping,
                            ex.getMessage(), NsiErrors.NRM_ERROR.toString(),
                            new ArrayList<>(),
                            header.getCorrelationId());
                } catch (NsiException nex) {
                    log.error("commit failed: then internal error", nex);

                } catch (WebServiceException | ServiceException cex) {
                    log.error("commit failed: then callback failed", cex);
                }
            } catch (RuntimeException ex) {
                log.error("serious error", ex);
            }
            log.info("ending commit");
            return null;
        });
    }

    public void abort(CommonHeaderType header, NsiMapping mapping) {
        log.info("starting abort task for " + mapping.getNsiConnectionId());

        Executors.newCachedThreadPool().submit(() -> {
            try {
                Connection c = nsiMappingService.getOscarsConnection(mapping);
                nsiStateEngine.abort(NsiEvent.ABORT_START, mapping);
                if (!c.getPhase().equals(Phase.HELD)) {
                    throw new NsiException("invalid reservation phase, cannot abort", NsiErrors.TRANS_ERROR);
                }
                connSvc.release(c);
                log.info("completed abort");
                nsiStateEngine.abort(NsiEvent.ABORT_CF, mapping);
                try {
                    this.okCallback(NsiEvent.ABORT_CF, mapping, header);
                } catch (WebServiceException | ServiceException ex) {
                    log.error("abort confirmed callback failed", ex);
                }

            } catch (NsiException ex) {
                log.error("internal error", ex);
            } catch (RuntimeException ex) {
                log.error("serious error", ex);
            }
            return null;
        });
    }

    public void provision(CommonHeaderType header, NsiMapping mapping) {
        log.info("starting provision task for " + mapping.getNsiConnectionId());

        Executors.newCachedThreadPool().submit(() -> {
            try {
                Connection c = nsiMappingService.getOscarsConnection(mapping);
                if (!c.getPhase().equals(Phase.RESERVED)) {
                    log.error("cannot provision unless RESERVED");
                    return null;
                }

                nsiStateEngine.provision(NsiEvent.PROV_START, mapping);


                c.setMode(BuildMode.AUTOMATIC);
                connRepo.save(c);

                nsiStateEngine.provision(NsiEvent.PROV_CF, mapping);

                try {
                    this.okCallback(NsiEvent.PROV_CF, mapping, header);
                    log.info("completed provision confirm callback");
                } catch (WebServiceException | ServiceException ex) {
                    log.error("provision confirmed callback failed", ex);
                }

                log.info("completed provision");
            } catch (RuntimeException ex) {
                log.error("serious error", ex);

            } catch (NsiException ex) {
                log.error("provision internal error", ex);
            }
            return null;
        });
    }

    public void release(CommonHeaderType header, NsiMapping mapping) {
        log.info("starting release task for " + mapping.getNsiConnectionId());

        Executors.newCachedThreadPool().submit(() -> {
            try {
                Connection c = nsiMappingService.getOscarsConnection(mapping);
                if (!c.getPhase().equals(Phase.RESERVED)) {
                    log.error("cannot release unless RESERVED");
                    return null;
                }

                nsiStateEngine.release(NsiEvent.REL_START, mapping);

                c.setMode(BuildMode.MANUAL);
                // if we are after start time, we will need to tear down
                if (Instant.now().isAfter(c.getReserved().getSchedule().getBeginning())) {
                    if (c.getState().equals(State.ACTIVE)) {
                        southboundQueuer.add(CommandType.DISMANTLE, c.getConnectionId(), State.FINISHED);
                    }
                }

                nsiStateEngine.release(NsiEvent.REL_CF, mapping);

                try {
                    this.okCallback(NsiEvent.REL_CF, mapping, header);
                } catch (WebServiceException | ServiceException ex) {
                    log.error("release confirmed callback failed", ex);
                }
                log.info("completed release");


            } catch (RuntimeException ex) {
                log.error("serious error", ex);

            } catch (NsiException ex) {
                log.error("release internal error", ex);
            }
            return null;
        });
    }

    public void terminate(CommonHeaderType header, NsiMapping mapping) {
        log.info("starting terminate task for " + mapping.getNsiConnectionId());

        Executors.newCachedThreadPool().submit(() -> {
            try {
                Connection c = nsiMappingService.getOscarsConnection(mapping);
                // the cancel only needs to happen if we are not in FORCED_END or PASSED_END_TIME
                if (mapping.getLifecycleState().equals(LifecycleStateEnumType.CREATED)) {
                    connSvc.release(c);
                }
                nsiStateEngine.termStart(mapping);
                log.info("completed terminate");
                nsiStateEngine.termConfirm(mapping);
                try {
                    this.okCallback(NsiEvent.TERM_CF, mapping, header);
                    log.info("sent term cf callback");
                } catch (WebServiceException | ServiceException ex) {
                    log.error("term confirmed callback failed", ex);
                }
            } catch (RuntimeException ex) {
                log.error("serious error", ex);

            } catch (NsiException ex) {
                log.error("failed terminate, internal error");
                log.error(ex.getMessage(), ex);
            }

            return null;
        });

    }

    public void rollbackModify(NsiMapping mapping) {
        log.info("modify timed out for " + mapping.getNsiConnectionId() + " " + mapping.getOscarsConnectionId());
        NsiModify modify = nsiRequestManager.getInFlightModify(mapping.getNsiConnectionId());

        Connection c = connSvc.findConnection(mapping.getOscarsConnectionId());

        try {
            log.info("rolling back modify " + mapping.getNsiConnectionId() + " " + mapping.getOscarsConnectionId());
            connSvc.modifyNsi(c, modify.getInitial().getBandwidth(), modify.getInitial().getBeginning(), modify.getInitial().getEnding());
        } catch (ModifyException ex) {
            log.error("Internal error: " + ex.getMessage(), ex);
        } finally {
            mapping.setDataplaneVersion(modify.getInitial().getDataplaneVersion());
            nsiMappingService.save(mapping);
            this.resvTimedOut(mapping);
        }

    }

    // triggered from ConnController.release(), called by the UI when the user
    // presses the "Release" button
    public void forcedEnd(NsiMapping mapping) {
        log.info("starting forcedEnd task for " + mapping.getNsiConnectionId());

        Executors.newCachedThreadPool().submit(() -> {
            try {
                nsiStateEngine.forcedEnd(mapping);
                this.errorNotify(NsiEvent.FORCED_END, mapping);
            } catch (NsiException ex) {
                log.error("failed forcedEnd, internal error");
                log.error(ex.getMessage(), ex);
            } catch (RuntimeException ex) {
                log.error("serious error", ex);

            } catch (ServiceException ex) {
                log.error("term confirm callback failed", ex);
            }

            return null;
        });

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
                QuerySummaryConfirmedType qsct = nsiQueries.querySummary(query);
                try {
                    port.querySummaryConfirmed(qsct, outHeader);

                } catch (ServiceException | WebServiceException ex) {
                    log.error("could not perform query callback");
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
                    log.error("could not perform query callback");
                    log.error(ex.getMessage(), ex);
                }
            } catch (RuntimeException ex) {
                log.error(ex.getMessage(), ex);
            }

            return null;
        });
    }



    /* triggered events from TransitionStates periodic tasks */
    public void resvTimedOut(NsiMapping mapping) {
        log.info("resv timeout for " + mapping.getNsiConnectionId() + " " + mapping.getOscarsConnectionId());
        try {
            nsiStateEngine.resvTimedOut(mapping);
            this.nsiMappingService.save(mapping);
            this.errCallback(NsiEvent.RESV_TIMEOUT, mapping,
                    "reservation timeout", "", new ArrayList<>(),
                    nsiHeaderUtils.newCorrelationId());
        } catch (NsiInternalException ex) {


        } catch (NsiStateException ex) {

        } catch (ServiceException ex) {
            log.error("timeout callback failed", ex);
        } catch (NsiException ex) {
            log.error("internal error", ex);
        }

    }


    public void pastEndTime(NsiMapping mapping) {
        log.info("past end time for " + mapping.getNsiConnectionId() + " " + mapping.getOscarsConnectionId());
        try {
            nsiStateEngine.pastEndTime(mapping);
        } catch (NsiException ex) {
            log.error("internal error", ex);
        }
    }

    /* submit hold */
    public NsiHoldResult submitHold(ReserveType rt, NsiMapping mapping) throws NsiInternalException {
        log.info("preparing connection");

        P2PServiceBaseType p2p = nsiMappingService.getP2PService(rt);
        log.info("got p2p");
        String src = p2p.getSourceSTP();
        String dst = p2p.getDestSTP();
        mapping.setSrc(src);
        mapping.setDst(dst);


        ReservationRequestCriteriaType crit = rt.getCriteria();

        long mbpsLong = p2p.getCapacity();
        Integer mbps = (int) mbpsLong;

        Interval interval = nsiMappingService.nsiToOscarsSchedule(crit.getSchedule());
        long begin = interval.getBeginning().getEpochSecond();
        long end = interval.getEnding().getEpochSecond();

        Instant exp = Instant.now().plus(nsiResvTimeout, ChronoUnit.SECONDS);
        long expSecs = exp.toEpochMilli() / 1000L;
        log.info("got schedule and bw");

        List<SimpleTag> tags = new ArrayList<>();
        tags.add(SimpleTag.builder().category("nsi").contents("").build());

        List<String> include = new ArrayList<>();
        List<TypeValuePairType> tvps = new ArrayList<>();

        if (p2p.getEro() != null) {
            for (OrderedStpType stp : p2p.getEro().getOrderedSTP()) {
                String urn = nsiMappingService.internalUrnFromStp(stp.getStp());
                include.add(urn);
            }
        }

        log.info("making fixtures and junctions");
        try {
            Pair<List<Fixture>, List<Junction>> fixturesAndJunctions = nsiMappingService.fixturesAndJunctionsFor(p2p, interval);
            log.info("making pipes");
            List<Pipe> pipes = nsiMappingService.pipesFor(interval, mbps, fixturesAndJunctions.getRight(), include);

            SimpleConnection simpleConnection = SimpleConnection.builder()
                    .connectionId(mapping.getOscarsConnectionId())
                    .description(rt.getDescription())
                    .heldUntil((int) expSecs)
                    .phase(Phase.HELD)
                    .state(State.WAITING)
                    .mode(BuildMode.MANUAL)
                    .begin((int) begin)
                    .end((int) end)
                    .fixtures(fixturesAndJunctions.getLeft())
                    .junctions(fixturesAndJunctions.getRight())
                    .serviceId(null)
                    .pipes(pipes)
                    .tags(tags)
                    .username("nsi")
                    .build();
            try {
                String pretty = jacksonObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(simpleConnection);
                log.debug("simple conn: \n" + pretty);

            } catch (JsonProcessingException ex) {
                log.error(ex.getMessage(), ex);
            }
            // add a validity check
            try {
                Validity v = connSvc.validate(simpleConnection, ConnectionMode.NEW);

                if (!v.isValid()) {
                    return NsiHoldResult.builder()
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

                return NsiHoldResult.builder()
                        .errorCode(NsiErrors.MSG_PAYLOAD_ERROR)
                        .success(false)
                        .errorMessage("No connection id")
                        .tvps(tvps)
                        .build();
            }

            Connection c = connSvc.toNewConnection(simpleConnection);
            try {
                String pretty = jacksonObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(c);
                log.debug("full conn: \n" + pretty);

            } catch (JsonProcessingException jpe) {
                // do nothing, this is just for debugging
            }

            log.info("saving new connection");
            connRepo.save(c);
            mapping.setOscarsConnectionId(c.getConnectionId());

            return NsiHoldResult.builder()
                    .errorCode(NsiErrors.OK)
                    .success(true)
                    .errorMessage("")
                    .tvps(tvps)
                    .build();

        } catch (NsiValidationException ex) {
            return NsiHoldResult.builder()
                    .errorCode(NsiErrors.RESV_ERROR)
                    .success(false)
                    .errorMessage(ex.getMessage())
                    .tvps(tvps)
                    .build();
        }
    }


    /* SOAP calls to the client */


    public void errorNotify(NsiEvent event, NsiMapping mapping) throws NsiException, ServiceException, DatatypeConfigurationException {
        String nsaId = mapping.getNsaId();
        NsiRequesterNSA requesterNSA = this.nsiHeaderUtils.getRequesterNsa(nsaId);

        ConnectionRequesterPort port = nsiSoapClientUtil.createRequesterClient(requesterNSA);
        String corrId = nsiHeaderUtils.newCorrelationId();
        Holder<CommonHeaderType> outHeader = nsiHeaderUtils.makeClientHeader(nsaId, corrId);

        ErrorEventType eet = new ErrorEventType();
        eet.setOriginatingConnectionId(mapping.getNsiConnectionId());
        eet.setOriginatingNSA(this.providerNsa);

        eet.setTimeStamp(nsiMappingService.getCalendar(Instant.now()));
        eet.setEvent(EventEnumType.FORCED_END);
        port.errorEvent(eet, outHeader);

    }

    public void reserveConfirmCallback(NsiMapping mapping, CommonHeaderType inHeader) throws NsiException, ServiceException {
        String nsaId = mapping.getNsaId();
        NsiRequesterNSA requesterNSA = nsiHeaderUtils.getRequesterNsa(nsaId);

        ConnectionRequesterPort port = nsiSoapClientUtil.createRequesterClient(requesterNSA);

        GenericConfirmedType gct = new GenericConfirmedType();
        gct.setConnectionId(mapping.getNsiConnectionId());

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
            String pretty = jacksonObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rct);
            log.debug("rcct: \n" + pretty);
        } catch (JsonProcessingException ex) {
            log.error(ex.getMessage(), ex);
        }
        port.reserveConfirmed(rct, outHeader);
    }

    public void dataplaneCallback(NsiMapping mapping, State st) throws NsiInternalException, ServiceException {
        log.info("dataplaneCallback ");
        String nsaId = mapping.getNsaId();
        NsiRequesterNSA requesterNSA = this.nsiHeaderUtils.getRequesterNsa(nsaId);

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
        }

        dst.setVersion(mapping.getDataplaneVersion());
        dst.setVersionConsistent(true);
        dsrt.setDataPlaneStatus(dst);

        String corrId = nsiHeaderUtils.newCorrelationId();
        Holder<CommonHeaderType> outHeader = nsiHeaderUtils.makeClientHeader(nsaId, corrId);
        port.dataPlaneStateChange(dsrt, outHeader);

    }

    public void okCallback(NsiEvent event, NsiMapping mapping, CommonHeaderType inHeader)
            throws NsiInternalException, ServiceException {
        log.info("OK callback for event " + event);
        String nsaId = mapping.getNsaId();

        NsiRequesterNSA requesterNSA = this.nsiHeaderUtils.getRequesterNsa(nsaId);
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

    }

    public void errCallback(NsiEvent event, NsiMapping mapping, String error, String errNum, List<TypeValuePairType> tvps, String corrId)
            throws NsiInternalException, ServiceException, NsiMappingException {
        String nsaId = mapping.getNsaId();

        NsiRequesterNSA requesterNSA = this.nsiHeaderUtils.getRequesterNsa(nsaId);

        ConnectionRequesterPort port = nsiSoapClientUtil.createRequesterClient(requesterNSA);

        Holder<CommonHeaderType> outHeader = this.nsiHeaderUtils.makeClientHeader(nsaId, corrId);

        GenericFailedType gft = new GenericFailedType();

        gft.setConnectionId(mapping.getNsiConnectionId());
        gft.setServiceException(this.makeSET(error, errNum, tvps, mapping));

        if (!event.equals(NsiEvent.RESV_FL)) {
            Connection c = nsiMappingService.getOscarsConnection(mapping);
            ConnectionStatesType cst = nsiMappingService.makeConnectionStates(mapping, c);
            gft.setConnectionStates(cst);
        } else {
            // if it's a RESV_FL there won't be a connection to find, so set from null
            gft.setConnectionStates(nsiMappingService.makeConnectionStates(mapping, null));
        }

        if (event.equals(NsiEvent.RESV_FL)) {
            port.reserveFailed(gft, outHeader);

        } else if (event.equals(NsiEvent.COMMIT_FL)) {
            port.reserveCommitFailed(gft, outHeader);

        } else if (event.equals(NsiEvent.RESV_TIMEOUT)) {
            ReserveTimeoutRequestType rrt = new ReserveTimeoutRequestType();
            rrt.setConnectionId(mapping.getNsiConnectionId());

            rrt.setTimeStamp(nsiMappingService.getCalendar(Instant.now()));
            // TODO: implement incrementing notificationIds

            rrt.setNotificationId(0L);
            rrt.setOriginatingConnectionId(mapping.getNsiConnectionId());
            rrt.setOriginatingNSA(providerNsa);
            rrt.setTimeoutValue(nsiResvTimeout);
            port.reserveTimeout(rrt, outHeader);
        }
    }


    /* utility / shared funcs */




    public Long getModifyCapacity(ReserveType rt) throws NsiException {
        ReservationRequestCriteriaType crit = rt.getCriteria();
        Long result = null;
        for (Object obj : crit.getAny()) {
            if (result == null) {
                if (obj instanceof @SuppressWarnings("rawtypes")JAXBElement jaxb) {
                    if (jaxb.getDeclaredType() == Long.class) {
                        result = ((Long) jaxb.getValue());
                        if (jaxb.getName().toString().equals("{http://schemas.ogf.org/nsi/2013/12/services/point2point}capacity")) {
                            log.debug("matched capacity qname");
                        }
                    }
                }
            }
        }

        if (result == null) {
            throw new NsiException("unable to determine capacity", NsiErrors.MISSING_PARAM_ERROR);
        }
        return result;
    }


}
