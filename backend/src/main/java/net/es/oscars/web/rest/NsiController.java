package net.es.oscars.web.rest;

import jakarta.xml.bind.JAXB;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.dto.esdb.gql.GraphqlEsdbNsiPeering;
import net.es.oscars.dto.esdb.gql.GraphqlEsdbVlanRange;
import net.es.oscars.esdb.EsdbCache;
import net.es.oscars.topo.nsi.Constants;
import net.es.oscars.topo.nsi.NsaProperties;
import net.es.oscars.topo.svc.TopoGenerator;
import net.es.topo.common.model.nsi.nsa.*;
import net.es.topo.common.model.nsi.topology.*;
import net.es.topo.common.model.oscars1.OscarsOneDevice;
import net.es.topo.common.model.oscars1.OscarsOnePort;
import net.es.topo.common.model.oscars1.OscarsOneTopo;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static net.es.oscars.topo.nsi.Constants.*;

@Slf4j
@Controller
public class NsiController {


    private final static net.es.topo.common.model.nsi.topology.ObjectFactory nmlFactory = new net.es.topo.common.model.nsi.topology.ObjectFactory();
    private final static net.es.topo.common.model.nsi.nsa.ObjectFactory nsaFactory = new net.es.topo.common.model.nsi.nsa.ObjectFactory();

    private final NsaProperties nsaProperties;
    private final TopoGenerator topoGenerator;
    private final Constants constants;
    private final EsdbCache esdbCache;

    public NsiController(NsaProperties nsaProperties, TopoGenerator topoGenerator, Constants constants, EsdbCache esdbCache) {
        this.nsaProperties = nsaProperties;
        this.topoGenerator = topoGenerator;
        this.constants = constants;
        this.esdbCache = esdbCache;
    }

