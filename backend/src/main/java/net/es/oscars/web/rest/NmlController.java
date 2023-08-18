package net.es.oscars.web.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.app.util.AppVersion;
import net.es.oscars.nsi.beans.NsiPeering;
import net.es.oscars.nsi.svc.NsiPopulator;
import net.es.oscars.nsi.svc.NsiService;
import net.es.oscars.topo.beans.IntRange;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.ent.Device;
import net.es.oscars.topo.ent.Port;
import net.es.oscars.topo.ent.Version;
import net.es.oscars.topo.enums.Layer;
import net.es.oscars.topo.svc.TopoService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Controller
@Slf4j
public class NmlController {
    @Value("${nml.topo-id}")
    private String topoId;

    @Value("${nml.topo-name}")
    private String topoName;

    @Value("${nml.base-url}")
    private String baseUrl;


    @Value("${nsi.provider-nsa}")
    private String providerNsa;

    @Value("${nsi.nsa-name}")
    private String nsaName;

    @Value("${nsi.nsa-contact}")
    private String nsaContact;

    @Value("${nsi.nsa-location}")
    private String nsaLocation;

    @Value("${resv.timeout}")
    private Integer resvTimeout;


    @Autowired
    private TopoService topoService;

    @Autowired
    private NsiService nsiService;

    @Autowired
    private NsiPopulator nsiPopulator;

    @Autowired
    private Startup startup;

    private static String NSA_PROVIDER_TYPE = "application/vnd.ogf.nsi.cs.v2.provider+soap";
    private static String NSA_TOPO_TYPE = "application/vnd.ogf.nsi.topology.v2+xml";
    private static String NSA_FEATURE_UPA = "vnd.ogf.nsi.cs.v2.role.uPA";
    private static String NSA_FEATURE_TIMEOUT = "org.ogf.nsi.cs.v2.commitTimeout";

    private static String nsBase = "http://schemas.ogf.org/nml/2013/05/base#";
    private static String nsDefs = "http://schemas.ogf.org/nsi/2013/12/services/definition";
    private static String nsEth = "http://schemas.ogf.org/nml/2012/10/ethernet";
    private static String isAliasType = "http://schemas.ogf.org/nml/2013/05/base#isAlias";

    private static String nsDiscovery = "http://schemas.ogf.org/nsi/2014/02/discovery/nsa";
    private static String nsVcard = "urn:ietf:params:xml:ns:vcard-4.0";

    @ExceptionHandler(StartupException.class)
    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    public void handleStartup(StartupException ex) {
        log.warn("Still in startup");
    }


    @GetMapping(value = "/api/topo/nml")
    public void getTopologyXml(HttpServletRequest request, HttpServletResponse res) throws Exception {

        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }
        if (!nsiPopulator.isLoaded()) {
            throw new StartupException("NSI files not loaded");
        }

        if (topoService.getCurrent() == null) {
            throw new InternalError("no valid topology version");
        }
        Version v = topoService.getCurrent();

        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        // log.info("ims from browser: "+ifModifiedSince);
        res.setHeader("Cache-Control", "max-age=3600");

        long lastModified = v.getUpdated().getEpochSecond() * 1000; // I-M-S in milliseconds

