package net.es.oscars.app.props;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "esdb")
@Data
@Component
@NoArgsConstructor
public class EsdbProperties {
    @NonNull
    private String apiKey;

    @NonNull
    private String uri;

    public String getUri() {
        if (uri.endsWith("/")) {
            return uri;
        } else {
            return uri+"/";
        }
    }

}