    /**
     * Generate the ESnet NSI topology description and makes it available as an XML based API endpoint
     *
     * @return returns an XML NML description of the NSI topology
     * @throws DatatypeConfigurationException
     */
    @RequestMapping(value = NML_TOPO_URI, produces = {MediaType.APPLICATION_XML_VALUE}, headers = "Accept=application/xml")
    public ResponseEntity<?> getNsiTopology() throws DatatypeConfigurationException {
        OscarsOneTopo oscarsOneTopo = null;
        try {
            oscarsOneTopo = topoGenerator.generate();
        } catch (IOException e) {
            log.error("Unable to load OSCARS topology", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        // determine start and end date
        Instant now = Instant.now();
        Instant expiration = now.plus(1, ChronoUnit.DAYS);

        GregorianCalendar gCal = new GregorianCalendar();
        gCal.setTimeInMillis(now.toEpochMilli());
        XMLGregorianCalendar startDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(gCal);

        gCal.setTimeInMillis(expiration.toEpochMilli());
        XMLGregorianCalendar endDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(gCal);

        // create new NmlTopology object model
        NmlTopologyType nsiTopology = nmlFactory.createNmlTopologyType();

        // init topo
        nsiTopology.setId(constants.PROVIDER_ID());
        nsiTopology.setVersion(startDate);
        nsiTopology.setName(nsaProperties.getProvider());

        // set topology lifetime
        NmlLifeTimeType lifeTime = new NmlLifeTimeType();
        lifeTime.setStart(startDate);
        lifeTime.setEnd(endDate);
        nsiTopology.setLifetime(lifeTime);

        // get esdb nsi peers and equipment
        Instant refreshIfOlderThan = Instant.now().minus(30, ChronoUnit.SECONDS);
        List<GraphqlEsdbNsiPeering> nsiPeerings = esdbCache.getNsiPeering();

        if (nsiPeerings == null ||  oscarsOneTopo == null) {
            log.error("Could not obtain required information");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not obtain required information");
        }

        String ID_PREFIX = constants.PROVIDER_ID() + ID_INFORMATION_TYPE_NONE;

        // create and add BiDirectionalPorts
        List<Pair<String, String>> portInOutIds = new ArrayList<>();
        Map<String, GraphqlEsdbNsiPeering> portInIdToPeering = new HashMap<>();
        // create and add bidirectional I/O ports to NSI topology
        for (GraphqlEsdbNsiPeering entry: nsiPeerings) {
            String portName = entry.getEquipmentInterface().getInterfaceName();
            String deviceName = entry.getEquipmentInterface().getDevice().getName();

            boolean inOscars = this.existsInOscars(deviceName, portName, oscarsOneTopo);
            if (!inOscars) {
                log.error("Could not locate " + deviceName + ":" + portName + " in OSCARS topology, skipping");
                continue;
            }
            portName = portName.replace("/", "_");

            String localName = ID_NONE;
            if (entry.getLocalName() != null) {
                localName = entry.getLocalName();
            }
            String biPortId = ID_PREFIX
                    + deviceName + ID_SEPARATOR
                    + portName + ID_SEPARATOR
                    + localName;
            String portInId = biPortId + ID_SEPARATOR + ID_PORT_IN;
            String portOutId = biPortId + ID_SEPARATOR + ID_PORT_OUT;

            // map the peering to the port id so we can retrieve it later
            portInIdToPeering.put(portInId, entry);

            // create BiDirectionalPort and add it to the topo
            NmlBidirectionalPortType bidirectionalPort = new NmlBidirectionalPortType();
            bidirectionalPort.setId(biPortId);

            // create location for BiPort
            NmlLocationType location = new NmlLocationType();
            String locationId = ID_PREFIX
                    + "locations" + ID_SEPARATOR
                    + entry.getEquipmentInterface().getDevice().getLocation().getId();

            String locationName = entry.getEquipmentInterface().getDevice().getLocation().getShortName();
            location.setId(locationId);
            location.setName(locationName.toUpperCase(Locale.ROOT));

            String lat = entry.getEquipmentInterface().getDevice().getLocation().getLatitude();
            location.setLat(Float.parseFloat(lat));

            String lng = entry.getEquipmentInterface().getDevice().getLocation().getLongitude();
            location.setLong(Float.parseFloat(lng));

            // add port groups, ids, and cache ids for switching service
            NmlPortGroupType inPort = new NmlPortGroupType();
            NmlPortGroupType outPort = new NmlPortGroupType();
            inPort.setId(portInId);
            outPort.setId(portOutId);

            portInOutIds.add(Pair.of(portInId, portOutId));

            // add PortGroups to BidirectionalPort
            bidirectionalPort.getRest().add(nmlFactory.createPortGroup(inPort));
            bidirectionalPort.getRest().add(nmlFactory.createPortGroup(outPort));

            // add label to BiDirectionalPort
            bidirectionalPort.setLocation(location);

            // add BiDirectionalPort to topology
            nsiTopology.getGroup().add(bidirectionalPort);
        }

        // create switching service and add it to the NSI topology
        ServiceDefinitionType serviceDefinition = new ServiceDefinitionType();
        serviceDefinition.setId(constants.ID_SERVICE_DEFINITION());
        serviceDefinition.setServiceType(SERVICE_TYPE);
        serviceDefinition.setName(SERVICE_NAME);
        nsiTopology.getAny().add(nmlFactory.createServiceDefinition(serviceDefinition));

        NmlSwitchingServiceType switchingService = new NmlSwitchingServiceType();
        switchingService.setId(constants.ID_SERVICE_DOMAIN());
        switchingService.setLabelSwapping(true);
        switchingService.setEncoding(EthEncodingTypes.HTTP_SCHEMAS_OGF_ORG_NML_2012_10_ETHERNET.value());
        switchingService.setLabelType(EthLabelTypes.HTTP_SCHEMAS_OGF_ORG_NML_2012_10_ETHERNET_VLAN.value());

        NmlSwitchingServiceRelationType inPortRelation = new NmlSwitchingServiceRelationType();
        NmlSwitchingServiceRelationType outPortRelation = new NmlSwitchingServiceRelationType();
        inPortRelation.setType(NML_BASE_NAMESPACE + NML_BASE_TYPE_HAS_IN_PORT);
        outPortRelation.setType(NML_BASE_NAMESPACE + NML_BASE_TYPE_HAS_OUT_PORT);

        for (Pair<String, String> portInOutId : portInOutIds) {
            NmlPortGroupType switchInPort = new NmlPortGroupType();
            switchInPort.setId(portInOutId.getFirst());
            inPortRelation.getPortGroup().add(switchInPort);

            NmlPortGroupType switchOutPort = new NmlPortGroupType();
            switchOutPort.setId(portInOutId.getSecond());
            outPortRelation.getPortGroup().add(switchOutPort);
        }

        switchingService.getRelation().add(inPortRelation);
        switchingService.getRelation().add(outPortRelation);
        serviceDefinition = new ServiceDefinitionType();
        serviceDefinition.setId(constants.ID_SERVICE_DEFINITION());
        switchingService.getAny().add(nmlFactory.createServiceDefinition(serviceDefinition));

        NmlTopologyRelationType serviceRelation = new NmlTopologyRelationType();
        serviceRelation.setType(NML_BASE_NAMESPACE + NML_BASE_TYPE_HAS_SERVICE);
        serviceRelation.getService().add(switchingService);

        nsiTopology.getRelation().add(serviceRelation);

        // I/O port settings
        NmlTopologyRelationType topologyInRelation = new NmlTopologyRelationType();
        topologyInRelation.setType(NML_BASE_NAMESPACE + NML_BASE_TYPE_HAS_IN_PORT);
        NmlTopologyRelationType topologyOutRelation = new NmlTopologyRelationType();
        topologyOutRelation.setType(NML_BASE_NAMESPACE + NML_BASE_TYPE_HAS_OUT_PORT);

        for (Pair<String, String> portInOutId : portInOutIds) {
            String portInId = portInOutId.getFirst();
            String portOutId = portInOutId.getSecond();

            NmlPortGroupType inPortGroup = new NmlPortGroupType();
            NmlPortGroupType outPortGroup = new NmlPortGroupType();

            inPortGroup.setId(portInId);
            outPortGroup.setId(portOutId);

            inPortGroup.setEncoding(EthEncodingTypes.HTTP_SCHEMAS_OGF_ORG_NML_2012_10_ETHERNET.value());
            outPortGroup.setEncoding(EthEncodingTypes.HTTP_SCHEMAS_OGF_ORG_NML_2012_10_ETHERNET.value());

            if (!portInIdToPeering.containsKey(portInId)) {
                log.error("Could not locate peering for " + portInId);
                continue;
            }
            GraphqlEsdbNsiPeering peer = portInIdToPeering.get(portInId);

            // add vlan range label
            NmlLabelGroupType label = new NmlLabelGroupType();
            label.setLabeltype(EthLabelTypes.HTTP_SCHEMAS_OGF_ORG_NML_2012_10_ETHERNET_VLAN.value());
            StringBuilder vlanLabels = new StringBuilder();
            for (int j = 0; j < peer.getAllowedVlanRanges().size(); j++) {
                GraphqlEsdbVlanRange range = peer.getAllowedVlanRanges().get(j);
                vlanLabels.append(range.getBeginRange());
                if (range.getEndRange() != null && range.getEndRange() != 0) {
                    vlanLabels.append("-").append(range.getEndRange());
                }
                if (j + 1 < peer.getAllowedVlanRanges().size()) {
                    vlanLabels.append(",");
                }
            }

            label.setValue(vlanLabels.toString());
            inPortGroup.getLabelGroup().add(label);
            outPortGroup.getLabelGroup().add(label);

            // EsdbNsiPeering peer = nsiPeerings.get(i); // the order doesn't match the id order - use map peer->equipment

            // if NSI peer has I/O alias
            if (peer.getInAlias() != null && !peer.getInAlias().isEmpty()) {
                // set in alias
                NmlPortGroupRelationType inRelation = new NmlPortGroupRelationType();
                inRelation.setType(NML_BASE_NAMESPACE + NML_BASE_TYPE_IS_ALIAS);

                NmlPortGroupType inAlias = new NmlPortGroupType();
                inAlias.setId(peer.getInAlias());

                inRelation.getPortGroup().add(inAlias);
                inPortGroup.getRelation().add(inRelation);
            }
            if (peer.getOutAlias() != null && !peer.getOutAlias().isEmpty()) {
                // set out alias
                NmlPortGroupRelationType outRelation = new NmlPortGroupRelationType();
                outRelation.setType(NML_BASE_NAMESPACE + NML_BASE_TYPE_IS_ALIAS);

                NmlPortGroupType outAlias = new NmlPortGroupType();
                outAlias.setId(peer.getOutAlias());

                outRelation.getPortGroup().add(outAlias);
                outPortGroup.getRelation().add(outRelation);
            }
            Long nsiSpeed = peer.getEquipmentInterface().getInterfaceBandwidth().getSpeed().longValue() * 1000000L;
            if (peer.getBandwidth() != null) {
                nsiSpeed = Long.valueOf(peer.getBandwidth()) * 1000000L;
            }
            inPortGroup.getAny().add(nmlFactory.createMaximumReservableCapacity(nsiSpeed));
            inPortGroup.getAny().add(nmlFactory.createCapacity(nsiSpeed));
            outPortGroup.getAny().add(nmlFactory.createMaximumReservableCapacity(nsiSpeed));
            outPortGroup.getAny().add(nmlFactory.createCapacity(nsiSpeed));

            // set min capacity and granularity
            inPortGroup.getAny().add(nmlFactory.createMinimumReservableCapacity(0L));
            inPortGroup.getAny().add(nmlFactory.createGranularity(NML_ETH_GRANULARITY));
            topologyInRelation.getPortGroup().add(inPortGroup);
            outPortGroup.getAny().add(nmlFactory.createMinimumReservableCapacity(0L));
            outPortGroup.getAny().add(nmlFactory.createGranularity(NML_ETH_GRANULARITY));
            topologyOutRelation.getPortGroup().add(outPortGroup);
        }

        // add I/O relation to NSI topology
        nsiTopology.getRelation().add(topologyInRelation);
        nsiTopology.getRelation().add(topologyOutRelation);

        StringWriter sw = new StringWriter();
        JAXB.marshal(nmlFactory.createTopology(nsiTopology), sw);

        String nmlTopologyXml = sw.toString();

        /*
        log.info("========================== GENERATE XML START ==========================");
        log.info(nmlTopologyXml);
        log.info("========================== GENERATE XML END ==========================");
        */

        return ResponseEntity.ok(nmlTopologyXml);
    }


    /**
     * Generate the ESnet NSA topology discovery information and makes it available as an XML based API endpoint
     *
     * @return returns an XML NSA description
     * @throws DatatypeConfigurationException
     */
    @RequestMapping(value = NSA_INFO_URI, produces = {MediaType.APPLICATION_XML_VALUE}, headers = "Accept=application/xml")
    public ResponseEntity<?> getNsaDiscovery() throws DatatypeConfigurationException {

        // determine start and end date
        Instant now = Instant.now();
        int daysValid = 90;
        Instant expiration = now.plus(daysValid, ChronoUnit.DAYS);

        GregorianCalendar gCal = new GregorianCalendar();
        gCal.setTimeInMillis(now.toEpochMilli());
        XMLGregorianCalendar startDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(gCal);

        gCal.setTimeInMillis(expiration.toEpochMilli());
        XMLGregorianCalendar endDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(gCal);

        // generate NSA discovery
        NsaType nsa = nsaFactory.createNsaType();
        nsa.setId(constants.NSA_ID());
        nsa.setVersion(startDate);
        nsa.setStartTime(startDate);
        nsa.setExpires(endDate);
        nsa.setName(NSA_NAME);
        nsa.setSoftwareVersion(SW_VERSION_NSA);
        nsa.getNetworkId().add(constants.PROVIDER_ID());

        XcardUidPropType props = new XcardUidPropType();
        props.setUri(nsaProperties.getBaseurl() + PROVIDER_PROPS_URI_NSA);

        XcardProdidPropType providerName = new XcardProdidPropType();
        providerName.setText(NSA_NAME);

        // create rev time stamp format
        String pattern = "yyyyMMdd'T'hhmmssZZ";
        DateTimeFormatter formatForVcard = DateTimeFormatter.ofPattern(pattern).withZone(ZoneOffset.systemDefault());
        XcardRevPropType revision = new XcardRevPropType();
        revision.setTimestamp(formatForVcard.format(now));

        XcardKindPropType kind = new XcardKindPropType();
        kind.setText(KIND_NSA);

        XcardFnPropType contact = new XcardFnPropType();
        String contactName = nsaProperties.getFirstname() + " " + nsaProperties.getLastname();
        contact.setText(contactName);

        // set contact info and vCard
        XcardNPropType name = new XcardNPropType();
        name.getGiven().add(nsaProperties.getFirstname());
        name.getSurname().add(nsaProperties.getLastname());

        XcardEmailPropType email = new XcardEmailPropType();
        email.setText(nsaProperties.getEmail());

        XcardVcardsType.XcardVcard vCard = new XcardVcardsType.XcardVcard();
        vCard.setUid(props);
        vCard.setProdid(providerName);
        vCard.setRev(revision);
        vCard.setKind(kind);
        vCard.setFn(contact);
        vCard.setN(name);
        vCard.getEmail().add(email);

        XcardVcardsType admin = new XcardVcardsType();
        admin.setVcard(vCard);
        nsa.setAdminContact(admin);

        // set LBNL as contact location
        LocationType location = new LocationType();
        location.setLatitude(GPS_LAT_LBNL);
        location.setLongitude(GPS_LONG_LBNL);
        nsa.setLocation(location);

        // add nml topo interface description
        InterfaceType nmlTopology = new InterfaceType();
        nmlTopology.setType(TOPO_NML_XML_TYPE);
        nmlTopology.setHref(nsaProperties.getBaseurl() + NML_TOPO_URI);
        nsa.getInterface().add(nmlTopology);

        InterfaceType cs = new InterfaceType();
        cs.setType(TOPO_PROVIDER_SOAP_TYPE);
        cs.setHref(nsaProperties.getBaseurl() + TOPO_PROVIDER_SOAP_URL);
        nsa.getInterface().add(cs);

        // set features
        // one = uPA / two = timeout
        FeatureType upa = new FeatureType();
        upa.setType(NSA_FEAT_UPA);
        FeatureType cmtTimeout = new FeatureType();
        cmtTimeout.setType(NSA_FEAT_CMTTIMEOUT);
        cmtTimeout.setValue(NSA_FEAT_CMTTIMEOUT_VAL);
        FeatureType modify = new FeatureType();
        modify.setType(NSA_FEAT_MODIFY);
        nsa.getFeature().add(upa);
        nsa.getFeature().add(cmtTimeout);
        nsa.getFeature().add(modify);


        // set nsa-agg
        PeersWithType nsaPeer = new PeersWithType();
        nsaPeer.setRole(PeerRoleEnum.PA);
        nsaPeer.setValue(constants.AGG_NSA());
        nsa.getPeersWith().add(nsaPeer);

        StringWriter sw = new StringWriter();
        JAXB.marshal(nsaFactory.createNsa(nsa), sw);

        String nsaXml = sw.toString();

        /*
        log.info("========================== GENERATE XML START ==========================");
        log.info(nsaXml);
        log.info("========================== GENERATE XML END ==========================");
        */

        return ResponseEntity.ok(nsaXml);
    }

    private boolean existsInOscars(String deviceName, String ifce, OscarsOneTopo oscarsOneTopo) {
        boolean exists = false;
        // set the port URN to look like what OSCARS topology uses internally,
        String portUrn = deviceName + ":" + ifce;
        // walk through the devices,
        for (OscarsOneDevice device : oscarsOneTopo.getDevices()) {
            if (device.getUrn().equals(deviceName)) {
                // walk through the ports of the matching device
                for (OscarsOnePort port : device.getPorts()) {
                    if (port.getUrn().equals(portUrn)) {
                        exists = true;
                        break;
                    }
                }
                break;
            }
        }

        return exists;
    }

}
