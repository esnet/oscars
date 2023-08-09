package net.es.oscars.soap;


import gen.nsi_2_0.services.point2point.ObjectFactory;
import lombok.extern.slf4j.Slf4j;
import gen.nsi_2_0.connection.requester.ConnectionRequesterPort;
import net.es.oscars.app.exc.NsiException;
import net.es.oscars.app.props.NsiProperties;
import net.es.oscars.nsi.beans.NsiErrors;
import net.es.oscars.nsi.ent.NsiRequesterNSA;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
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
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ClientUtil {

    @Autowired
    private NsiProperties nsiProps;

    HashMap<String, ConnectionRequesterPort> requesterPorts = new HashMap<String, ConnectionRequesterPort>();


    /**
     * Creates a client for interacting with an NSA requester
     *
     * @param requesterNSA the details of the requester to contact
     * @return the ConnectionRequesterPort that you can use at the client
     */
    public ConnectionRequesterPort createRequesterClient(NsiRequesterNSA requesterNSA) throws NsiException {
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
