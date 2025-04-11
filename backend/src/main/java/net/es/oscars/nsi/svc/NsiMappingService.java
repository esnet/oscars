package net.es.oscars.nsi.svc;

import jakarta.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.*;
import net.es.nsi.lib.soap.gen.nsi_2_0.services.point2point.P2PServiceBaseType;
import net.es.nsi.lib.soap.gen.nsi_2_0.services.types.DirectionalityType;
import net.es.nsi.lib.soap.gen.nsi_2_0.services.types.OrderedStpType;
import net.es.nsi.lib.soap.gen.nsi_2_0.services.types.StpListType;
import net.es.nsi.lib.soap.gen.nsi_2_0.services.types.TypeValueType;
import net.es.oscars.app.exc.*;
import net.es.oscars.model.Interval;
import net.es.oscars.nsi.beans.NsiErrors;
import net.es.oscars.nsi.db.NsiMappingRepository;
import net.es.oscars.nsi.ent.NsiMapping;
import net.es.oscars.pce.PceService;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.State;
import net.es.oscars.resv.svc.ConnUtils;
import net.es.oscars.resv.svc.ResvLibrary;
import net.es.oscars.resv.svc.ResvService;
import net.es.oscars.topo.beans.PortBwVlan;
import net.es.oscars.topo.beans.TopoUrn;
import net.es.oscars.topo.enums.UrnType;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.oscars.web.beans.PceMode;
import net.es.oscars.web.beans.PceRequest;
import net.es.oscars.web.beans.PceResponse;
import net.es.oscars.web.simple.Fixture;
import net.es.oscars.web.simple.Junction;
import net.es.oscars.web.simple.Pipe;
import net.es.topo.common.model.oscars1.IntRange;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@Slf4j
public class NsiMappingService {

    private final ConnectionRepository connRepo;
    private final NsiMappingRepository nsiRepo;
    private final TopologyStore topologyStore;
    private final ResvService resvService;
    private final PceService pceService;

    @Value("${nsi.strict-policing:true}")
    private boolean strictPolicing;

    @Value("${nml.topo-id}")
    private String topoId;

    @Value("${nml.topo-name}")
    private String topoName;


    @Autowired
    private ConnUtils connUtils;
    public NsiMappingService(ConnectionRepository connRepo, NsiMappingRepository nsiRepo, TopologyStore topologyStore, ResvService resvService, PceService pceService) {
        this.connRepo = connRepo;
        this.nsiRepo = nsiRepo;
        this.topologyStore = topologyStore;
        this.resvService = resvService;
        this.pceService = pceService;
    }

    // methods moved here to avoid transactional self-invocation

    @Transactional
    public Connection getOscarsConnection(NsiMapping mapping) throws NsiMappingException {
        // log.debug("getting oscars connection for "+mapping.getOscarsConnectionId());
        Optional<Connection> c = connRepo.findByConnectionId(mapping.getOscarsConnectionId());
        if (c.isEmpty()) {
            throw new NsiMappingException("OSCARS connection not found", NsiErrors.NO_SCH_ERROR);
        } else {
            return c.get();
        }
    }

    @Transactional
    public Optional<Connection> getMaybeOscarsConnection(NsiMapping mapping) {
        // log.debug("getting oscars connection for "+mapping.getOscarsConnectionId());
        return connRepo.findByConnectionId(mapping.getOscarsConnectionId());
    }

    @Transactional
    public NsiMapping save(NsiMapping mapping) {
        return nsiRepo.save(mapping);
    }

    @Transactional
    public Optional<NsiMapping> getMappingForOscarsId(String oscarsConnectionId) {
        List<NsiMapping> mappings = nsiRepo.findByOscarsConnectionId(oscarsConnectionId);
        if (mappings.isEmpty()) {
            return Optional.empty();
        } else if (mappings.size() > 1) {
            // delete any extra mappings
            for (int i = 1; i < mappings.size(); i++) {
                nsiRepo.delete(mappings.get(i));
            }
        }
        return Optional.of(mappings.getFirst());
    }

