package net.es.oscars.web;


import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }
    @Order(1)
    @Bean
    public SecurityFilterChain clientFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(antMatcher("/api/**")).permitAll()
                        .requestMatchers(antMatcher("/services/**")).permitAll()
                        .requestMatchers(antMatcher("/protected/**")).authenticated()
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );


        return http.build();
    }

    @Order(2)
    @Bean
    public SecurityFilterChain resourceServerFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(new AntPathRequestMatcher("/protected/**"))
                .hasAuthority("OSCARS_USER")
                .anyRequest()
                .authenticated()
        );
        http.oauth2ResourceServer((oauth2) ->
                oauth2.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(new OscarsAuthenticationConverter())
                )
        );
        return http.build();
    }
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .build();
    }

    @Slf4j
    public static class OscarsAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
        private final List<String> allowedGroups;
        private final List<String> adminGroups;
        public OscarsAuthenticationConverter() {
            allowedGroups = new ArrayList<>();
            adminGroups = new ArrayList<>();
            allowedGroups.add("svc_network_admin");
            allowedGroups.add("svc_seg_admin");
            allowedGroups.add("svc_oscars_user");
            allowedGroups.add("svc_oscars_admin");
            adminGroups.add("svc_oscars_admin");
        }

        @Override
        public AbstractAuthenticationToken convert(@NonNull Jwt source) {
            return new JwtAuthenticationToken(source,
                    Stream.concat(new JwtGrantedAuthoritiesConverter().convert(source).stream(),
                            extractGroupRoles(source).stream()).collect(toSet()));
        }

        private Collection<? extends GrantedAuthority> extractGroupRoles(Jwt jwt) {
            Set<SimpleGrantedAuthority> authorities = new HashSet<>();
            List<String> tokenGroups = new ArrayList<>(jwt.getClaim("groups"));

            for (String group : allowedGroups) {
                if (tokenGroups.contains(group)) {
                    log.info("is an oscars user");
                    authorities.add(new SimpleGrantedAuthority("ROLE_OSCARS_USER"));
                }
            }
            for (String group : adminGroups) {
                if (tokenGroups.contains(group)) {
                    log.info("is an oscars admin");
                    authorities.add(new SimpleGrantedAuthority("ROLE_OSCARS_ADMIN"));
                }
            }

            return authorities;

        }
    }
}
