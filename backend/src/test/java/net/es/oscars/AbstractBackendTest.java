package net.es.oscars;

import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = BackendTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@CucumberContextConfiguration
@TestPropertySource(locations = "classpath:testing.properties")
//@ActiveProfiles(profiles = "test") // This is actually set in testing.properties

public abstract class AbstractBackendTest {


}