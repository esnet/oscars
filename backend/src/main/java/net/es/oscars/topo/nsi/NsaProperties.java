package net.es.oscars.topo.nsi;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * This class fetches the NSI NSA contact info for the config
 */
@ConfigurationProperties(prefix = "nsa")
@Data
@Component
@NoArgsConstructor
public class NsaProperties {
    private String firstname;
    private String lastname;
    private String email;
    private String baseurl;
    private String provider;
}
