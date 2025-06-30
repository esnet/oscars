package net.es.oscars.cuke;

import io.cucumber.java.Before;
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
import net.es.oscars.resv.svc.L2VPNService;
import net.es.oscars.web.rest.v2.EseApiController;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Category({UnitTests.class})
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        BackendTestConfiguration.class,
        Startup.class,
        L2VPNService.class,
        UsernameGetter.class,
        EseApiController.class,
    }
)
public class EseApiControllerSteps extends CucumberSteps {
    @Autowired
    private CucumberWorld world;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Startup startup;

    private ResponseEntity<String> response;

    @MockitoBean
    L2VPNService l2VPNService;

    @MockitoBean
    UsernameGetter usernameGetter;

    @Autowired
    private EseApiController controller;

    @Before("@EseApiControllerSteps")
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
        setupMockL2VPNService();
        setupMockUsernameGetter();
    }

    private void setupMockL2VPNService() throws Exception {
        l2VPNService = Mockito.mock(L2VPNService.class);

        // TODO: Setup mock handlers for L2VPNService class methods
        // Mock L2VPNService.get()
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

        L2VPN mockL2VPN = L2VPN.builder()
            .id(1L)
            .name("TEST")
            .meta(
                L2VPN.Meta.builder()
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

        Mockito
            .when(
                l2VPNService.get(
                    Mockito.anyString()
                )
            )
            .thenReturn(mockL2VPN);

        controller.setL2VPNService(l2VPNService);
    }

    private void setupMockUsernameGetter() throws Exception {
        usernameGetter = Mockito.mock(UsernameGetter.class);

        // TODO: Setup mock handlers for UsernameGetter class methods

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

    @When("The client receives a response from EseApiController")
    public void theClientReceivesAResponseFromEseApiController() {
        assert response != null;
    }

    @Then("The client receives a EseApiController response status code of {int}")
    public void theClientReceivesAEseApiControllerResponseStatusCodeOf(int httpStatusCode) {
        //
    }

    @Then("The EseApiController response is a valid L2VPN object")
    public void theEseApiControllerResponseIsAValidLVPNObject() {
        //
    }
}
