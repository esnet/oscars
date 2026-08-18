package net.es.oscars.cuke;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.ctg.UnitTests;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
@Category({UnitTests.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConnControllerSteps extends CucumberSteps {
    private CucumberWorld world;

    @Autowired
    private Startup startup;

    @Autowired
    private RestTestClient restTestClient;
    private EntityExchangeResult<String> response;

    @Before("@ConnControllerSteps")
    public void before() {
        // Reset stuff
        clear();
        // Setup mock data sources
        setupDatasources();

        // Mock startup
        startup.setInStartup(false);
    }

    private void clear() {
        response = null;
    }

    private void setupDatasources() {
        // TODO: setup mock database
    }

    @Given("The client executes {string} on ConnController path {string}")
    public void theClientExecutesOnConnControllerPath(String httpMethod, String httpPath) throws Throwable {
        HttpMethod method = HttpMethod.valueOf(httpMethod);
        try {
            log.info("Executing " + httpMethod + " on ConnController path " + httpPath);
            if (method == HttpMethod.GET) {
                response = restTestClient.get().uri(httpPath).exchange().returnResult(String.class);
            } else if (method == HttpMethod.DELETE) {
                response = restTestClient.delete().uri(httpPath)
                        .accept(MediaType.APPLICATION_JSON)
                        .exchange().returnResult(String.class);
            } else {
                throw new Throwable("Unsupported HTTP method " + method);
            }
            assertEquals(HttpStatus.OK, response.getStatus());
        } catch (Exception ex) {
            world.add(ex);
            log.error(ex.getLocalizedMessage(), ex);
        }
    }

    @When("The client receives a response from ConnController")
    public void theClientReceivesTheResponse() throws Throwable {
        assert response != null;
    }

    @Then("The client receives a ConnController response status code of {int}")
    public void theClientReceivesTheStatusCodeOf(int statusCode) throws Throwable {
        log.info("response status code: " + response.getStatus());
        assertEquals(statusCode, response.getStatus().value());
    }

    @Then("The client receives a ConnController response payload")
    public void theClientReceivesThePayload() throws Throwable {
        log.info("response body: " + response.getResponseBody());
        assertNotNull(response.getResponseBody());
    }

    @Then("The ConnController generated ID is valid")
    public void theConnControllerGeneratedIDIsValid() throws Throwable {
        assertNotNull(response.getResponseBody());
        assert(response.getResponseBody().matches("^[A-Z0-9]{4,}"));
    }

}