    /* db funcs */
    public NsiMapping getMapping(String nsiConnectionId) throws NsiMappingException {
        if (nsiConnectionId == null || nsiConnectionId.isEmpty()) {
            throw new NsiMappingException("null or blank connection id! " + nsiConnectionId, NsiErrors.MISSING_PARAM_ERROR);
        }
        Optional<NsiMapping> mapping = nsiRepo.findByNsiConnectionId(nsiConnectionId);
        if (mapping.isEmpty()) {
            throw new NsiMappingException("unknown connection id " + nsiConnectionId, NsiErrors.NO_SCH_ERROR);
        } else {
            return mapping.get();
        }
    }
    public boolean hasNsiMapping(String nsiConnectionId) throws NsiMappingException {
        if (nsiConnectionId == null || nsiConnectionId.isEmpty()) {
            throw new NsiMappingException("null or blank connection id! " + nsiConnectionId, NsiErrors.MISSING_PARAM_ERROR);
        }
        Optional<NsiMapping> mapping = nsiRepo.findByNsiConnectionId(nsiConnectionId);
        return mapping.isPresent();
    }


    public NsiMapping newMapping(String nsiConnectionId, String nsiGri, String nsaId, Integer version) throws ServiceException {
        if (nsiConnectionId == null || nsiConnectionId.isEmpty()) {
            throw new ServiceException("null nsi connection id");
        }
        if (nsiGri == null) {
            nsiGri = "";
        }
        if (nsiRepo.findByNsiConnectionId(nsiConnectionId).isPresent()) {
            throw new ServiceException("previously used nsi connection id! " + nsiConnectionId);
        }
        String oscarsConnectionId = connUtils.genUniqueConnectionId();

        NsiMapping mapping = NsiMapping.builder()
                .nsiConnectionId(nsiConnectionId)
                .nsiGri(nsiGri)
                .oscarsConnectionId(oscarsConnectionId)
                .dataplaneVersion(version)
                .nsaId(nsaId)
                .lifecycleState(LifecycleStateEnumType.CREATED)
                .provisionState(ProvisionStateEnumType.RELEASED)
                .reservationState(ReservationStateEnumType.RESERVE_START)
                .build();
        log.info("added an NSI mapping: "+nsiConnectionId+" --> "+oscarsConnectionId);
        return mapping;
    }

    public P2PServiceBaseType getP2PService(ReserveType rt) throws NsiInternalException {
        ReservationRequestCriteriaType crit = rt.getCriteria();
        P2PServiceBaseType p2pt = null;
        for (Object o : crit.getAny()) {
            if (o instanceof P2PServiceBaseType) {
                p2pt = (P2PServiceBaseType) o;
            } else {
                try {

                    @SuppressWarnings("unchecked") JAXBElement<P2PServiceBaseType> payload
                            = (JAXBElement<P2PServiceBaseType>) o;
                    p2pt = payload.getValue();
                } catch (ClassCastException ex) {
                    log.error(ex.getMessage(), ex);
                    p2pt = null;
                }
            }
        }

        if (p2pt == null) {
            throw new NsiInternalException("Missing P2PServiceBaseType element!", NsiErrors.MISSING_PARAM_ERROR);
        }
        return p2pt;
    }


