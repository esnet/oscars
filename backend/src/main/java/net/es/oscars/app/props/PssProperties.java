package net.es.oscars.app.props;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "pss")
@Data
@Component
@NoArgsConstructor
public class PssProperties {
    @NonNull
    private String url;

    @NonNull
    private String profile;

    @NonNull
    private String serverType;

    @NonNull
    private Integer configTimeoutSec;


}