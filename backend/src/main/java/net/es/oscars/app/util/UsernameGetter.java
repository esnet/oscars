package net.es.oscars.app.util;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.AuthProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class UsernameGetter {
    private final AuthProperties authProperties;

    public UsernameGetter(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public String username(Authentication authentication) {
        if (!authProperties.isOauthEnabled()) {
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
