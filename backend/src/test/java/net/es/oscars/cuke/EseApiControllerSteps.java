package net.es.oscars.cuke;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import net.es.oscars.model.L2VPN;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.enums.*;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.resv.svc.L2VPNService;
import net.es.oscars.topo.beans.TopoException;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.pop.TopoPopulator;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.oscars.web.beans.BandwidthAvailabilityResponse;
import net.es.oscars.web.beans.ConnectionFilter;
import net.es.oscars.web.beans.v2.L2VPNList;
import net.es.oscars.web.beans.v2.ValidationResponse;
import net.es.oscars.web.rest.v2.EseApiController;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
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
    private RestTestClient restTestClient;

    @Autowired
    private Startup startup;

    @MockitoBean
    private Authentication authentication;

    private EntityExchangeResult<String> response;

    @Autowired
    private TopoPopulator topoPopulator;

    @Autowired
    private TopologyStore topologyStore;

    @MockitoBean
    L2VPNService l2VPNService;

    @MockitoBean
    UsernameGetter usernameGetter;

    @MockitoBean
    ConnService connSvc;

    private JsonMapper mapper;

    @Autowired
    private EseApiController controller;

    @Before("@EseApiControllerSteps")
    public void before() throws Exception {
        mapper = JsonMapper.builder()
                .build();

        try {
            Topology t = topoPopulator.loadTopology("topo/esnet.json");
            topologyStore.replaceTopology(t);
            startup.setInStartup(false);
        } catch (TopoException | IOException e) {
            throw new RuntimeException(e);
        }

        // Reset stuff
        clear();

        // Setup mock data sources
        MockitoAnnotations.openMocks(this);

        // Mock startup
        startup.setInStartup(false);
    }

    private void clear() {
        response = null;
    }

    @Given("The client executes {string} on EseApiController path {string}")
    public void theClientExecutesStringOnEseApiControllerPathString(String httpMethod, String httpPath) throws Throwable {
        HttpMethod method = HttpMethod.valueOf(httpMethod);
        try {
            log.info("Executing " + httpMethod + " on EseApiController path " + httpPath);
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

    @Given("The client executes POST with a ConnectionFilter payload on EseApiController path {string}")
    public void theClientExecutesPOSTWithAConnectionFilterPayloadOnEseApiControllerPath(String httpPath) throws Throwable {
        try {
            log.info("Executing POST on EseApiController path " + httpPath);

            ConnectionFilter requestPayload = ConnectionFilter.builder()
                .connectionId("ABCD")
                .sizePerPage(1)
                .page(1)
                .build();

            String payload = mapper.writeValueAsString(requestPayload);

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

    @Given("The client executes POST with a L2VPN payload on EseApiController path {string}")
    public void theClientExecutesPOSTWithALVPNPayloadOnEseApiControllerPath(String httpPath) throws Throwable {
        try {
            log.info("Executing POST on EseApiController path " + httpPath);

            L2VPN requestPayload = world.l2vpn;

            String payload = mapper.writeValueAsString(requestPayload);

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

    @When("The client receives a response from EseApiController")
    public void theClientReceivesAResponseFromEseApiController() {
        assert response != null;
    }

    @Then("The client receives a EseApiController response status code of {int}")
    public void theClientReceivesAEseApiControllerResponseStatusCodeOf(int statusCode) {
        log.info("response status code: " + response.getStatus());
        assertEquals(statusCode, response.getStatus().value());
    }

    @Then("The EseApiController response is a valid L2VPN object")
    public void theEseApiControllerResponseIsAValidLVPNObject() throws Exception {
        assert response != null;

        String payload = response.getResponseBody();
        L2VPN responseObject = mapper.readValue(
            payload,
            L2VPN.class
        );
        assert responseObject != null;
    }

    @Then("The EseApiController response is a valid L2VPNList object")
    public void theEseApiControllerResponseIsAValidLVPNListObject() throws Exception {
        assert response != null;

        String payload = response.getResponseBody();
        L2VPNList responseObject = mapper.readValue(
            payload,
            L2VPNList.class
        );
        assert responseObject != null;
    }

    @Then("The EseApiController response L2VPN object's meta username property matches {string}")
    public void theEseApiControllerResponseMetaUsernamePropertyMatches(String expectedUsername) throws JsonProcessingException {
        assert response != null;
        String payload = response.getResponseBody();
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
        String payload = response.getResponseBody();
        BandwidthAvailabilityResponse responseObject = mapper.readValue(
            payload,
            BandwidthAvailabilityResponse.class
        );
        assert responseObject != null;
    }


    @And("The EseApiController response is a valid ValidationResponse object")
    public void theEseApiControllerResponseIsAValidValidationResponseObject() throws JsonProcessingException {
        assert response != null;
        String payload = response.getResponseBody();
        ValidationResponse responseObject = mapper.readValue(payload, ValidationResponse.class);
        assert responseObject != null;
        assert responseObject.isValid();
    }
}
