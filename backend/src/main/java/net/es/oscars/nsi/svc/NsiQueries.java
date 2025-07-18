package net.es.oscars.nsi.svc;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.*;
import net.es.nsi.lib.soap.gen.nsi_2_0.services.point2point.ObjectFactory;
import net.es.nsi.lib.soap.gen.nsi_2_0.services.point2point.P2PServiceBaseType;
import net.es.oscars.app.exc.NsiException;
import net.es.oscars.app.exc.NsiInternalException;
import net.es.oscars.nsi.beans.NsiErrors;
import net.es.oscars.nsi.beans.NsiRequest;
import net.es.oscars.nsi.db.NsiMappingRepository;
import net.es.oscars.nsi.ent.NsiMapping;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.Phase;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class NsiQueries {

    private final NsiMappingRepository nsiRepo;
    private final NsiMappingService nsiMappingService;
    private final NsiRequestManager nsiRequestManager;

    public NsiQueries(NsiMappingRepository nsiRepo, NsiMappingService nsiMappingService, NsiRequestManager nsiRequestManager) {
        this.nsiRepo = nsiRepo;
        this.nsiMappingService = nsiMappingService;
        this.nsiRequestManager = nsiRequestManager;
    }

    public void validateQuery(QueryType query) throws NsiException {
        if (query.getIfModifiedSince() != null) {
            throw new NsiInternalException("IMS not supported yet", NsiErrors.UNIMPLEMENTED);
        }

        for (String connId : query.getConnectionId()) {
            /*
            if (!nsiMappingService.hasNsiMapping(connId)) {
                throw new NsiValidationException("NSI connection id not found", NsiErrors.RESERVATION_NONEXISTENT);
            }
             */
        }

    }

    @Transactional
    public QueryRecursiveConfirmedType queryRecursive(QueryType query) throws NsiInternalException {
        log.info("queryRecursive");
        QueryRecursiveConfirmedType qrct = new QueryRecursiveConfirmedType();

        Set<NsiMapping> mappings = new HashSet<>();
        for (String connId : query.getConnectionId()) {
            nsiRepo.findByNsiConnectionId(connId).ifPresent(mappings::add);
        }
        for (String gri : query.getGlobalReservationId()) {
            mappings.addAll(nsiRepo.findByNsiGri(gri));
        }

        Long resultId = 0L;
        for (NsiMapping mapping : mappings) {
            QueryRecursiveResultType qrrt = this.toQRRT(mapping);
            if (qrrt != null) {
                qrrt.setResultId(resultId);
                qrct.getReservation().add(qrrt);
                resultId++;
            }

        }
        return qrct;
    }


    @Transactional
    public QuerySummaryConfirmedType querySummary(QueryType query, Map<String, NsiMapping> initialReserveMappings) throws NsiInternalException {
        log.info("querySummary");

        QuerySummaryConfirmedType qsct = new QuerySummaryConfirmedType();

        qsct.setLastModified(nsiMappingService.getCalendar(Instant.now()));

        // set last modified if it happens to be empty
        nsiRepo.findAll().forEach(m -> {
            if (m.getLastModified() == null) {
                m.setLastModified(Instant.now());
            }
        });


        Set<NsiMapping> mappings = new HashSet<>();
        if (query.getConnectionId().isEmpty() && query.getGlobalReservationId().isEmpty()) {
            // empty query = find all
            mappings.addAll(nsiRepo.findAll()
                    .stream()
                    .filter(m -> {
                        if (m.getLifecycleState().equals(LifecycleStateEnumType.TERMINATED) || m.getLifecycleState().equals(LifecycleStateEnumType.FAILED)) {
                            // filter out any in lifecycle TERMINATED or FAILED that haven't been modified in the last 1 hour
                            return !m.getLastModified().isBefore(Instant.now().minus(1, ChronoUnit.HOURS));
                        }

                        return true;
                    }).collect(Collectors.toSet())
            );
            // if there happens to be a mapping in-memory for an nsiConnectionId that is found in nsiRepo,
            // that means it has been processed so we will remove it from our map
            for (NsiMapping m : mappings) {
                initialReserveMappings.remove(m.getNsiConnectionId());
            }
            // we then add all the remaining reserve mappings
            mappings.addAll(initialReserveMappings.values());

            log.debug("added all mappings: " + mappings.size());
        } else {

            for (String connId : query.getConnectionId()) {
                log.info("finding by connId : {}", connId);
                Optional<NsiMapping> m = nsiRepo.findByNsiConnectionId(connId);
                if (m.isPresent()) {
                    mappings.add(m.get());
                } else {
                    if (initialReserveMappings.containsKey(connId)) {
                        mappings.add(initialReserveMappings.get(connId));
                    }
                }
            }

            for (String gri : query.getGlobalReservationId()) {
                log.info("finding by gri : {}", gri);
                mappings.addAll(nsiRepo.findByNsiGri(gri));
            }
            log.debug("added by connection & gri: " + mappings.size());
        }

        Long resultId = 0L;
        for (NsiMapping mapping : mappings) {
            // this might be null if there's no in-flight reserve request, that's ok
            NsiRequest nsiRequest = nsiRequestManager.getInFlightRequest(mapping.getNsiConnectionId());

                    // log.debug("query result entry "+mapping.getNsiConnectionId()+" --- "+mapping.getOscarsConnectionId());
            QuerySummaryResultType qsrt = this.toQSRT(mapping, nsiRequest);
            if (qsrt != null) {
                qsrt.setResultId(resultId);
                qsct.getReservation().add(qsrt);
                resultId++;
            }
        }
        log.debug("returning results, total: " + resultId);
        return qsct;
    }

    public QueryRecursiveResultType toQRRT(NsiMapping mapping) throws NsiInternalException {
        Optional<Connection> mc = nsiMappingService.getMaybeOscarsConnection(mapping);
        if (mc.isEmpty()) {
            log.error("nsi mapping for nonexistent OSCARS connection " + mapping.getOscarsConnectionId());
            return null;
        }
        Connection c = mc.get();

        QueryRecursiveResultType qrrt = new QueryRecursiveResultType();
        qrrt.setConnectionId(mapping.getNsiConnectionId());

        QueryRecursiveResultCriteriaType qrrct = new QueryRecursiveResultCriteriaType();
        Schedule sch;
        if (c.getPhase().equals(Phase.HELD)) {
            sch = c.getHeld().getSchedule();
        } else {
            sch = c.getArchived().getSchedule();
        }
        qrrct.setSchedule(nsiMappingService.oscarsToNsiSchedule(sch));
        qrrct.setServiceType(NsiService.SERVICE_TYPE);
        qrrct.setVersion(mapping.getDataplaneVersion());
        Components cmp = getComponents(c);

        P2PServiceBaseType p2p = nsiMappingService.makeP2P(cmp, mapping);

        net.es.nsi.lib.soap.gen.nsi_2_0.services.point2point.ObjectFactory p2pof
                = new net.es.nsi.lib.soap.gen.nsi_2_0.services.point2point.ObjectFactory();

        qrrct.getAny().add(p2pof.createP2Ps(p2p));
        qrrt.getCriteria().add(qrrct);

        qrrt.setDescription(c.getDescription());
        qrrt.setGlobalReservationId(mapping.getNsiGri());
        qrrt.setRequesterNSA(mapping.getNsaId());
        ConnectionStatesType cst = nsiMappingService.makeConnectionStates(mapping, c);
        qrrt.setConnectionStates(cst);
        qrrt.setNotificationId(0L);
        return qrrt;
    }

    public QuerySummaryResultType toQSRT(NsiMapping mapping, NsiRequest request) throws NsiInternalException {
        Optional<Connection> mc = nsiMappingService.getMaybeOscarsConnection(mapping);
        QuerySummaryResultType qsrt = new QuerySummaryResultType();
        qsrt.setConnectionId(mapping.getNsiConnectionId());
        String description;
        ConnectionStatesType cst;
        if (mc.isEmpty()) {
            // if an OSCARS connection is not present, we should be in RESV_CHECKING
            if (request == null || request.getIncoming() == null) {
                description = "";
            } else {
                description = request.getIncoming().getDescription();
            }


            log.info("returning a placeholder for "+mapping.getNsiConnectionId());
            cst = new ConnectionStatesType();
            cst.setProvisionState(mapping.getProvisionState());
            cst.setLifecycleState(mapping.getLifecycleState());
            cst.setReservationState(mapping.getReservationState());
            DataPlaneStatusType dst = new DataPlaneStatusType();
            dst.setVersion(mapping.getDataplaneVersion());
            dst.setActive(false);
            dst.setVersionConsistent(true);
            cst.setDataPlaneStatus(dst);

        } else {
            Connection c = mc.get();
            Schedule sch;

            if (c.getPhase().equals(Phase.ARCHIVED)) {
                // if this is archived, return it only the last modified date was 24 hours ago or less
                int yesterday = (int) Instant.now().minus(24L, ChronoUnit.HOURS).getEpochSecond();
                if (c.getLast_modified() > yesterday) {
                    return null;
                }
            }
            // if this is HELD, it is not committed yet & we should not return any criteria
            if (!c.getPhase().equals(Phase.HELD)) {
                sch = c.getArchived().getSchedule();
                QuerySummaryResultCriteriaType qsrct = new QuerySummaryResultCriteriaType();
                qsrct.setSchedule(nsiMappingService.oscarsToNsiSchedule(sch));
                Components cmp = getComponents(c);
                P2PServiceBaseType p2p = nsiMappingService.makeP2P(cmp, mapping);
                net.es.nsi.lib.soap.gen.nsi_2_0.services.point2point.ObjectFactory p2pof = new ObjectFactory();
                qsrct.getAny().add(p2pof.createP2Ps(p2p));
                qsrct.setServiceType(NsiService.SERVICE_TYPE);
                qsrct.setVersion(mapping.getDataplaneVersion());
                qsrt.getCriteria().add(qsrct);
            }

            description = c.getDescription();


            cst = nsiMappingService.makeConnectionStates(mapping, c);
        }


        qsrt.setDescription(description);
        qsrt.setGlobalReservationId(mapping.getNsiGri());
        qsrt.setRequesterNSA(mapping.getNsaId());
        qsrt.setConnectionStates(cst);
        qsrt.setNotificationId(Long.valueOf(mapping.getNotificationId()));
        return qsrt;
    }



    private static Components getComponents(Connection c) throws NsiInternalException {
        if (c.getReserved() != null) {
            return c.getReserved().getCmp();
        } else if (c.getArchived() != null) {
            return c.getArchived().getCmp();
        } else if (c.getHeld() != null) {
            return c.getHeld().getCmp();
        } else {
            throw new NsiInternalException("Internal error", NsiErrors.NRM_ERROR);
        }
    }
}