        // if request did not set the header, we get a -1 in iMS
        if (ifModifiedSince != -1 && lastModified <= ifModifiedSince) {
            // log.debug("returning not-modified to browser,  ims: "+ifModifiedSince+ " lm: "+lastModified);
            res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
        res.setDateHeader("Last-Modified", lastModified);


        Topology topology = topoService.currentTopology();
        List<Port> edgePorts = new ArrayList<>();
        for (Device d : topology.getDevices().values()) {
            for (Port p : d.getPorts()) {
                if (p.getCapabilities().contains(Layer.ETHERNET) && !p.getReservableVlans().isEmpty()) {
                    boolean allow = false;
                    for (String filter : nsiPopulator.getFilter()) {
                        if (p.getUrn().contains(filter)) {
                            allow = true;
                        }

                    }
                    if (allow) {
                        edgePorts.add(p);
                    }
                }
            }
        }

        String prefix = topoId + ":";
        String granularity = "1000000";
        String minRc = "0";

        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_INSTANT;

        Instant now = Instant.now();
        Instant topoExpiration = now.plus(1, ChronoUnit.DAYS);

        GregorianCalendar nowC = new GregorianCalendar();
        nowC.setTimeInMillis(now.toEpochMilli());
        XMLGregorianCalendar nowX = DatatypeFactory.newInstance().newXMLGregorianCalendar(nowC);

        GregorianCalendar endC = new GregorianCalendar();
        endC.setTimeInMillis(topoExpiration.toEpochMilli());
        XMLGregorianCalendar endX = DatatypeFactory.newInstance().newXMLGregorianCalendar(endC);


        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);

        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElementNS(nsBase, "nml-base:Topology");
        doc.appendChild(rootElement);
        rootElement.setAttribute("xmlns:nml-base", nsBase);
        rootElement.setAttribute("xmlns:nml-eth", nsEth);
        rootElement.setAttribute("xmlns:nsi-defs", nsDefs);


        rootElement.setAttribute("version", dateFormatter.format(now));
        rootElement.setAttribute("id", topoId);
        Element tName = doc.createElementNS(nsBase, "nml-base:name");
        tName.setTextContent(topoName);
        rootElement.appendChild(tName);

        Element lifetime = doc.createElementNS(nsBase, "nml-base:Lifetime");
        Element lStart = doc.createElementNS(nsBase, "nml-base:start");
        lStart.setTextContent(nowX.toString());
        lifetime.appendChild(lStart);
        Element lEnd = doc.createElementNS(nsBase, "nml-base:end");
        lEnd.setTextContent(endX.toString());
        lifetime.appendChild(lEnd);

        rootElement.appendChild(lifetime);

        for (Port p : edgePorts) {
            String nsiUrn = nsiService.nsiUrnFromInternal(p.getUrn());

            Element bdp = doc.createElementNS(nsBase, "nml-base:BidirectionalPort");
            bdp.setAttribute("id", nsiUrn);
            rootElement.appendChild(bdp);

            this.addLocation(doc, bdp, p.getDevice());

            Element pgi = doc.createElementNS(nsBase, "nml-base:PortGroup");
            bdp.appendChild(pgi);
            pgi.setAttribute("id", nsiUrn + ":in");

            Element pgo = doc.createElementNS(nsBase, "nml-base:PortGroup");
            pgo.setAttribute("id", nsiUrn + ":out");
            bdp.appendChild(pgo);
        }

        for (NsiPeering peering : nsiPopulator.getNotPlusPorts()) {
            String inUrn = topoId + ":" + peering.getIn().getLocal();
            String outUrn = topoId + ":" + peering.getOut().getLocal();

            String inUrnEnding = StringUtils.right(inUrn, 3);
            if (!inUrnEnding.equals(":in")) {
                log.error("invalid NSI peering in.local URN " + inUrn);
                continue;
            }
            String portNsiUrn = inUrn.substring(0, inUrn.length() - 3);

            Element bdp = doc.createElementNS(nsBase, "nml-base:BidirectionalPort");
            bdp.setAttribute("id", portNsiUrn);
            rootElement.appendChild(bdp);

            String[] localParts = StringUtils.split(peering.getIn().getLocal(), ":");
            String localDevice = localParts[0];

            Optional<Device> maybeDevice = topoService.getDeviceRepo().findByUrn(localDevice);
            if (maybeDevice.isPresent()) {
                this.addLocation(doc, bdp, maybeDevice.get());
            } else {
                log.error("device not found for peering: " + peering.getIn());
            }


            Element bdpgi = doc.createElementNS(nsBase, "nml-base:PortGroup");
            bdp.appendChild(bdpgi);
            bdpgi.setAttribute("id", inUrn);

            Element bdpgo = doc.createElementNS(nsBase, "nml-base:PortGroup");
            bdpgo.setAttribute("id", outUrn);
            bdp.appendChild(bdpgo);
        }


