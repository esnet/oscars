package net.es.oscars.cuke;

import com.fasterxml.jackson.databind.json.JsonMapper;
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
import net.es.oscars.web.simple.SimpleConnection;
import net.es.oscars.web.simple.Validity;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
    private RestTestClient restTestClient;

    @Autowired
    private MockSimpleConnectionHelper helper;

    @Autowired
    private Startup startup;

    @MockitoBean
    private ConnectionRepository connRepo;

    @MockitoBean
    private ConnService connSvc;

    @Autowired
    private HoldController controller;

    private EntityExchangeResult<String> response;

    @Before("@HoldControllerSteps")
    public void before() throws Exception {
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

    private void setupDatasources() throws Exception {
        setupMockConnRepo();
        setupMockConnSvc();
    }

    private void setupMockConnRepo() {
        connRepo = Mockito.mock(ConnectionRepository.class);
        List<Connection> mockConnections = new ArrayList<>();
        mockConnections.add(
            generateMockConnection()
        );

        Mockito.when(
            connRepo
                .findByPhase(Mockito.any(Phase.class))
        ).thenReturn(
            mockConnections
        );

        controller.setConnRepo(connRepo);
    }
    private Connection generateMockConnection() {
        return generateMockConnection(null);
    }
    private Connection generateMockConnection(String projectId) {
        Set<String> projectIds = new HashSet<>();
        projectIds.add(projectId);

        return Connection.builder()
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
            .projectIds(projectIds)
            .build();
    }
    private void setupMockConnSvc() throws Exception {
        connSvc = Mockito.mock(ConnService.class);

        // Mock ConnService.extendHold()
        Mockito
            .when(
                connSvc.extendHold(Mockito.anyString()
            ))
            .thenReturn(
                Instant.now()
            );
        // Mock ConnService.validate()
        Mockito
            .when(
                connSvc.validate(
                    Mockito.any(SimpleConnection.class),
                    Mockito.any(ConnectionMode.class)
                )
            )
            .thenReturn(
                Validity.builder()
                    .valid(true)
                    .message("valid test message")
                    .build()
            );

        // Mock ConnService.holdConnection(), returns Tuple <SimpleConnection, Connection>
        SimpleConnection simpleConnection = helper.createSimpleConnection(
            "ABCD",
            10000,
            10000,
            10000,
            10000,
            10000
        );

        SimpleConnection simpleConnectionWithProjectId = helper.createSimpleConnection(
            "ABCD",
            10000,
            10000,
            10000,
            10000,
            10000,
            "ABCD-1234-EFGH-5678"
        );

        Pair<SimpleConnection, Connection> mockHoldConnection = Pair.of(
            simpleConnection,
            generateMockConnection()
        );
        Pair<SimpleConnection, Connection> mockHoldConnectionWithProjectId = Pair.of(
            simpleConnectionWithProjectId,
            generateMockConnection("ABCD-1234-EFGH-5678")
        );
        Mockito
            .when(
                connSvc.holdConnection(Mockito.any(SimpleConnection.class))
            )
            .thenAnswer(
                invocation -> {
                    Pair<SimpleConnection, Connection> mockResult = null;
                    SimpleConnection s = (SimpleConnection) invocation.getArgument(0);
                    if (s.getProjectIds() == null) {
                        mockResult = mockHoldConnection;
                    } else {
                        mockResult = mockHoldConnectionWithProjectId;
                    }
                    
                    return mockResult;
                }
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
                response = restTestClient.get().uri(httpPath).exchange().returnResult(String.class);
            } else if (method == HttpMethod.DELETE) {
                response = restTestClient.delete().uri(httpPath)
                        .accept(MediaType.APPLICATION_JSON)
                        .exchange().returnResult(String.class);
            } else {
                throw new Throwable("Unsupported HTTP method " + method);
            }
        } catch (Exception ex) {
            world.add(ex);
            log.error(ex.getLocalizedMessage(), ex);
        }
    }

    @Given("The client executes POST with SimpleConnection payload on HoldController path {string}")
    public void theClientExecutesWithSimpleConnectionPayloadOnHoldControllerPath(String httpPath) throws Throwable {
        try {
            log.info("Executing POST on HoldController path " + httpPath);

            JsonMapper mapper = new JsonMapper();
            SimpleConnection simpleConnection = helper.createSimpleConnection(
                "ABCD",
                10000,
                10000,
                10000,
                10000,
                10000
            );
            String payload = mapper.writeValueAsString(simpleConnection);

            response = restTestClient.post().uri(httpPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .exchange().returnResult(String.class);
        } catch (Exception ex) {
            world.add(ex);
            log.error(ex.getLocalizedMessage(), ex);
        }
    }

    @Given("The client executes POST with SimpleConnection payload on HoldController path {string} and projectId {string}")
    public void theClientExecutesWithSimpleConnectionPayloadOnHoldControllerPathAndProjectId(String httpPath, String projectId) throws Throwable {
        try {
            log.info("Executing POST on HoldController path " + httpPath);

            JsonMapper mapper = new JsonMapper();
            SimpleConnection simpleConnection = helper.createSimpleConnection(
                "ABCD",
                10000,
                10000,
                10000,
                10000,
                10000,
                projectId
            );
            String payload = mapper.writeValueAsString(simpleConnection);

            response = restTestClient.post().uri(httpPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .exchange().returnResult(String.class);
            log.info("response from {} is {}", httpPath, response.getStatus());
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
        log.info("response status code: " + response.getStatus());
        assertEquals(statusCode, response.getStatus().value());
    }

    @Then("The HoldController response is a valid list of CurrentlyHeldEntry objects")
    public void theConnControllerGeneratedIDIsValid() throws Throwable {
        JsonMapper mapper = new JsonMapper();
        assertNotNull(response.getResponseBody());
        String payload = response.getResponseBody();

        CurrentlyHeldEntry[] currentlyHeldEntries = mapper.readValue(payload, CurrentlyHeldEntry[].class);
        List<CurrentlyHeldEntry> list = Arrays.asList(currentlyHeldEntries);

        assertNotNull(list);
        assert !list.isEmpty();
    }

    @Then("The HoldController response is a valid Instant object")
    public void theHoldControllerResponseIsAValidInstantObject() {
        assertNotNull(response.getResponseBody());
        String payload = response.getResponseBody();
        log.error(payload);
        double timestampDouble = Double.parseDouble(payload);
        long seconds = (long) timestampDouble;
        long nanos = (long) ((timestampDouble - seconds) * 1_000_000_000);
        Instant instant = Instant.ofEpochSecond(seconds, nanos);

        assertNotNull(instant);
    }

    @Then("The HoldController response is a valid SimpleConnection")
    public void theHoldControllerResponseIsAValidSimpleConnection() {
        assertNotNull(response.getResponseBody());
        String payload = response.getResponseBody();
        JsonMapper mapper = new JsonMapper();
        try {
            SimpleConnection simpleConnection = mapper.readValue(payload, SimpleConnection.class);
            assertNotNull(simpleConnection);
        } catch (Exception ex) {
            world.add(ex);
            log.error(ex.getLocalizedMessage(), ex);
        }
    }

    @Then("The HoldController response does not have a projectId field")
    public void theHoldControllerResponseDoesNotHaveAProjectIdField() {

    }

    @Then("The HoldController response does have a projectId field")
    public void theHoldControllerResponseDoesHaveAProjectIdField() {

    }
}
