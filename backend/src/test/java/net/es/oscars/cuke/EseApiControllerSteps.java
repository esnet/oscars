package net.es.oscars.cuke;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.BackendTestConfiguration;
import net.es.oscars.app.Startup;
import net.es.oscars.app.util.UsernameGetter;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.model.Bundle;
import net.es.oscars.model.Endpoint;
import net.es.oscars.model.Interval;
import net.es.oscars.model.L2VPN;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.Components;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.ent.Held;
import net.es.oscars.resv.enums.*;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.resv.svc.L2VPNService;
import net.es.oscars.web.beans.BandwidthAvailabilityResponse;
import net.es.oscars.web.beans.ConnectionFilter;
import net.es.oscars.web.beans.v2.L2VPNList;
import net.es.oscars.web.beans.v2.ValidationResponse;
import net.es.oscars.web.rest.v2.EseApiController;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.*;

import static org.junit.Assert.assertEquals;

@Slf4j
@Category({UnitTests.class})
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        BackendTestConfiguration.class,
        Startup.class,
        ConnectionRepository.class,
        ConnService.class,
        L2VPNService.class,
        UsernameGetter.class,
        EseApiController.class,
        Authentication.class,
    }
)
public class EseApiControllerSteps extends CucumberSteps {
    @Autowired
    private CucumberWorld world;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Startup startup;

    @MockitoBean
    private Authentication authentication;

    @Autowired
    private MockSimpleConnectionHelper helper;

    private ResponseEntity<String> response;

    @MockitoBean
    L2VPNService l2VPNService;

    @MockitoBean
    UsernameGetter usernameGetter;

    @MockitoBean
    ConnService connSvc;

    private ObjectMapper mapper;

    @Autowired
    private EseApiController controller;

    @Before("@EseApiControllerSteps")
    public void before() throws Exception {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

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
//        setupMockL2VPNConversions();
        setupMockConnService();
        setupMockL2VPNService();
        setupMockUsernameGetter();
    }

//    private void setupMockL2VPNConversions() throws Exception {
//        Mockito
//            .when(
//                L2VPNConversions.fromConnection(Mockito.any(Connection.class))
//            )
//            .thenReturn(
//                generateMockL2VPN()
//            );
//    }

    private void setupMockConnService() throws Exception {
        connSvc = Mockito.mock(ConnService.class);
        Connection mockConn = generateMockConnection();
        Optional<Connection> mockConnOpt = Optional.of(mockConn);

        // Mock ConnService.findConnection()
        // ...requires ConnService.held data property (Map<String, Connection>)
        connSvc.setHeld(Map.of("ABCD", mockConn));
        Mockito
            .doReturn(mockConnOpt).when(
            connSvc
        ).findConnection(Mockito.anyString());
    }

    private void setupMockL2VPNService() throws Exception {
        l2VPNService = Mockito.mock(L2VPNService.class);

        l2VPNService.setConnSvc(connSvc);

        // TODO: Setup mock handlers for L2VPNService class methods
        // Mock L2VPNService.get()
        L2VPN mockL2VPN = generateMockL2VPN();

        Mockito
            .doReturn(
                mockL2VPN
            )
            .when(
                l2VPNService
            ).get(
                Mockito.anyString()
            );

        // Mock L2VPNService.list()
        List<L2VPN> mockL2VPNs = new ArrayList<>();
        mockL2VPNs.add(mockL2VPN);

        L2VPNList mockL2VPNList = L2VPNList.builder()
            .page(1)
            .sizePerPage(1)
            .totalSize(1)
            .l2vpns(mockL2VPNs)
            .build();

        Mockito
            .when(
                l2VPNService.list(
                    Mockito.any(ConnectionFilter.class)
                )
            )
            .thenReturn(
                mockL2VPNList
            );

        // Mock L2VPNService.create()
        Mockito
            .when(
                l2VPNService.createOrReplace(Mockito.any(L2VPN.class))
            )
            .thenReturn(
                mockL2VPN
            );
        // Mock L2VPNService.bwAvailability()
        Mockito
            .when(
                l2VPNService.bwAvailability(Mockito.any(L2VPN.class))
            )
            .thenReturn(
                BandwidthAvailabilityResponse.builder()
                    .available(1)
                    .baseline(1)
                    .build()
            );
        Mockito
                .when(
                        l2VPNService.validate(Mockito.any(L2VPN.class), Mockito.any(ConnectionMode.class))
                )
                .thenReturn(
                        ValidationResponse.builder()
                                .valid(true)
                                .message("OK")
                                .build()
                );
        controller.setL2VPNService(l2VPNService);
    }

    private L2VPN generateMockL2VPN() {
        List<Bundle> mockBundles = new ArrayList<>();
        List<Endpoint> mockEndpoints = new ArrayList<>();

        mockBundles.add(
            Bundle.builder()
                .id(1L)
                .name("TestBundle1")
                .a("test1-cr6")
                .z("test2-cr6")
                .constraints(
                    Bundle.Constraints.builder()
                        .build()
                )
                .build()
        );

        mockEndpoints.add(
            Endpoint.builder()
                .id(1L)
                .device("test1-cr6")
                .port("1/1/c32/1")
                .vlan(122)
                .tagged(false)
                .build()
        );
        mockEndpoints.add(
            Endpoint.builder()
                .id(2L)
                .device("test2-cr6")
                .port("1/1/c32/2")
                .vlan(122)
                .tagged(false)
                .build()
        );
        return L2VPN.builder()
            .id(1L)
            .name("TEST")
            .meta(
                L2VPN.Meta.builder()
                    .username("test-user")
                    .build()
            )
            .schedule(
                Interval.builder()
                    .build()
            )
            .status(
                L2VPN.Status.builder()
                    .build()
            )
            .qos(
                L2VPN.Qos.builder()
                    .build()
            )
            .tech(
                L2VPN.Tech.builder()
                    .build()
            )
            .bundles(mockBundles)
            .endpoints(mockEndpoints)
            .build();
    }