        Element serviceDefinition = doc.createElementNS(nsDefs, "nsi-defs:serviceDefinition");
        serviceDefinition.setAttribute("id", prefix + "ServiceDefinition:EVTS.A-GOLE");
        rootElement.appendChild(serviceDefinition);
        Element sdName = doc.createElement("name");
        sdName.setTextContent("GLIF Automated GOLE Ethernet VLAN Transfer Service");
        serviceDefinition.appendChild(sdName);
        Element svcType = doc.createElement("serviceType");
        svcType.setTextContent("http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE");
        serviceDefinition.appendChild(svcType);

        Element ssRel = doc.createElementNS(nsBase, "nml-base:Relation");
        ssRel.setAttribute("type", "http://schemas.ogf.org/nml/2013/05/base#hasService");
        rootElement.appendChild(ssRel);
        Element sSvc = doc.createElementNS(nsBase, "nml-base:SwitchingService");
        sSvc.setAttribute("id", prefix + "ServiceDomain:EVTS.A-GOLE");
        sSvc.setAttribute("encoding", "http://schemas.ogf.org/nml/2012/10/ethernet");
        sSvc.setAttribute("labelSwapping", "true");
        sSvc.setAttribute("labelType", "http://schemas.ogf.org/nml/2012/10/ethernet#vlan");
        ssRel.appendChild(sSvc);
        Element ssIRel = doc.createElementNS(nsBase, "nml-base:Relation");
        ssIRel.setAttribute("type", "http://schemas.ogf.org/nml/2013/05/base#hasInboundPort");
        sSvc.appendChild(ssIRel);
        Element ssORel = doc.createElementNS(nsBase, "nml-base:Relation");
        ssORel.setAttribute("type", "http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort");
        sSvc.appendChild(ssORel);


        Element ssSd = doc.createElementNS(nsDefs, "nsi-defs:serviceDefinition");
        ssSd.setAttribute("id", prefix + "ServiceDefinition:EVTS.A-GOLE");
        sSvc.appendChild(ssSd);

        Set<String> addedToIn = new HashSet<>();
        Set<String> addedToOut = new HashSet<>();
        for (Port p : edgePorts) {
            String nsiUrn = nsiService.nsiUrnFromInternal(p.getUrn());
            Element pgsi = doc.createElementNS(nsBase, "nml-base:PortGroup");
            pgsi.setAttribute("id", nsiUrn + ":in");
            ssIRel.appendChild(pgsi);
            addedToIn.add(nsiUrn + ":in");
            Element pgso = doc.createElementNS(nsBase, "nml-base:PortGroup");
            pgso.setAttribute("id", nsiUrn + ":out");
            ssORel.appendChild(pgso);
            addedToOut.add(nsiUrn + ":out");
        }


        Element hip = doc.createElementNS(nsBase, "nml-base:Relation");
        hip.setAttribute("type", "http://schemas.ogf.org/nml/2013/05/base#hasInboundPort");
        rootElement.appendChild(hip);
        Element hop = doc.createElementNS(nsBase, "nml-base:Relation");
        hop.setAttribute("type", "http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort");
        rootElement.appendChild(hop);

