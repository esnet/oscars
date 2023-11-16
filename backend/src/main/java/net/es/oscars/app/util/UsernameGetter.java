package net.es.oscars.app.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

public class UsernameGetter {
    public static String username(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getClaimAsString("preferred_username");
    }
}