    public List<Pipe> pipesFor(Interval interval, Integer mbps,
                               List<Junction> junctions, List<String> include)
            throws NsiValidationException, NsiInternalException {
        if (junctions.isEmpty()) {
            throw new NsiValidationException("no junctions - at least one required", NsiErrors.PCE_ERROR);
        } else if (junctions.size() == 1) {
            // no pipes required
            return new ArrayList<>();
        } else if (junctions.size() > 2) {
            throw new NsiValidationException("Too many junctions for NSI reserve", NsiErrors.PCE_ERROR);
        }
        // first element of ero should be the first device...
        if (include.isEmpty()) {
            include.add(junctions.get(0).getDevice());
        } else if (!include.get(0).equals(junctions.get(0).getDevice())) {
            include.add(0, junctions.get(0).getDevice());
        }
        // last element of ero should be the last device.
        if (!include.get(include.size() - 1).equals(junctions.get(1).getDevice())) {
            include.add(junctions.get(1).getDevice());
        }

        try {
            PceRequest request = PceRequest.builder()
                    .a(junctions.get(0).getDevice())
                    .z(junctions.get(1).getDevice())
                    .interval(interval)
                    .azBw(mbps)
                    .zaBw(mbps)
                    .include(include)
                    .build();
            PceResponse response = pceService.calculatePaths(request);
            if (response.getFits() != null) {
                List<String> ero = new ArrayList<>();
                for (EroHop hop : response.getFits().getAzEro()) {
                    ero.add(hop.getUrn());
                }
                Pipe p = Pipe.builder()
                        .mbps(mbps)
                        .ero(ero)
                        .protect(true)
                        .a(junctions.get(0).getDevice())
                        .z(junctions.get(1).getDevice())
                        .pceMode(PceMode.BEST)
                        .build();
                List<Pipe> result = new ArrayList<>();
                result.add(p);
                return result;
            } else {
                throw new NsiValidationException("Path not found", NsiErrors.PCE_ERROR);
            }

        } catch (PCEException ex) {
            throw new NsiInternalException(ex.getMessage(), NsiErrors.PCE_ERROR);

        }

    }

