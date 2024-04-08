package net.es.oscars.app.props;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@EnableConfigurationProperties(StartupProperties.class)
@ConfigurationProperties(prefix = "startup")
@NoArgsConstructor
public class StartupProperties {
    @NonNull
    private Boolean exit = false;
    @NonNull
    private Boolean standalone = false;
    private String banner = "";
}
