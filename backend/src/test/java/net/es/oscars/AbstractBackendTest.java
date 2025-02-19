package net.es.oscars;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = BackendTestConfiguration.class)
@TestPropertySource(locations = "file:config/test/testing.properties")
@ActiveProfiles(profiles = "test")

public abstract class AbstractBackendTest {


}