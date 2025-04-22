package net.es.oscars.cuke;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.web.beans.NsoStateResponse;
import net.es.topo.common.dto.nso.NsoVPLS;
import org.springframework.http.HttpMethod;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;

@Slf4j
@Category({UnitTests.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class NsoSyncControllerSteps extends CucumberSteps {

    @Autowired
    private TestRestTemplate restTemplate;
    private ResponseEntity<String> response;

    @Given("the client executes {string} on {string}")
    public void theClientExecutesOn(String arg0, String arg1) throws Throwable {
        HttpMethod method = HttpMethod.valueOf(arg0);
        if (method == HttpMethod.GET) {
            response = restTemplate.getForEntity(arg1, String.class);
        } else if (method == HttpMethod.DELETE) {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>("", headers);

            response = restTemplate.exchange(arg1, HttpMethod.DELETE, entity, String.class);
        } else {
            throw new Throwable("Unsupported HTTP method " + method);
        }
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Given("the client executes {string} on {string} with payload from {string}")
    public void theClientExecutesOnWith(String arg0, String arg1, String arg2) throws Throwable {
        try {
            HttpMethod method = HttpMethod.valueOf(arg0);

            InputStream bodyInputStream = new ClassPathResource(arg2).getInputStream();
            String payload = StreamUtils.copyToString(bodyInputStream, Charset.defaultCharset());
            response = new ResponseEntity<>("", HttpStatus.NOT_FOUND);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);

            switch (arg0.toUpperCase()) {
                case "POST":
                    response = restTemplate.postForEntity(arg1, entity, String.class);
                    break;
                case "PUT":

                    restTemplate.put(arg1, entity);
                    response = new ResponseEntity<>("", HttpStatus.OK);

                    break;
                case "DELETE":
                    restTemplate.delete(arg1, entity, String.class);
                    response = new ResponseEntity<>("", HttpStatus.OK);
                    break;
                default:
                    throw new Throwable("Unsupported HTTP method " + method);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Throwable(e);
        }
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @When("the client receives the response")
    public void theClientReceivesTheResponse() throws Throwable {}

    @Then("the client receives the status code of {int}")
    public void theClientReceivesTheStatusCodeOf(int statusCode) throws Throwable {
        assertEquals(statusCode, response.getStatusCode().value());
    }

    @Then("the client receives the payload")
    public void theClientReceivesThePayload() throws Throwable {
        assertNotNull(response.getBody());
    }

    @Then("the client receives a status code of {int}")
    public void theClientReceivesAStatusCodeOf(int arg0) {

    }

    @Then("the client receives the payload {string}")
    public void theClientReceivesThePayload(String arg0) throws Exception {
        InputStream bodyInputStream = new ClassPathResource(arg0).getInputStream();
        String payload = StreamUtils.copyToString(bodyInputStream, Charset.defaultCharset());

        ObjectMapper mapper = new ObjectMapper();
        NsoStateResponse expectedResponse = mapper.readValue(payload, NsoStateResponse.class);
        NsoStateResponse actualResponse = mapper.readValue(response.getBody(), NsoStateResponse.class);

        Comparator<NsoVPLS> expectedComparator = Comparator.comparingInt(NsoVPLS::getVcId);
        Comparator<NsoVPLS> actualComparator = Comparator.comparingInt(NsoVPLS::getVcId);

        List<NsoVPLS> expectedList = expectedResponse.getVpls();
        List<NsoVPLS> actualList = actualResponse.getVpls();
        expectedList.sort(expectedComparator);
        actualList.sort(actualComparator);

        assertEquals( expectedList, actualList);

    }

    @Then("the client receives a true synchronization flag")
    public void theClientReceivesASynchronizationFlag() throws Throwable {

        ObjectMapper mapper = new ObjectMapper();
        NsoStateResponse actualResponse = mapper.readValue(response.getBody(), NsoStateResponse.class);

        assertTrue(actualResponse.isSynchronized());
    }
}