        for (Port p : edgePorts) {
            List<String> parts = new ArrayList<>();
            for (IntRange r : p.getReservableVlans()) {
                if (r.getFloor().equals(r.getCeiling())) {
                    parts.add(r.getFloor().toString());
                } else {
                    parts.add(r.getFloor() + "-" + r.getCeiling());
                }
            }
            String vlans = String.join(",", parts);
            String nsiUrn = nsiService.nsiUrnFromInternal(p.getUrn());


            Element pgi = doc.createElementNS(nsBase, "nml-base:PortGroup");
            pgi.setAttribute("id", nsiUrn + ":in");
            pgi.setAttribute("encoding", "http://schemas.ogf.org/nml/2012/10/ethernet");
            hip.appendChild(pgi);

            String peeringUrn = p.getUrn().replace("/", "_");
            //log.info("checking peering urn "+peeringUrn);
            NsiPeering peering = nsiPopulator.getPlusPorts().get(peeringUrn);


            Element ilg = doc.createElementNS(nsBase, "nml-base:LabelGroup");
            ilg.setAttribute("labeltype", "http://schemas.ogf.org/nml/2012/10/ethernet#vlan");
            if (peering != null) {
                ilg.setTextContent(peering.getVlan());
            } else {
                ilg.setTextContent(vlans);
            }
            pgi.appendChild(ilg);

            if (peering != null) {
                Element isAliasRelation = doc.createElementNS(nsBase, "nml-base:Relation");
                isAliasRelation.setAttribute("type", isAliasType);
                Element remote = doc.createElementNS(nsBase, "nml-base:PortGroup");
                remote.setAttribute("id", peering.getIn().getRemote());
                isAliasRelation.appendChild(remote);
                pgi.appendChild(isAliasRelation);
            }

            Element imxrc = doc.createElementNS(nsEth, "nml-eth:maximumReservableCapacity");
            String ibps = p.getReservableIngressBw().toString() + "000000";
            imxrc.setTextContent(ibps);
            Element imnrc = doc.createElementNS(nsEth, "nml-eth:minimumReservableCapacity");
            imnrc.setTextContent(minRc);
            Element icap = doc.createElementNS(nsEth, "nml-eth:capacity");
            icap.setTextContent(ibps);
            Element igrn = doc.createElementNS(nsEth, "nml-eth:granularity");
            igrn.setTextContent(granularity);
            pgi.appendChild(imxrc);
            pgi.appendChild(imnrc);
            pgi.appendChild(icap);
            pgi.appendChild(igrn);


            Element pgo = doc.createElementNS(nsBase, "nml-base:PortGroup");
            pgo.setAttribute("id", nsiUrn + ":out");
            pgo.setAttribute("encoding", "http://schemas.ogf.org/nml/2012/10/ethernet");
            hop.appendChild(pgo);

            Element olg = doc.createElementNS(nsBase, "nml-base:LabelGroup");
            olg.setAttribute("labeltype", "http://schemas.ogf.org/nml/2012/10/ethernet#vlan");
            if (peering != null) {
                olg.setTextContent(peering.getVlan());
            } else {
                olg.setTextContent(vlans);
            }
            pgo.appendChild(olg);
            if (peering != null) {
                Element isAliasRelation = doc.createElementNS(nsBase, "nml-base:Relation");
                isAliasRelation.setAttribute("type", isAliasType);
                Element remote = doc.createElementNS(nsBase, "nml-base:PortGroup");
                remote.setAttribute("id", peering.getOut().getRemote());
                isAliasRelation.appendChild(remote);
                pgo.appendChild(isAliasRelation);
            }

            Element omxrc = doc.createElementNS(nsEth, "nml-eth:maximumReservableCapacity");
            String obps = p.getReservableEgressBw() + "000000";
            omxrc.setTextContent(obps);
            Element omnrc = doc.createElementNS(nsEth, "nml-eth:minimumReservableCapacity");
            omnrc.setTextContent(minRc);
            Element ocap = doc.createElementNS(nsEth, "nml-eth:capacity");
            ocap.setTextContent(obps);
            Element ogrn = doc.createElementNS(nsEth, "nml-eth:granularity");
            ogrn.setTextContent(granularity);
            pgo.appendChild(omxrc);
            pgo.appendChild(omnrc);
            pgo.appendChild(ocap);
            pgo.appendChild(ogrn);
        }

