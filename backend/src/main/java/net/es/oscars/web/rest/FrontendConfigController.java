package net.es.oscars.web.rest;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.AuthProperties;
import net.es.oscars.app.props.FrontendProperties;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
public class FrontendConfigController {

    private final FrontendProperties frontendProperties;
    private final AuthProperties authProperties;

    public FrontendConfigController(FrontendProperties frontendProperties, AuthProperties authProperties) {
        this.frontendProperties = frontendProperties;
        this.authProperties = authProperties;
    }


    @RequestMapping(value = "/api/frontend/oauth", method = RequestMethod.GET)
    @ResponseBody
    @Transactional
    public FrontendOauthConfig getOauthConfig() {

        return FrontendOauthConfig.builder()
                .enabled(authProperties.isOauthEnabled())
                .clientId(frontendProperties.getOauthClientId())
                .authorizationEndpoint(frontendProperties.getOauthAuthEndpoint())
                .logoutEndpoint(frontendProperties.getOauthLogoutEndpoint())
                .tokenEndpoint(frontendProperties.getOauthTokenEndpoint())
                .redirectUri(frontendProperties.getOauthRedirectUri())
                .scope(frontendProperties.getOauthScope())
                .allowedGroups(new ArrayList<>(authProperties.getUserGroups()))
                .build();
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FrontendOauthConfig {
        Boolean enabled;
        String clientId;
        String authorizationEndpoint;
        String tokenEndpoint;
        String logoutEndpoint;
        String redirectUri;
        String scope;
        List<String> allowedGroups;
    }

}
