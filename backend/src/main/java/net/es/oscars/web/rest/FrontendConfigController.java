package net.es.oscars.web.rest;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.FrontendProperties;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class FrontendConfigController {

    private final FrontendProperties frontendProperties;

    public FrontendConfigController(FrontendProperties frontendProperties) {
        this.frontendProperties = frontendProperties;
    }


    @RequestMapping(value = "/api/frontend/oauth", method = RequestMethod.GET)
    @ResponseBody
    @Transactional
    public FrontendOauthConfig getOauthConfig() {

        return FrontendOauthConfig.builder()
                .clientId(frontendProperties.getOauthClientId())
                .authorizationEndpoint(frontendProperties.getOauthAuthEndpoint())
                .logoutEndpoint(frontendProperties.getOauthLogoutEndpoint())
                .tokenEndpoint(frontendProperties.getOauthTokenEndpoint())
                .redirectUri(frontendProperties.getOauthRedirectUri())
                .scope(frontendProperties.getOauthScope())
                .build();
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FrontendOauthConfig {
        String clientId;
        String authorizationEndpoint;
        String tokenEndpoint;
        String logoutEndpoint;
        String redirectUri;
        String scope;
    }

}
