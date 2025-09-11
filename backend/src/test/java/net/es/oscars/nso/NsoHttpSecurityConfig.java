package net.es.oscars.nso;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Profile("test")
@Configuration
@EnableWebSecurity
public class NsoHttpSecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(
                "/api/**",
                "/services/**",
                "/protected/**",
                "/protected/conn/generateId",
                "/**"
            )
            .authorizeHttpRequests(
                authorizeRequests -> authorizeRequests
                    .anyRequest()
                    .permitAll()
            )
            .csrf(AbstractHttpConfigurer::disable)
        ;

        return http.build();
    }
}
