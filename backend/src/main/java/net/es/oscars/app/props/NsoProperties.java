package net.es.oscars.app.props;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "nso")
@Data
@Component
@NoArgsConstructor
public class NsoProperties {
    @NonNull
    private String vcIdRange;

    @NonNull
    private String sdpIdRange;

    @NonNull
    private String sapQosIdRange;

    @NonNull
    public String uri;
    public String getUri() {
        if (uri.endsWith("/")) {
            return uri;
        } else {
            return uri+"/";
        }
    }

    public boolean mockLiveShowCommands;

    @NonNull
    public String routingDomain;

    @NonNull
    public String username;

    @NonNull
    public String password;

    public Integer retryAttempts;

    public Integer backoffMillisecs;

    public Boolean sdpIdsGloballyUnique;

}