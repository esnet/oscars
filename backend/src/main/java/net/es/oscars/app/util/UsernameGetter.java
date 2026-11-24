package net.es.oscars.app.util;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.AuthProperties;
import net.es.oscars.app.props.StartupProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class UsernameGetter {
    private final AuthProperties authProperties;
    private final StartupProperties startupProperties;

    public UsernameGetter(AuthProperties authProperties, StartupProperties startupProperties) {
        this.authProperties = authProperties;
        this.startupProperties = startupProperties;
    }

    public String username(Authentication authentication) {
        if (!authProperties.isOauthEnabled() || startupProperties.getStandalone() ) {
            return "anonymous";
        }
        if (authentication == null) {
            log.info("authentication is null");
            throw new RuntimeException("authentication is null");
        }
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getClaimAsString("preferred_username");
    }
}
