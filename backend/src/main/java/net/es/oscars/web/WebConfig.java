package net.es.oscars.web;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.FrontendProperties;
import net.es.oscars.app.props.StartupProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
@Slf4j
public class WebConfig implements WebMvcConfigurer {
    private final StartupProperties startupProperties;
    private final FrontendProperties frontendProperties;

    public WebConfig(StartupProperties startupProperties, FrontendProperties frontendProperties) {
        this.startupProperties = startupProperties;
        this.frontendProperties = frontendProperties;
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {

        if (startupProperties.getStandalone()) {
            registry.addMapping("/**")
                    .allowedOrigins("*")
                    .allowedMethods("*")
                    .allowedHeaders("*")
                    .allowCredentials(true);
        } else {
            registry.addMapping("/**")
                    .allowedOrigins(frontendProperties.getCorsOrigins())
                    .allowedMethods("*")
                    .allowedHeaders("*")
                    .allowCredentials(true);
        }
    }
}