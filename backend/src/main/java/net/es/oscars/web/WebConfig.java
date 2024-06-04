package net.es.oscars.web;

import lombok.NonNull;
import net.es.oscars.app.props.StartupProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {
    private final StartupProperties startupProperties;

    public WebConfig(StartupProperties startupProperties) {
        this.startupProperties = startupProperties;
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        if (startupProperties.getStandalone()) {
            registry.addMapping("/**");
        }
    }
}