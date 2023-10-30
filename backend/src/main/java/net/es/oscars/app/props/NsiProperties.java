package net.es.oscars.app.props;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;

@Data
@ConfigurationProperties(prefix = "nsi")
@Component
@NoArgsConstructor
public class NsiProperties {
    private File keyStore;
    private String keyStoreType;
    private String keyStoreAlias;
    private String keyStorePassword;
    private String publishedEndpointUrl;

}

