package net.es.nsi.lib.soap.gen.nsi_2_0.connection.requester;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import jakarta.xml.ws.WebEndpoint;
import jakarta.xml.ws.WebServiceClient;
import jakarta.xml.ws.WebServiceFeature;
import jakarta.xml.ws.Service;

/**
 * This class was generated by Apache CXF 4.0.3
 * 2023-10-30T12:56:23.453-07:00
 * Generated source version: 4.0.3
 *
 */
@WebServiceClient(name = "ConnectionServiceRequester",
                  wsdlLocation = "file:/Users/haniotak/ij/stack/oscars/nsi/src/main/resources/schema/ogf_nsi_connection_requester_v2_0.wsdl",
                  targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/requester")
public class ConnectionServiceRequester extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://schemas.ogf.org/nsi/2013/12/connection/requester", "ConnectionServiceRequester");
    public final static QName ConnectionServiceRequesterPort = new QName("http://schemas.ogf.org/nsi/2013/12/connection/requester", "ConnectionServiceRequesterPort");
    static {
        URL url = null;
        try {
            url = new URL("file:/Users/haniotak/ij/stack/oscars/nsi/src/main/resources/schema/ogf_nsi_connection_requester_v2_0.wsdl");
        } catch (MalformedURLException e) {
            java.util.logging.Logger.getLogger(ConnectionServiceRequester.class.getName())
                .log(java.util.logging.Level.INFO,
                     "Can not initialize the default wsdl from {0}", "file:/Users/haniotak/ij/stack/oscars/nsi/src/main/resources/schema/ogf_nsi_connection_requester_v2_0.wsdl");
        }
        WSDL_LOCATION = url;
    }

    public ConnectionServiceRequester(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public ConnectionServiceRequester(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public ConnectionServiceRequester() {
        super(WSDL_LOCATION, SERVICE);
    }

    public ConnectionServiceRequester(WebServiceFeature ... features) {
        super(WSDL_LOCATION, SERVICE, features);
    }

    public ConnectionServiceRequester(URL wsdlLocation, WebServiceFeature ... features) {
        super(wsdlLocation, SERVICE, features);
    }

    public ConnectionServiceRequester(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
        super(wsdlLocation, serviceName, features);
    }




    /**
     *
     * @return
     *     returns ConnectionRequesterPort
     */
    @WebEndpoint(name = "ConnectionServiceRequesterPort")
    public ConnectionRequesterPort getConnectionServiceRequesterPort() {
        return super.getPort(ConnectionServiceRequesterPort, ConnectionRequesterPort.class);
    }

    /**
     *
     * @param features
     *     A list of {@link jakarta.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns ConnectionRequesterPort
     */
    @WebEndpoint(name = "ConnectionServiceRequesterPort")
    public ConnectionRequesterPort getConnectionServiceRequesterPort(WebServiceFeature... features) {
        return super.getPort(ConnectionServiceRequesterPort, ConnectionRequesterPort.class, features);
    }

}