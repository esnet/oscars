package net.es.oscars;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@AutoConfigureRestTestClient
@SpringBootTest(
    classes = BackendTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@CucumberContextConfiguration
@TestPropertySource(locations = "classpath:testing.properties")
//@ActiveProfiles(profiles = "test") // This is actually set in testing.properties

public abstract class AbstractBackendTest {


}