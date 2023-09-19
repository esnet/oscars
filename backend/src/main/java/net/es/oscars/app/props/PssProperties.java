package net.es.oscars.app.props;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "pss")
@Data
@Component
@NoArgsConstructor
public class PssProperties {
    private String serverType;

    private String url;

    private String profile;

    private Integer configTimeoutSec;

    private String username;

    private String password;


}