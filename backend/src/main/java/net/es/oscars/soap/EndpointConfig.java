package net.es.oscars.soap;


import net.es.nsi.lib.soap.gen.nsi_2_0.services.point2point.ObjectFactory;
import jakarta.xml.ws.Endpoint;
import net.es.oscars.app.props.NsiProperties;
import org.apache.cxf.Bus;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class EndpointConfig {

    @Autowired
    private Bus bus;

    @Autowired
    private NsiProvider nsiProvider;

    @Autowired
    private NsiProperties nsiProperties;

    @Bean
    public Endpoint endpoint() {

        EndpointImpl provider = new EndpointImpl(bus, nsiProvider);

        Map<String, Object> props = provider.getProperties();
        if (props == null) {
            props = new HashMap<>();
        }
        provider.setPublishedEndpointUrl(nsiProperties.getPublishedEndpointUrl());
        props.put("jaxb.additionalContextClasses",
                new Class[]{
                        ObjectFactory.class
                });
        provider.setProperties(props);


        LoggingFeature lf = new LoggingFeature();
        lf.setPrettyLogging(true);
        provider.getFeatures().add(lf);


        provider.publish("/provider");
        return provider;
    }

}