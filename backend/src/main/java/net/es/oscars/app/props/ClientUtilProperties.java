package net.es.oscars.app.props;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "client-util")
@Data
@Component
@NoArgsConstructor
public class ClientUtilProperties {
    public boolean enableGzipCompression = false;
}
