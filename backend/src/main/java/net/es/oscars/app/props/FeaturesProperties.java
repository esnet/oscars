package net.es.oscars.app.props;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@EnableConfigurationProperties(FeaturesProperties.class)
@ConfigurationProperties(prefix = "features")
@NoArgsConstructor
public class FeaturesProperties {
    @NonNull
    private Boolean untaggedPorts = false;
    @NonNull
    private Boolean qinqPorts = false;
}