    public Pair<List<Fixture>, List<Junction>> fixturesAndJunctionsFor(P2PServiceBaseType p2p, Interval interval)
            throws NsiInternalException, NsiValidationException {
        String src = p2p.getSourceSTP();
        String dst = p2p.getDestSTP();
        String in_a = this.internalUrnFromStp(src);
        String in_z = this.internalUrnFromStp(dst);
        long mbps = p2p.getCapacity();

        TopoUrn a_urn = topologyStore.getTopoUrnMap().get(in_a);
        if (a_urn == null) {
            log.error("could not find stp " + src + ", converted to " + in_a);
            throw new NsiValidationException("src STP not found in topology " + src, NsiErrors.LOOKUP_ERROR);
        } else if (!a_urn.getUrnType().equals(UrnType.PORT)) {
            throw new NsiValidationException("src STPs is not a port " + src, NsiErrors.LOOKUP_ERROR);
        }

        TopoUrn z_urn = topologyStore.getTopoUrnMap().get(in_z);
        if (z_urn == null) {
            log.error("could not find stp " + src + ", converted to " + in_z);
            throw new NsiValidationException("dst STP not found in topology " + dst, NsiErrors.LOOKUP_ERROR);
        } else if (!z_urn.getUrnType().equals(UrnType.PORT)) {
            throw new NsiValidationException("dst STP is not a port " + dst, NsiErrors.LOOKUP_ERROR);
        }
        List<Junction> junctions = new ArrayList<>();
        Junction aJ = Junction.builder().device(a_urn.getDevice().getUrn()).build();
        junctions.add(aJ);
        Junction zJ = aJ;
        if (!a_urn.getDevice().getUrn().equals(z_urn.getDevice().getUrn())) {
            zJ = Junction.builder()
                    .device(z_urn.getDevice().getUrn())
                    .build();
            junctions.add(zJ);
        }

        Set<IntRange> aVlansSet = new HashSet<>();
        IntRange aRange = this.getVlanRange(src);
        aVlansSet.add(aRange);

        Set<IntRange> zVlansSet = new HashSet<>();
        IntRange zRange = this.getVlanRange(dst);
        zVlansSet.add(zRange);

        // check if they're trying a
        if (a_urn.getPort().getUrn().equals(z_urn.getPort().getUrn())) {
            if (aRange.getFloor().equals(aRange.getCeiling())) {
                if (zRange.getFloor().equals(zRange.getCeiling())) {
                    if (aRange.getFloor().equals(zRange.getFloor())) {
                        throw new NsiValidationException("Cannot provision same port.vlan for both src and dst", NsiErrors.MSG_ERROR);
                    }
                }
            }
        }


        Map<String, PortBwVlan> available = resvService.available(interval, null);
        PortBwVlan aAvail = available.get(a_urn.getUrn());
        PortBwVlan zAvail = available.get(z_urn.getUrn());

        if (aAvail.getIngressBandwidth() < mbps) {
            throw new NsiValidationException("bandwidth unavailable for " + src, NsiErrors.UNAVAIL_ERROR);
        } else if (aAvail.getEgressBandwidth() < mbps) {
            throw new NsiValidationException("bandwidth unavailable for " + src, NsiErrors.UNAVAIL_ERROR);

        } else if (zAvail.getIngressBandwidth() < mbps) {
            throw new NsiValidationException("bandwidth unavailable for " + dst, NsiErrors.UNAVAIL_ERROR);
        } else if (zAvail.getEgressBandwidth() < mbps) {
            throw new NsiValidationException("bandwidth unavailable for " + dst, NsiErrors.UNAVAIL_ERROR);
        }

        Map<String, Set<IntRange>> requestedVlans = new HashMap<>();
        requestedVlans.put(a_urn.getPort().getUrn() + "#A", aVlansSet);
        requestedVlans.put(z_urn.getPort().getUrn() + "#Z", zVlansSet);

        Map<String, Set<IntRange>> availVlans = new HashMap<>();
        availVlans.put(a_urn.getPort().getUrn(), aAvail.getVlanRanges());
        availVlans.put(z_urn.getPort().getUrn(), zAvail.getVlanRanges());

        Map<String, Integer> vlans = ResvLibrary.decideIdentifier(requestedVlans, availVlans);
        Integer aVlanId = vlans.get(a_urn.getPort().getUrn() + "#A");
        Integer zVlanId = vlans.get(z_urn.getPort().getUrn() + "#Z");

        if (aVlanId == null) {
            throw new NsiValidationException("vlan(s) unavailable for " + src, NsiErrors.UNAVAIL_ERROR);

        } else if (zVlanId == null) {
            throw new NsiValidationException("vlan(s) unavailable for " + dst, NsiErrors.UNAVAIL_ERROR);
        }

        Fixture aF = Fixture.builder()
                .junction(aJ.getDevice())
                .port(a_urn.getPort().getUrn())
                .mbps((int) mbps)
                .strict(strictPolicing)
                .vlan(aVlanId)
                .build();
        Fixture zF = Fixture.builder()
                .junction(zJ.getDevice())
                .port(z_urn.getPort().getUrn())
                .mbps((int) mbps)
                .strict(strictPolicing)
                .vlan(zVlanId)
                .build();

        List<Fixture> fixtures = new ArrayList<>();
        fixtures.add(aF);
        fixtures.add(zF);

        return new ImmutablePair<>(fixtures, junctions);
    }

