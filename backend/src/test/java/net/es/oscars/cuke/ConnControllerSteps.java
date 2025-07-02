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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Collections;

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
    private TestRestTemplate restTemplate;
    private ResponseEntity<String> response;

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

    @When("The client receives a response from ConnController")
    public void theClientReceivesTheResponse() throws Throwable {
        assert response != null;
    }

    @Then("The client receives a ConnController response status code of {int}")
    public void theClientReceivesTheStatusCodeOf(int statusCode) throws Throwable {
        log.info("response status code: " + response.getStatusCode());
        assertEquals(statusCode, response.getStatusCode().value());
    }

    @Then("The client receives a ConnController response payload")
    public void theClientReceivesThePayload() throws Throwable {
        log.info("response body: " + response.getBody());
        assertNotNull(response.getBody());
    }

    @Then("The ConnController generated ID is valid")
    public void theConnControllerGeneratedIDIsValid() throws Throwable {
        assertNotNull(response.getBody());
        assert(response.getBody().matches("^[A-Z0-9]{4,}"));
    }

}
