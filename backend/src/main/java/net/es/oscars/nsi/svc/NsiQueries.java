package net.es.oscars.nsi.svc;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.*;
import net.es.nsi.lib.soap.gen.nsi_2_0.services.point2point.ObjectFactory;
import net.es.nsi.lib.soap.gen.nsi_2_0.services.point2point.P2PServiceBaseType;
import net.es.oscars.app.exc.NsiInternalException;
import net.es.oscars.nsi.beans.NsiErrors;
import net.es.oscars.nsi.db.NsiMappingRepository;
import net.es.oscars.nsi.ent.NsiMapping;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.Phase;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@Slf4j
public class NsiQueries {

    private final NsiMappingRepository nsiRepo;
    private final NsiMappingService nsiMappingService;

    public NsiQueries(NsiMappingRepository nsiRepo, NsiMappingService nsiMappingService) {
        this.nsiRepo = nsiRepo;
        this.nsiMappingService = nsiMappingService;
    }

    @Transactional
    public QueryRecursiveConfirmedType queryRecursive(QueryType query) throws NsiInternalException {
        QueryRecursiveConfirmedType qrct = new QueryRecursiveConfirmedType();

        Set<NsiMapping> mappings = new HashSet<>();
        for (String connId : query.getConnectionId()) {
            nsiRepo.findByNsiConnectionId(connId).ifPresent(mappings::add);
        }
        for (String gri : query.getGlobalReservationId()) {
            mappings.addAll(nsiRepo.findByNsiGri(gri));
        }

        Long resultId = 0L;
        List<NsiMapping> invalidMappings = new ArrayList<>();
        for (NsiMapping mapping : mappings) {
            QueryRecursiveResultType qrrt = this.toQRRT(mapping);
            if (qrrt != null) {
                qrrt.setResultId(resultId);
                qrct.getReservation().add(qrrt);
                resultId++;
            }

        }
        nsiRepo.deleteAll(invalidMappings);
        return qrct;
    }


    @Transactional
    public QuerySummaryConfirmedType querySummary(QueryType query) throws NsiInternalException {
        QuerySummaryConfirmedType qsct = new QuerySummaryConfirmedType();

        qsct.setLastModified(nsiMappingService.getCalendar(Instant.now()));

        if (query.getIfModifiedSince() != null) {
            throw new NsiInternalException("IMS not supported yet", NsiErrors.UNIMPLEMENTED);
        }

        Set<NsiMapping> mappings = new HashSet<>();
        if (query.getConnectionId().isEmpty() && query.getGlobalReservationId().isEmpty()) {
            // empty query = find all
            mappings.addAll(nsiRepo.findAll());
            log.debug("added all mappings: " + mappings.size());
        } else {
            for (String connId : query.getConnectionId()) {
                // log.debug("added mapping for nsi connId: "+connId);
                nsiRepo.findByNsiConnectionId(connId).ifPresent(mappings::add);
            }
            for (String gri : query.getGlobalReservationId()) {
                // log.debug("added mapping for gri : "+gri);
                mappings.addAll(nsiRepo.findByNsiGri(gri));
            }
            log.debug("added by connection & gri: " + mappings.size());
        }

        Long resultId = 0L;
        List<NsiMapping> invalidMappings = new ArrayList<>();
        for (NsiMapping mapping : mappings) {
            // log.debug("query result entry "+mapping.getNsiConnectionId()+" --- "+mapping.getOscarsConnectionId());
            QuerySummaryResultType qsrt = this.toQSRT(mapping);
            if (qsrt != null) {
                qsrt.setResultId(resultId);
                qsct.getReservation().add(qsrt);
                resultId++;
            }
        }
        nsiRepo.deleteAll(invalidMappings);
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

    public QuerySummaryResultType toQSRT(NsiMapping mapping) throws NsiInternalException {
        Optional<Connection> mc = nsiMappingService.getMaybeOscarsConnection(mapping);
        if (mc.isEmpty()) {
            log.error("nsi mapping for nonexistent OSCARS connection " + mapping.getOscarsConnectionId());
            return null;
        }
        Connection c = mc.get();
        if (c.getPhase().equals(Phase.ARCHIVED)) {
            // if this is archived return it only the last modified date was 24 hours ago or less
            int yesterday = (int) Instant.now().minus(24L, ChronoUnit.HOURS).getEpochSecond();
            if (c.getLast_modified() > yesterday) {
                return null;
            }
        }

        QuerySummaryResultType qsrt = new QuerySummaryResultType();
        qsrt.setConnectionId(mapping.getNsiConnectionId());

        QuerySummaryResultCriteriaType qsrct = new QuerySummaryResultCriteriaType();
        Schedule sch;
        if (c.getPhase().equals(Phase.HELD)) {
            sch = c.getHeld().getSchedule();
        } else {
            sch = c.getArchived().getSchedule();
        }
        qsrct.setSchedule(nsiMappingService.oscarsToNsiSchedule(sch));
        qsrct.setServiceType(NsiService.SERVICE_TYPE);
        qsrct.setVersion(mapping.getDataplaneVersion());

        Components cmp = getComponents(c);
        P2PServiceBaseType p2p = nsiMappingService.makeP2P(cmp, mapping);

        net.es.nsi.lib.soap.gen.nsi_2_0.services.point2point.ObjectFactory p2pof
                = new ObjectFactory();

        qsrct.getAny().add(p2pof.createP2Ps(p2p));
        qsrt.getCriteria().add(qsrct);

        qsrt.setDescription(c.getDescription());
        qsrt.setGlobalReservationId(mapping.getNsiGri());
        qsrt.setRequesterNSA(mapping.getNsaId());
        ConnectionStatesType cst = nsiMappingService.makeConnectionStates(mapping, c);
        qsrt.setConnectionStates(cst);
        qsrt.setNotificationId(0L);
        return qsrt;
    }



    private static Components getComponents(Connection c) throws NsiInternalException {

        if (c.getPhase().equals(Phase.RESERVED)) {
            return c.getReserved().getCmp();

        } else if (c.getPhase().equals(Phase.ARCHIVED)) {
            return c.getArchived().getCmp();

        } else if (c.getPhase().equals(Phase.HELD)) {
            return c.getHeld().getCmp();
        } else {
            throw new NsiInternalException("Internal error", NsiErrors.NRM_ERROR);
        }

    }
}
