package net.es.oscars;


import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@Configuration
@ComponentScan(lazyInit = true)
@EnableAutoConfiguration(exclude={OAuth2ClientAutoConfiguration.class})
@ContextConfiguration(
        loader = SpringBootContextLoader.class,
        classes = Backend.class
)
public class BackendTestConfiguration {
}