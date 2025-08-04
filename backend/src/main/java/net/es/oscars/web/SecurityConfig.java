package net.es.oscars.web;


import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.AuthProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@Configuration
@EnableWebSecurity
@Slf4j
@Profile("default")
public class SecurityConfig {
    public static String ROLE_OSCARS_USER = "ROLE_OSCARS_USER";
    public static String ROLE_OSCARS_ADMIN = "ROLE_OSCARS_ADMIN";

    private final AuthProperties authProperties;

    public SecurityConfig(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    @Order(1)
    @Bean
    public SecurityFilterChain clientFilterChain(HttpSecurity http) throws Exception {

        http.securityMatcher("/api/**", "/services/**")
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize ->
                    authorize.anyRequest().permitAll()
                )
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Order(2)
    @Bean
    public SecurityFilterChain resourceServerFilterChain(HttpSecurity http) throws Exception {
        if (!authProperties.isOauthEnabled()) {
            http.securityMatcher("/protected/**")
                    .authorizeHttpRequests(authorize ->
                            authorize.anyRequest().permitAll()
                    )
                    .csrf(AbstractHttpConfigurer::disable);
        } else {
            http.securityMatcher("/protected/**")
                    .authorizeHttpRequests(authorize ->
                            authorize.anyRequest().hasAuthority(ROLE_OSCARS_USER)
                    )
                    .oauth2ResourceServer(oauth2 ->
                            oauth2.jwt(
                                    jwt -> jwt.jwtAuthenticationConverter(new OscarsAuthenticationConverter(authProperties))
                            )
                    )
                    .csrf(AbstractHttpConfigurer::disable);
        }
        return http.build();
    }


    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) ->
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, HttpStatus.UNAUTHORIZED.getReasonPhrase());
    }

    @Slf4j
    public static class OscarsAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
        private final AuthProperties authProperties;

        public OscarsAuthenticationConverter(AuthProperties authProperties) {
            this.authProperties = authProperties;
        }

        @Override
        public AbstractAuthenticationToken convert(@NonNull Jwt source) {
            return new JwtAuthenticationToken(source,
                    Stream.concat(new JwtGrantedAuthoritiesConverter().convert(source).stream(),
                            makeAuthsFromGroups(source).stream()).collect(toSet()));
        }

        private Collection<? extends GrantedAuthority> makeAuthsFromGroups(Jwt jwt) {

            Set<SimpleGrantedAuthority> authorities = new HashSet<>();
            List<String> tokenGroups = new ArrayList<>(jwt.getClaim("groups"));

            for (String group : authProperties.getUserGroups()) {
                if (tokenGroups.contains(group)) {
                    authorities.add(new SimpleGrantedAuthority(ROLE_OSCARS_USER));
                }
            }
            for (String group : authProperties.getAdminGroups()) {
                if (tokenGroups.contains(group)) {
                    authorities.add(new SimpleGrantedAuthority(ROLE_OSCARS_ADMIN));
                }
            }

            return authorities;

        }
    }
}
