package net.es.oscars.topo.nsi;

import org.springframework.stereotype.Component;

@Component
public class Constants {
    private final NsaProperties nsaProperties;

    // NsiCOntroller
    public final static String NML_TOPO_URI = "/api/topo/nml";
    public final static String NSA_INFO_URI = "/api/nsa/discovery";

    // NML topology
    public final static String NML_BASE_NAMESPACE = "http://schemas.ogf.org/nml/2013/05/base#";

    public final static String ID_SEPARATOR = ":";
    public final static String ID_NONE = "+";

    public final static String NML_BASE_TYPE_HAS_IN_PORT = "hasInboundPort";
    public final static String NML_BASE_TYPE_HAS_OUT_PORT = "hasOutboundPort";
    public final static String NML_BASE_TYPE_HAS_SERVICE = "hasService";
    public final static String NML_BASE_TYPE_IS_ALIAS = "isAlias";

    public Constants(NsaProperties nsaProperties) {
        this.nsaProperties = nsaProperties;
    }

    public String PROVIDER_ID() {
        return "urn:ogf:network:" + nsaProperties.getProvider() + ":2013" + ID_SEPARATOR;
    }


    public final static String ID_INFORMATION_TYPE_TOPO = "topology" + ID_SEPARATOR;
    public final static String ID_INFORMATION_TYPE_NONE = ID_SEPARATOR;

    public final static String ID_PORT_IN = "in";
    public final static String ID_PORT_OUT = "out";

    public final static String ID_SERVICE_DEF_CONSUMER = "EVTS.A-GOLE";

    public String ID_SERVICE_DEFINITION() {
        return PROVIDER_ID() + ID_INFORMATION_TYPE_TOPO
                + "ServiceDefinition" + ID_SEPARATOR + ID_SERVICE_DEF_CONSUMER;
    }

    public String ID_SERVICE_DOMAIN() {
        return PROVIDER_ID() + ID_INFORMATION_TYPE_TOPO + "ServiceDomain" + ID_SEPARATOR + ID_SERVICE_DEF_CONSUMER;
    }

    public final static String SERVICE_TYPE = "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE";
    public final static String SERVICE_NAME = "GLIF Automated GOLE Ethernet VLAN Transfer Service";

    public final static Long NML_ETH_GRANULARITY = 1000000L;

    // NSA discovery
    public String NSA_ID() {
        return PROVIDER_ID() + "nsa";
    }

    public final static String NSA_NAME = "ESnet OSCARS NSA";
    public final static String SW_VERSION_NSA = "1.0.45";

    public final static String PROVIDER_PROPS_URI_NSA = "/services/provider#adminContact";

    public final static String KIND_NSA = "individual";
    public final static String NSA_FEAT_UPA = "vnd.ogf.nsi.cs.v2.role.uPA";
    public final static String NSA_FEAT_CMTTIMEOUT = "org.ogf.nsi.cs.v2.commitTimeout";
    public final static String NSA_FEAT_CMTTIMEOUT_VAL = "900";
    public final static String NSA_FEAT_MODIFY = "org.ogf.nsi.cs.v2.modify";

    public String AGG_NSA() {
        return NSA_ID() + ID_SEPARATOR + "nsa:nsi-aggr-west";
    }

    public final static float GPS_LONG_LBNL = -122.253f;
    public final static float GPS_LAT_LBNL = 37.876f;

    public final static String TOPO_NML_XML_TYPE = "application/vnd.ogf.nsi.topology.v2+xml";
    public final static String TOPO_PROVIDER_SOAP_TYPE = "application/vnd.ogf.nsi.cs.v2.provider+soap";
    public final static String TOPO_PROVIDER_SOAP_URL = "/services/provider";

}