        for (NsiPeering peering : nsiPopulator.getNotPlusPorts()) {

            String inUrn = topoId + ":" + peering.getIn().getLocal();
            String outUrn = topoId + ":" + peering.getOut().getLocal();

            Element pgi = doc.createElementNS(nsBase, "nml-base:PortGroup");
            pgi.setAttribute("id", inUrn);
            pgi.setAttribute("encoding", "http://schemas.ogf.org/nml/2012/10/ethernet");
            hip.appendChild(pgi);

            Element ilg = doc.createElementNS(nsBase, "nml-base:LabelGroup");
            ilg.setAttribute("labeltype", "http://schemas.ogf.org/nml/2012/10/ethernet#vlan");
            ilg.setTextContent(peering.getVlan());
            pgi.appendChild(ilg);
            Element inIsAliasRelation = doc.createElementNS(nsBase, "nml-base:Relation");
            inIsAliasRelation.setAttribute("type", isAliasType);

            Element inRemote = doc.createElementNS(nsBase, "nml-base:PortGroup");
            inRemote.setAttribute("id", peering.getIn().getRemote());
            inIsAliasRelation.appendChild(inRemote);
            pgi.appendChild(inIsAliasRelation);

            Element imxrc = doc.createElementNS(nsEth, "nml-eth:maximumReservableCapacity");
            String ibps = peering.getCapacity().replaceAll("[gG]", "000000000");
            imxrc.setTextContent(ibps);
            Element imnrc = doc.createElementNS(nsEth, "nml-eth:minimumReservableCapacity");
            imnrc.setTextContent(minRc);
            Element icap = doc.createElementNS(nsEth, "nml-eth:capacity");
            icap.setTextContent(ibps);
            Element igrn = doc.createElementNS(nsEth, "nml-eth:granularity");
            igrn.setTextContent(granularity);
            pgi.appendChild(imxrc);
            pgi.appendChild(imnrc);
            pgi.appendChild(icap);
            pgi.appendChild(igrn);


            Element pgo = doc.createElementNS(nsBase, "nml-base:PortGroup");
            pgo.setAttribute("id", outUrn);
            pgo.setAttribute("encoding", "http://schemas.ogf.org/nml/2012/10/ethernet");

            Element olg = doc.createElementNS(nsBase, "nml-base:LabelGroup");
            olg.setAttribute("labeltype", "http://schemas.ogf.org/nml/2012/10/ethernet#vlan");
            olg.setTextContent(peering.getVlan());
            pgo.appendChild(olg);
            Element outIsAliasRelation = doc.createElementNS(nsBase, "nml-base:Relation");
            outIsAliasRelation.setAttribute("type", isAliasType);

            Element outRemote = doc.createElementNS(nsBase, "nml-base:PortGroup");
            outRemote.setAttribute("id", peering.getOut().getRemote());
            outIsAliasRelation.appendChild(outRemote);
            pgo.appendChild(outIsAliasRelation);
            hop.appendChild(pgo);

            if (!addedToIn.contains(inUrn)) {
                Element pgsi = doc.createElementNS(nsBase, "nml-base:PortGroup");
                pgsi.setAttribute("id", inUrn);
                ssIRel.appendChild(pgsi);
            }
            if (!addedToOut.contains(outUrn)) {
                Element pgso = doc.createElementNS(nsBase, "nml-base:PortGroup");
                pgso.setAttribute("id", outUrn);
                ssORel.appendChild(pgso);
            }

            Element omxrc = doc.createElementNS(nsEth, "nml-eth:maximumReservableCapacity");
            String obps = peering.getCapacity().replaceAll("[gG]", "000000000");
            omxrc.setTextContent(obps);
            Element omnrc = doc.createElementNS(nsEth, "nml-eth:minimumReservableCapacity");
            omnrc.setTextContent(minRc);
            Element ocap = doc.createElementNS(nsEth, "nml-eth:capacity");
            ocap.setTextContent(obps);
            Element ogrn = doc.createElementNS(nsEth, "nml-eth:granularity");
            ogrn.setTextContent(granularity);
            pgo.appendChild(omxrc);
            pgo.appendChild(omnrc);
            pgo.appendChild(ocap);
            pgo.appendChild(ogrn);
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);

        String xmlString = result.getWriter().toString();


        res.setContentType("application/xml");

