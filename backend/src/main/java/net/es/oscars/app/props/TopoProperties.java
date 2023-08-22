package net.es.oscars.app.props;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "topo")
@NoArgsConstructor
public class TopoProperties {
    @NonNull
    private String prefix;
    private String esdbKey;
    private String importScriptPath;

    private String url;
}
