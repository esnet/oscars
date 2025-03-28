package net.es.oscars.soap;


import lombok.extern.slf4j.Slf4j;
import net.es.nsi.lib.soap.gen.nsi_2_0.services.point2point.ObjectFactory;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.requester.ConnectionRequesterPort;
import net.es.oscars.app.exc.NsiException;
import net.es.oscars.app.props.NsiProperties;
import net.es.oscars.app.props.ClientUtilProperties;
import net.es.oscars.nsi.beans.NsiErrors;
import net.es.oscars.nsi.ent.NsiRequesterNSA;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.apache.cxf.transport.http.HTTPConduit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.*;
import javax.xml.ws.handler.MessageContext;

@Slf4j
@Component
public class ClientUtil {

    @Autowired
    private NsiProperties nsiProps;

    @Autowired
    private ClientUtilProperties clientUtilProps;

    HashMap<String, ConnectionRequesterPort> requesterPorts = new HashMap<String, ConnectionRequesterPort>();


    /**
     * Creates a client for interacting with an NSA requester
     *
     * @param requesterNSA the details of the requester to contact
     * @return the ConnectionRequesterPort that you can use at the client
     */
    public ConnectionRequesterPort createRequesterClient(NsiRequesterNSA requesterNSA) throws NsiException {
        // if withGzipCompression is not set, default to application.properties setting for
        // client-util.enable-gzip-compression flag.
        return createRequesterClient(requesterNSA, false);
    }
    /**
     * Creates a client for interacting with an NSA requester
     *
     * @param requesterNSA the details of the requester to contact
*
     * @return the ConnectionRequesterPort that you can use at the client
     */
    public ConnectionRequesterPort createRequesterClient(NsiRequesterNSA requesterNSA, boolean withGzipCompression) throws NsiException {
        if (this.requesterPorts.containsKey(requesterNSA.getCallbackUrl())) {
            return this.requesterPorts.get(requesterNSA.getCallbackUrl());
        }

        JaxWsProxyFactoryBean fb = new JaxWsProxyFactoryBean();
        LoggingFeature lf = new LoggingFeature();
        lf.setPrettyLogging(true);
        fb.getFeatures().add(lf);

        Map<String, Object> props = fb.getProperties();
        if (props == null) {
            props = new HashMap<>();
        }
        props.put("jaxb.additionalContextClasses",
                new Class[]{
                        ObjectFactory.class
                });
        fb.setProperties(props);

        fb.setAddress(requesterNSA.getCallbackUrl());

        fb.setServiceClass(ConnectionRequesterPort.class);

        ConnectionRequesterPort port = (ConnectionRequesterPort) fb.create();
        Client client = ClientProxy.getClient(port);

        // Enable GZIP compression with appropriate headers in JaxWs Client if
        // client-util.enable-gzip-compression=true
        // or
        // withGzipCompression argument is true.
        if (clientUtilProps.enableGzipCompression || withGzipCompression) {
            log.debug("ClientUtil.createRequesterClient() - Gzip compression enabled");
//            Map<String, Object> requestHeaders = new HashMap<>();
//            requestHeaders.put("Accept-Encoding", new ArrayList<>(List.of("gzip")));

            // Ensure we sent the "Accept-Encoding: gzip" header to negotiate HTTP Compression
//            client.getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, requestHeaders);
            GZIPOutInterceptor gzipOutInterceptor = new GZIPOutInterceptor();

            GZIPInInterceptor gzipInInterceptor = new GZIPInInterceptor();
            
            gzipOutInterceptor.setForce(true);
            gzipOutInterceptor.setThreshold(7);

            client.getInInterceptors().add(gzipInInterceptor);
            client.getOutInterceptors().add(gzipOutInterceptor);
        }

        this.configureConduit(client, requesterNSA);

        this.requesterPorts.put(requesterNSA.getCallbackUrl(), port);

        return port;
    }

    private void configureConduit(Client client, NsiRequesterNSA requesterNSA) throws NsiException {
        HTTPConduit conduit = (HTTPConduit) client.getConduit();
        if (requesterNSA.getCallbackUrl().startsWith("https")) {

            try {
                KeyStore keyStore = KeyStore.getInstance(nsiProps.getKeyStoreType());
                InputStream fileStream = new FileInputStream(nsiProps.getKeyStore());
                keyStore.load(fileStream, nsiProps.getKeyStorePassword().toCharArray());

                TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmfactory.init(keyStore);
                TrustManager[] tms = tmfactory.getTrustManagers();

                KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmfactory.init(keyStore, nsiProps.getKeyStorePassword().toCharArray());
                KeyManager[] kms = kmfactory.getKeyManagers();

                TLSClientParameters tlsParams = new TLSClientParameters();
                tlsParams.setTrustManagers(tms);
                tlsParams.setKeyManagers(kms);
                conduit.setTlsClientParameters(tlsParams);


            } catch (KeyStoreException | IOException | UnrecoverableKeyException |
                     NoSuchAlgorithmException | CertificateException ex) {
                log.error(ex.getMessage(), ex);
                throw new NsiException(ex.getMessage(), NsiErrors.NRM_ERROR);
            }
        }
    }


}
