package net.es.oscars.cuke;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.BackendTestConfiguration;
import net.es.oscars.app.Startup;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.enums.*;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.web.beans.CurrentlyHeldEntry;
import net.es.oscars.web.rest.HoldController;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Time;
import java.time.Instant;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
@Category({UnitTests.class})
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        BackendTestConfiguration.class,
        ConnectionRepository.class,
        ConnService.class,
        HoldController.class,
    }
)
public class HoldControllerSteps {
    @Autowired
    private CucumberWorld world;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Startup startup;

    @MockitoBean
    private ConnectionRepository connRepo;

    @MockitoBean
    private ConnService connSvc;

    @Autowired
    private HoldController controller;

    private ResponseEntity<String> response;

    @Before("@HoldControllerSteps")
    public void before() {
        // Reset stuff
        clear();

        // Setup mock data sources
        MockitoAnnotations.openMocks(this);
        setupDatasources();

        // Mock startup
        startup.setInStartup(false);
    }

    private void clear() {
        response = null;
    }

    private void setupDatasources() {
        setupMockConnRepo();
        setupMockConnSvc();
    }

    private void setupMockConnRepo() {
        connRepo = Mockito.mock(ConnectionRepository.class);
        List<Connection> mockConnections = new ArrayList<>();
        mockConnections.add(
            Connection.builder()
                .connectionId("ABCD")
                .phase(Phase.HELD)
                .mode(BuildMode.AUTOMATIC)
                .state(State.WAITING)
                .deploymentState(DeploymentState.UNDEPLOYED)
                .deploymentIntent(DeploymentIntent.SHOULD_BE_DEPLOYED)
                .username("test")
                .description("test description")
                .connection_mtu(10000)
                .last_modified( ((Long) Instant.now().getEpochSecond()).intValue() )
                .build()
        );

        Mockito.when(
            connRepo
                .findByPhase(Mockito.any(Phase.class))
        ).thenReturn(
            mockConnections
        );

        controller.setConnRepo(connRepo);
    }
    private void setupMockConnSvc() {
        connSvc = Mockito.mock(ConnService.class);
        Mockito
            .when(
                connSvc.extendHold(Mockito.anyString()
            ))
            .thenReturn(
                Instant.now()
            );
        connSvc.setConnRepo(connRepo);
        controller.setConnSvc(connSvc);
    }

    @Given("The client executes {string} on HoldController path {string}")
    public void theClientExecutesOnHoldControllerPath(String httpMethod, String httpPath) throws Throwable {
        HttpMethod method = HttpMethod.valueOf(httpMethod);
        try {
            log.info("Executing " + httpMethod + " on HoldController path " + httpPath);
            if (method == HttpMethod.GET) {
                 response = restTemplate.getForEntity(httpPath, String.class);
            } else if (method == HttpMethod.DELETE) {

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                HttpEntity<String> entity = new HttpEntity<>("", headers);

                response = restTemplate.exchange(httpPath, HttpMethod.DELETE, entity, String.class);
            } else {
                throw new Throwable("Unsupported HTTP method " + method);
            }
            assertEquals(HttpStatus.OK, response.getStatusCode());
        } catch (Exception ex) {
            world.add(ex);
            log.error(ex.getLocalizedMessage(), ex);
        }
    }

    @When("The client receives a response from HoldController")
    public void theClientReceivesTheResponse() throws Throwable {
        assert response != null;
    }

    @Then("The client receives a HoldController response status code of {int}")
    public void theClientReceivesTheStatusCodeOf(int statusCode) throws Throwable {
        log.info("response status code: " + response.getStatusCode());
        assertEquals(statusCode, response.getStatusCode().value());
    }

    @Then("The HoldController response is a valid list of CurrentlyHeldEntry objects")
    public void theConnControllerGeneratedIDIsValid() throws Throwable {
        ObjectMapper mapper = new ObjectMapper();
        assertNotNull(response.getBody());
        String payload = response.getBody();

        CurrentlyHeldEntry[] currentlyHeldEntries = mapper.readValue(payload, CurrentlyHeldEntry[].class);
        List<CurrentlyHeldEntry> list = Arrays.asList(currentlyHeldEntries);

        assertNotNull(list);
        assert !list.isEmpty();
    }

    @Then("The HoldController response is a valid Instant object")
    public void theHoldControllerResponseIsAValidInstantObject() {
        assertNotNull(response.getBody());
        String payload = response.getBody();
        double timestampDouble = Double.parseDouble(payload);
        long seconds = (long) timestampDouble;
        long nanos = (long) ((timestampDouble - seconds) * 1_000_000_000);
        Instant instant = Instant.ofEpochSecond(seconds, nanos);

        assertNotNull(instant);
    }
}