    public IntRange getVlanRange(String stp) throws NsiValidationException {
        this.validateStp(stp);
        String[] stpParts = StringUtils.split(stp, "\\?");
        if (stpParts.length < 2) {
            throw new NsiValidationException("no labels (VLAN or otherwise) for stp " + stp, NsiErrors.LOOKUP_ERROR);
        }
        IntRange vlanRange = IntRange.builder().floor(-1).ceiling(-1).build();

        for (int i = 1; i < stpParts.length; i++) {
            String labelAndValue = stpParts[i];
            String[] lvParts = StringUtils.split(labelAndValue, "=");
            if (lvParts == null || lvParts.length == 0) {
                log.info("empty label-value part");
            } else if (lvParts.length == 1) {
                log.info("just a label, ignoring: " + lvParts[0]);
            } else if (lvParts.length > 2) {
                log.info("label-value parse error: [" + labelAndValue + "]");
            } else {
                // lvParts.length == 2
                String label = lvParts[0];
                String value = lvParts[1];
                if (label.equals("vlan")) {
                    String[] ranges = value.split(",");
                    for (String range : ranges) {
                        String[] parts = range.split("-");

                        try {
                            if (parts.length == 1) {
                                Integer vlan = Integer.valueOf(parts[0]);
                                vlanRange.setFloor(vlan);
                                vlanRange.setCeiling(vlan);
                                log.info("vlan range for " + stp + " : " + vlan);
                                return vlanRange;

                            } else if (parts.length == 2) {
                                Integer f = Integer.valueOf(parts[0]);
                                Integer c = Integer.valueOf(parts[1]);
                                vlanRange.setFloor(f);
                                vlanRange.setCeiling(c);
                                log.info("vlan range for " + stp + " : " + f + " - " + c);
                                return vlanRange;

                            }
                        } catch (NumberFormatException ex) {
                            throw new NsiValidationException("Could not parse vlan id parameter", NsiErrors.MSG_ERROR);
                        }
                    }
                } else {
                    log.info("label-value: " + lvParts[0] + " = " + lvParts[1]);
                }
            }
        }
        throw new NsiValidationException("could not locate VLAN range for STP " + stp, NsiErrors.LOOKUP_ERROR);
    }

    public void validateStp(String stp) throws NsiValidationException {
        if (stp == null) {
            log.error("null STP");
            throw new NsiValidationException("null STP", NsiErrors.LOOKUP_ERROR);
        } else if (stp.isEmpty()) {
            log.error("empty STP");
            throw new NsiValidationException("empty STP string", NsiErrors.LOOKUP_ERROR);
        }
        if (!stp.startsWith("urn:ogf:network:")) {
            throw new NsiValidationException("STP does not start with 'urn:ogf:network:' :" + stp, NsiErrors.LOOKUP_ERROR);
        }

    }

    public String internalUrnFromStp(String stp) throws NsiInternalException {
        String[] stpParts = StringUtils.split(stp, "\\?");
        return internalUrnFromNsi(stpParts[0]);
    }

    public Interval nsiToOscarsSchedule(ScheduleType schedule) {
        Instant beg = Instant.now().plus(30, ChronoUnit.SECONDS);
        Instant end = beg.plus(Duration.of(24, ChronoUnit.HOURS));

        if (schedule.getStartTime() != null) {
            XMLGregorianCalendar xst = schedule.getStartTime().getValue();
            beg = xst.toGregorianCalendar().toInstant();
        }
        if (schedule.getEndTime() != null) {
            XMLGregorianCalendar xet = schedule.getEndTime().getValue();
            end = xet.toGregorianCalendar().toInstant();
        }

        return Interval.builder()
                .beginning(beg)
                .ending(end)
                .build();
    }

    public ScheduleType oscarsToNsiSchedule(Schedule sch) throws NsiInternalException {

        XMLGregorianCalendar xgb = this.getCalendar(sch.getBeginning());
        XMLGregorianCalendar xge = this.getCalendar(sch.getEnding());

        net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ObjectFactory of =
                new net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ObjectFactory();

        ScheduleType st = of.createScheduleType();

        st.setStartTime(of.createScheduleTypeStartTime(xgb));
        st.setEndTime(of.createScheduleTypeEndTime(xge));
        return st;

    }

    public XMLGregorianCalendar getCalendar(Instant when) throws NsiInternalException {
        try {

            ZonedDateTime zd = ZonedDateTime.ofInstant(when, ZoneId.systemDefault());
            GregorianCalendar c = GregorianCalendar.from(zd);
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        } catch (DatatypeConfigurationException ex) {
            log.error(ex.getMessage(), ex);
            throw new NsiInternalException(ex.getMessage(), NsiErrors.NRM_ERROR);
        }
    }


