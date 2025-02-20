package net.es.oscars;


import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@Configuration
@ComponentScan(lazyInit = true)
@EnableAutoConfiguration
@ContextConfiguration(
        loader = SpringBootContextLoader.class,
        classes = Backend.class
)
public class BackendTestConfiguration {
}