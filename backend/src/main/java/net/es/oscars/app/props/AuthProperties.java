package net.es.oscars.app.props;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;


@ConfigurationProperties(prefix = "auth")
@Data
@Component
@NoArgsConstructor
public class AuthProperties {
    private List<String> userGroups;

    private List<String> adminGroups;

}