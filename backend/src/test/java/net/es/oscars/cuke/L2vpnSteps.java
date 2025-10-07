package net.es.oscars.cuke;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
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
import net.es.oscars.resv.enums.ConnectionMode;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.resv.svc.L2VPNService;
import net.es.oscars.topo.beans.TopoException;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.pop.ConsistencyException;
import net.es.oscars.topo.pop.TopoPopulator;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.oscars.web.beans.ConnException;
import net.es.oscars.web.beans.ConnectionFilter;
import net.es.oscars.web.beans.v2.L2VPNList;
import net.es.oscars.web.beans.v2.ValidationResponse;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;

@Slf4j
@Category({UnitTests.class})
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                BackendTestConfiguration.class
        }
)
public class L2vpnSteps extends CucumberSteps {
    private L2VPN request;
    private ValidationResponse validationResponse;
    private boolean gotSubmitException;
    private boolean gotListException;
    private L2VPNList l2VPNList;
    private ResponseEntity<String> response;

    @Autowired
    private CucumberWorld world;

    @Autowired
    private Startup startup;

    @Autowired
    private TopoPopulator topoPopulator;

    @Autowired
    private TopologyStore topologyStore;

    @Autowired
    private L2VPNService l2VPNService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRestTemplate restTemplate;

    @Before("@L2vpnSteps")
    public void prepare()  {
        try {
            Topology t = topoPopulator.loadTopology("topo/esnet.json");
            topologyStore.replaceTopology(t);
            startup.setInStartup(false);
        } catch (TopoException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Given("I have loaded the {string} L2VPN request")
    public void iHaveLoadedTheL2VPNRequest(String path) throws IOException {
        var jsonFile = new ClassPathResource(path).getFile();

        request = objectMapper.readValue(
                jsonFile,
                L2VPN.class
        );
        // modify the schedule
        request.getSchedule().setBeginning(Instant.now().plus(1, ChronoUnit.MINUTES));
        request.getSchedule().setEnding(Instant.now().plus(30, ChronoUnit.MINUTES));
        world.l2vpn = request;
    }

    @When("I validate the new L2VPN request")
    public void iValidateTheL2VPNRequest() {
        validationResponse = l2VPNService.validate(request, ConnectionMode.NEW);
    }


    @When("I submit the L2VPN request")
    public void iSubmitTheL2VPNRequest() {
        gotSubmitException = false;
        try {
            l2VPNService.createOrReplace(request);
        } catch (ConnException | ConsistencyException e) {
            gotSubmitException = true;
        }
    }

    @Then("the L2VPN request validates successfully")
    public void theL2VPNRequestValidatesSuccessfully() {
        assert validationResponse.isValid();
    }

    @Then("the L2VPN request does not validate successfully")
    public void theL2VPNRequestDoesNotValidateSuccessfully() {
        assert !validationResponse.isValid();
    }

    @Then("the L2VPN request did not throw an exception")
    public void theL2VPNRequestNoException() {
        assert !gotSubmitException;
    }

    @Then("the L2VPN request did throw an exception")
    public void theL2VPNRequestDidThrowAnException() {
        assert gotSubmitException;
    }

    @Given("I have cleared all L2VPNs")
    public void iHaveClearedAllLV2PNs() {
        l2VPNService.clear();
    }

    @When("I list all L2VPNs")
    public void iListAllL2VPNs() {
        gotListException = false;
        ConnectionFilter filter = ConnectionFilter.builder()
                .sizePerPage(-1)
                .build();
        try {
            l2VPNList = l2VPNService.list(filter);
        } catch (ConnException | ConsistencyException e) {
            gotListException = true;
        }
    }


    @Then("the L2VPN list size is {int}")
    public void theLVPNListSizeIs(int arg) {
        assert l2VPNList.getL2vpns().size() == arg;
    }

    @Then("the L2VPN list contains {string}")
    public void theL2VPNListContains(String arg1) {
        AtomicBoolean found = new AtomicBoolean(false);
        l2VPNList.getL2vpns().forEach(l2vpn -> {
            if (l2vpn.getName().equals(arg1)) {
                found.set(true);
            }
        });
        assert found.get();
    }

    @Then("the L2VPN list did not throw an exception")
    public void theL2VPNListDidNotThrowAnException() {
        assert !gotListException;
    }


    @When("The REST client sends the L2VPN request as {string} on path {string}")
    public void theRESTClientSendsTheLVPNRequestAsPOSTOnPath(String httpMethod, String httpPath) throws Throwable {

        HttpMethod method = HttpMethod.valueOf(httpMethod);
        try {
            log.info("Executing {} on path {}", httpMethod, httpPath);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            String payload = objectMapper.writeValueAsString(request);
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);

            response = restTemplate.exchange(httpPath, method, entity, String.class);

        } catch (Exception ex) {
            world.add(ex);
            log.error(ex.getLocalizedMessage(), ex);
        }
    }
    @Then("The REST client received a status code of {int}")
    public void theClientReceivedAStatusCodeOf(int statusCode) {
        log.info("response status code: " + response.getStatusCode());
        assertEquals(statusCode, response.getStatusCode().value());
    }
}