        PrintWriter out = res.getWriter();
        out.println(xmlString);
        out.close();
    }

    @GetMapping(value = "/api/nsa/discovery")
    public void getNsaDiscovery(HttpServletRequest req, HttpServletResponse res) throws Exception {
        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }


        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_INSTANT;
        String pattern = "yyyyMMdd'T'hhmmssZZ";

        DateTimeFormatter formatForVcard = DateTimeFormatter
                .ofPattern(pattern)
                .withZone(ZoneOffset.systemDefault());

        if (topoService.getCurrent() == null) {
            throw new InternalError("no valid topology version");
        }
        Version v = topoService.getCurrent();

        long lastModified = v.getUpdated().getEpochSecond() * 1000; // I-M-S in milliseconds
        long ifModifiedSince = req.getDateHeader("If-Modified-Since");
        // log.info("ims from browser: "+ifModifiedSince);
        res.setHeader("Cache-Control", "max-age=3600");
        res.setDateHeader("Last-Modified", lastModified);

        // if request did not set the header, we get a -1 in iMS
        if (lastModified <= ifModifiedSince && ifModifiedSince != -1) {
            res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }


        String[] locParts = nsaLocation.split(",");
        String latitude = locParts[0];
        String longitude = locParts[1];

        String[] contactParts = nsaContact.split(",");
        String contactName = contactParts[0];
        String contactEmail = contactParts[1];
        String[] cnParts = contactName.split("\\s+");
        String cnGiven = cnParts[0];
        String cnSurname = cnParts[1];

        Topology topology = topoService.currentTopology();

        String xmlVersion = dateFormatter.format(v.getUpdated());
        String vCardTimestamp = formatForVcard.format(v.getUpdated());

        Instant now = Instant.now();
        GregorianCalendar nowc = new GregorianCalendar();
        nowc.setTimeInMillis(now.toEpochMilli());
        XMLGregorianCalendar nowx = DatatypeFactory.newInstance().newXMLGregorianCalendar(nowc);

        Instant threeMonths = now.plus(90, ChronoUnit.DAYS);
        String expires = dateFormatter.format(threeMonths);

        String soapUrl = baseUrl + "/services/provider";
        String topoUrl = baseUrl + "/api/topo/nml";

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);


        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElementNS(nsDiscovery, "disc:nsa");

        rootElement.setAttribute("expires", expires);
        rootElement.setAttribute("id", providerNsa);
        rootElement.setAttribute("version", xmlVersion);

        rootElement.setAttribute("xmlns:disc", nsDiscovery);
        rootElement.setAttribute("xmlns:vc", nsVcard);
        doc.appendChild(rootElement);


        Element dName = doc.createElement("name");
        dName.setTextContent(nsaName);
        rootElement.appendChild(dName);


        Element dSwVer = doc.createElement("softwareVersion");
        dSwVer.setTextContent(AppVersion.getVersion());
        rootElement.appendChild(dSwVer);

        Element dStartTime = doc.createElement("startTime");
        dStartTime.setTextContent(xmlVersion);
        rootElement.appendChild(dStartTime);

        // contact vcard
        Element dAdminContact = doc.createElement("adminContact");
        rootElement.appendChild(dAdminContact);
        Element vVcard = doc.createElementNS(nsVcard, "vc:vcard");
        dAdminContact.appendChild(vVcard);

        Element vUid = doc.createElementNS(nsVcard, "vc:uid");
        vVcard.appendChild(vUid);
        Element vUri = doc.createElementNS(nsVcard, "vc:uri");
        vUri.setTextContent(soapUrl + "#adminContact");
        vUid.appendChild(vUri);

        Element vProdId = doc.createElementNS(nsVcard, "vc:prodid");
        Element vProdText = doc.createElementNS(nsVcard, "vc:text");
        vProdText.setTextContent(nsaName);
        vProdId.appendChild(vProdText);
        vVcard.appendChild(vProdId);

        Element vRev = doc.createElementNS(nsVcard, "vc:rev");
        Element vTimestamp = doc.createElementNS(nsVcard, "vc:timestamp");
        vTimestamp.setTextContent(vCardTimestamp);
        vRev.appendChild(vTimestamp);
        vVcard.appendChild(vRev);


        Element vKind = doc.createElementNS(nsVcard, "vc:kind");
        Element vKindText = doc.createElementNS(nsVcard, "vc:text");
        vKindText.setTextContent("individual");
        vKind.appendChild(vKindText);
        vVcard.appendChild(vKind);

        Element vFn = doc.createElementNS(nsVcard, "vc:fn");
        Element vFnText = doc.createElementNS(nsVcard, "vc:text");
        vFnText.setTextContent(contactName);
        vFn.appendChild(vFnText);
        vVcard.appendChild(vFn);

        Element vN = doc.createElementNS(nsVcard, "vc:n");
        Element vSurname = doc.createElementNS(nsVcard, "vc:surname");
        vSurname.setTextContent(cnSurname);
        Element vGiven = doc.createElementNS(nsVcard, "vc:given");
        vGiven.setTextContent(cnGiven);
        vN.appendChild(vSurname);
        vN.appendChild(vGiven);
        vVcard.appendChild(vN);

        Element vEmail = doc.createElementNS(nsVcard, "vc:email");
        Element vEmailText = doc.createElementNS(nsVcard, "vc:text");
        vEmailText.setTextContent(contactEmail);
        vEmail.appendChild(vEmailText);
        vVcard.appendChild(vEmail);

        // lat and long
        Element dLocation = doc.createElement("location");
        Element dLongitude = doc.createElement("longitude");
        dLongitude.setTextContent(longitude);

        Element dLatitude = doc.createElement("latitude");
        dLatitude.setTextContent(latitude);

        dLocation.appendChild(dLongitude);
        dLocation.appendChild(dLatitude);
        rootElement.appendChild(dLocation);


        Element dNetworkId = doc.createElement("networkId");
        dNetworkId.setTextContent(topoId);
        rootElement.appendChild(dNetworkId);

        Element dPaIfce = doc.createElement("interface");
        Element dPaType = doc.createElement("type");
        dPaType.setTextContent(NSA_PROVIDER_TYPE);
        dPaIfce.appendChild(dPaType);
        Element dPaHref = doc.createElement("href");
        dPaHref.setTextContent(soapUrl);
        dPaIfce.appendChild(dPaHref);
        rootElement.appendChild(dPaIfce);

        Element dTopoIfce = doc.createElement("interface");
        Element dTopoType = doc.createElement("type");
        dTopoType.setTextContent(NSA_TOPO_TYPE);
        dTopoIfce.appendChild(dTopoType);
        Element dTopoHref = doc.createElement("href");
        dTopoHref.setTextContent(topoUrl);
        dTopoIfce.appendChild(dTopoHref);

        rootElement.appendChild(dTopoIfce);

        Element dFeatureUpa = doc.createElement("feature");
        dFeatureUpa.setAttribute("type", NSA_FEATURE_UPA);
        rootElement.appendChild(dFeatureUpa);

        Element dFeatureTimeout = doc.createElement("feature");
        dFeatureTimeout.setAttribute("type", NSA_FEATURE_TIMEOUT);
        dFeatureTimeout.setTextContent("" + resvTimeout);
        rootElement.appendChild(dFeatureTimeout);

        Element dPeersWith = doc.createElement("peersWith");
        dPeersWith.setAttribute("role", "PA");
        dPeersWith.setTextContent("urn:ogf:network:es.net:2013:nsa:nsi-aggr-west");
        rootElement.appendChild(dPeersWith);


        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);

        String xmlString = result.getWriter().toString();


        PrintWriter out = res.getWriter();
        res.setContentType("application/xml");
        out.println(xmlString);
        out.close();

    }

    private void addLocation(Document doc, Element bdp, Device device) {

        String locationId = this.topoId + ":locations:" + device.getLocationId();
        String locationName = device.getLocation();
        Double latitude = device.getLatitude();
        Double longitude = device.getLongitude();

        Element location = doc.createElementNS(nsBase, "nml-base:Location");
        location.setAttribute("id", locationId);

        Element name = doc.createElementNS(nsBase, "nml-base:name");
        name.setTextContent(locationName);
        location.appendChild(name);

        Element lat = doc.createElementNS(nsBase, "nml-base:lat");
        lat.setTextContent(String.valueOf(latitude));
        location.appendChild(lat);

        Element longd = doc.createElementNS(nsBase, "nml-base:long");
        longd.setTextContent(String.valueOf(longitude));
        location.appendChild(longd);

        bdp.appendChild(location);
    }

}