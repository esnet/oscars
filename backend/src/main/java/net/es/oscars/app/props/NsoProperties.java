package net.es.oscars.app.props;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "nso")
@Data
@Component
@NoArgsConstructor
public class NsoProperties {
    private String vcIdRange;

    private String sdpIdRange;

    private String sapQosIdRange;
}