    private void setupMockUsernameGetter() throws Exception {
        usernameGetter = Mockito.mock(UsernameGetter.class);

        // Mock UsernameGetter.username()
        Mockito
            .doReturn("test-user")
            .when(
                usernameGetter

            )
            .username(Mockito.any());

        controller.setUsernameGetter(usernameGetter);
    }

    @Given("The client executes {string} on EseApiController path {string}")
    public void theClientExecutesStringOnEseApiControllerPathString(String httpMethod, String httpPath) throws Throwable {
        HttpMethod method = HttpMethod.valueOf(httpMethod);
        try {
            log.info("Executing " + httpMethod + " on EseApiController path " + httpPath);
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
        } catch (Exception ex) {
            world.add(ex);
            log.error(ex.getLocalizedMessage(), ex);
        }
    }

    @Given("The client executes POST with a ConnectionFilter payload on EseApiController path {string}")
    public void theClientExecutesPOSTWithAConnectionFilterPayloadOnEseApiControllerPath(String httpPath) throws Throwable {
        HttpMethod method = HttpMethod.POST;
        try {
            log.info("Executing " + method + " on EseApiController path " + httpPath);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            ConnectionFilter requestPayload = ConnectionFilter.builder()
                .connectionId("ABCD")
                .sizePerPage(1)
                .page(1)
                .build();

            String payload = mapper.writeValueAsString(requestPayload);

            HttpEntity<String> entity = new HttpEntity<>(payload, headers);

            response = restTemplate.exchange(httpPath, method, entity, String.class);
        } catch (Exception ex) {
            world.add(ex);
            log.error(ex.getLocalizedMessage(), ex);
        }
    }

    @Given("The client executes POST with a L2VPN payload on EseApiController path {string}")
    public void theClientExecutesPOSTWithALVPNPayloadOnEseApiControllerPath(String httpPath) throws Throwable {
        HttpMethod method = HttpMethod.POST;
        try {
            log.info("Executing " + method + " on EseApiController path " + httpPath);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            L2VPN requestPayload = generateMockL2VPN();

            String payload = mapper.writeValueAsString(requestPayload);

            HttpEntity<String> entity = new HttpEntity<>(payload, headers);

            response = restTemplate.exchange(httpPath, method, entity, String.class);
        } catch (Exception ex) {
            world.add(ex);
            log.error(ex.getLocalizedMessage(), ex);
        }
    }

    @When("The client receives a response from EseApiController")
    public void theClientReceivesAResponseFromEseApiController() {
        assert response != null;
    }

    @Then("The client receives a EseApiController response status code of {int}")
    public void theClientReceivesAEseApiControllerResponseStatusCodeOf(int statusCode) {
        log.info("response status code: " + response.getStatusCode());
        assertEquals(statusCode, response.getStatusCode().value());
    }

    @Then("The EseApiController response is a valid L2VPN object")
    public void theEseApiControllerResponseIsAValidLVPNObject() throws Exception {
        assert response != null;

        String payload = response.getBody();
        L2VPN responseObject = mapper.readValue(
            payload,
            L2VPN.class
        );
        assert responseObject != null;
    }

    @Then("The EseApiController response is a valid L2VPNList object")
    public void theEseApiControllerResponseIsAValidLVPNListObject() throws Exception {
        assert response != null;

        String payload = response.getBody();
        L2VPNList responseObject = mapper.readValue(
            payload,
            L2VPNList.class
        );
        assert responseObject != null;
    }

    @Then("The EseApiController response L2VPN object's meta username property matches {string}")
    public void theEseApiControllerResponseMetaUsernamePropertyMatches(String expectedUsername) throws JsonProcessingException {
        assert response != null;
        String payload = response.getBody();
        L2VPN responseObject = mapper.readValue(
            payload,
            L2VPN.class
        );
        assert responseObject != null;
        assert responseObject.getMeta().getUsername().equals(expectedUsername);
    }

    @Then("The EseApiController response is a valid BandwidthAvailabilityResponse object")
    public void theEseApiControllerResponseIsAValidBandwidthAvailabilityResponseObject() throws JsonProcessingException {
        assert response != null;
        String payload = response.getBody();
        BandwidthAvailabilityResponse responseObject = mapper.readValue(
            payload,
            BandwidthAvailabilityResponse.class
        );
        assert responseObject != null;
    }

    private Connection generateMockConnection() {
        return Connection.builder()
            .id(1L)
            .held( // Required, or /protected/modify/description result will become an HTTP 500 Internal Server Error
                Held.builder()
                    .connectionId("ABCD")
                    .cmp(
                        Components.builder()
                            .id(1L)
                            .fixtures(new ArrayList<>())
                            .junctions(new ArrayList<>())
                            .pipes(new ArrayList<>())
                            .build()
                    )
                    .expiration(Instant.now().plusSeconds(60 * 20)) // 20 minutes from now
                    .schedule(helper.createValidSchedule())
                    .build()
            )
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
            .build();
    }

    @And("The EseApiController response is a valid ValidationResponse object")
    public void theEseApiControllerResponseIsAValidValidationResponseObject() throws JsonProcessingException {
        assert response != null;
        String payload = response.getBody();
        ValidationResponse responseObject = mapper.readValue(payload, ValidationResponse.class);
        assert responseObject != null;
        assert responseObject.isValid();
    }
}
