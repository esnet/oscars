package net.es.oscars.app.props;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "frontend")
@Component
@NoArgsConstructor
public class FrontendProperties {
  String oauthClientId;
  String oauthAuthEndpoint;
  String oauthTokenEndpoint;
  String oauthLogoutEndpoint;
  String oauthRedirectUri;
  String oauthScope;

}