    public String nsiUrnFromInternal(String internalUrn) {
        String prefix = topoId + ":";
        return prefix + internalUrn.replace("/", "_") + ":+";

    }

    private String internalUrnFromNsi(String nsiUrn) throws NsiInternalException {
        String prefix = topoId + ":";

        String stripped = nsiUrn.replace(prefix, "")
                .replace("_", "/")
                .replace(":+", "");

        String[] parts = stripped.split(":");
        if (parts.length == 2 || parts.length == 3) {
            return parts[0] + ":" + parts[1];

        } else {
            throw new NsiInternalException("Error retrieving internal URN from STP " + nsiUrn, NsiErrors.NRM_ERROR);
        }

    }


    public P2PServiceBaseType makeP2P(Components cmp, NsiMapping mapping) {

        P2PServiceBaseType p2p = new P2PServiceBaseType();

        TypeValueType tvt = new TypeValueType();
        tvt.setType("oscarsId");
        tvt.setValue(mapping.getOscarsConnectionId());
        p2p.getParameter().add(tvt);

        VlanFixture a = cmp.getFixtures().getFirst();
        String srcStp = this.nsiUrnFromInternal(a.getPortUrn()) + "?vlan=" + a.getVlan().getVlanId();
        if (mapping.getSrc() != null) {
            String[] stpParts = StringUtils.split(mapping.getSrc(), "\\?");
            srcStp = stpParts[0] + "?vlan=" + a.getVlan().getVlanId();
        }

        VlanFixture z = cmp.getFixtures().get(1);
        String dstStp = this.nsiUrnFromInternal(z.getPortUrn()) + "?vlan=" + z.getVlan().getVlanId();
        if (mapping.getDst() != null) {
            String[] stpParts = StringUtils.split(mapping.getDst(), "\\?");
            dstStp = stpParts[0] + "?vlan=" + z.getVlan().getVlanId();
        }

        List<String> strEro = new ArrayList<>();
        if (cmp.getPipes() == null || cmp.getPipes().isEmpty()) {
            strEro.add(srcStp);
            strEro.add(dstStp);
        } else {
            VlanPipe p = cmp.getPipes().get(0);
            strEro.add(srcStp);
            for (int i = 0; i < p.getAzERO().size(); i++) {
                // skip devices in NSI ERO
                if (i % 3 != 0) {
                    strEro.add(this.nsiUrnFromInternal(p.getAzERO().get(i).getUrn()));
                }
            }
            strEro.add(dstStp);
        }

        StpListType ero = new StpListType();
        for (int i = 0; i < strEro.size(); i++) {
            OrderedStpType ostp = new OrderedStpType();
            ostp.setStp(strEro.get(i));
            ostp.setOrder(i);

            ero.getOrderedSTP().add(ostp);

        }

        p2p.setSourceSTP(srcStp);
        p2p.setDestSTP(dstStp);
        p2p.setCapacity(a.getIngressBandwidth());
        p2p.setEro(ero);
        p2p.setDirectionality(DirectionalityType.BIDIRECTIONAL);
        p2p.setSymmetricPath(true);
        return p2p;
    }

    public ConnectionStatesType makeConnectionStates(NsiMapping mapping, Connection c) {
        DataPlaneStatusType dst = new DataPlaneStatusType();
        dst.setActive(false);
        if (c != null) {
            if (c.getState().equals(State.ACTIVE)) {
                dst.setActive(true);
            }
        }
        dst.setVersion(mapping.getDataplaneVersion());
        dst.setVersionConsistent(true);

        ConnectionStatesType cst = new ConnectionStatesType();
        cst.setDataPlaneStatus(dst);
        cst.setLifecycleState(mapping.getLifecycleState());
        cst.setProvisionState(mapping.getProvisionState());
        cst.setReservationState(mapping.getReservationState());
        if (cst.getLifecycleState().equals(LifecycleStateEnumType.TERMINATED)) {
            dst.setActive(false);
        }
        return cst;
    }


}
