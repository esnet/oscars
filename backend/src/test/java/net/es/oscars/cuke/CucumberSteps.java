package net.es.oscars.cuke;

import net.es.oscars.BackendTestConfiguration;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = "file:config/test/testing.properties")
@ContextConfiguration(
        loader = SpringBootContextLoader.class,
        classes = BackendTestConfiguration.class)
@ActiveProfiles(profiles = "test")
public class CucumberSteps {

}