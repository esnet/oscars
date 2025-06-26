package net.es.oscars;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@Configuration
@ComponentScan(lazyInit = true)
@ContextConfiguration(
        loader = SpringBootContextLoader.class,
        classes = Backend.class
)
public class BackendTestConfiguration {
}