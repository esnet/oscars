package net.es.oscars.cuke;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.web.beans.NsoStateResponse;
import net.es.topo.common.dto.nso.NsoVPLS;
import org.springframework.http.HttpMethod;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;

@Slf4j
@Category({UnitTests.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class NsoSyncControllerSteps extends CucumberSteps {

    @Autowired
    private RestTestClient restTestClient;
    @Autowired
    private Startup startup;

    private EntityExchangeResult<String> response;

    @Before("@RestNsoSync")
    public void before() {
        startup.setInStartup(false);
    }

    @Given("the client executes {string} on {string}")
    public void theClientExecutesOn(String arg0, String arg1) throws Throwable {
        HttpMethod method = HttpMethod.valueOf(arg0);
        if (method == HttpMethod.GET) {
            response = restTestClient.get().uri(arg1).exchange().returnResult(String.class);
        } else if (method == HttpMethod.DELETE) {
            response = restTestClient.delete().uri(arg1)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange().returnResult(String.class);
        } else {
            throw new Throwable("Unsupported HTTP method " + method);
        }
        assertEquals(HttpStatus.OK, response.getStatus());
    }

    @Given("the client executes {string} on {string} with payload from {string}")
    public void theClientExecutesOnWith(String arg0, String arg1, String arg2) throws Throwable {
        try {
            HttpMethod method = HttpMethod.valueOf(arg0);

            InputStream bodyInputStream = new ClassPathResource(arg2).getInputStream();
            String payload = StreamUtils.copyToString(bodyInputStream, Charset.defaultCharset());

            switch (arg0.toUpperCase()) {
                case "POST":
                    response = restTestClient.post().uri(arg1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(payload)
                            .exchange().returnResult(String.class);
                    break;
                case "PUT":
                    response = restTestClient.put().uri(arg1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(payload)
                            .exchange().returnResult(String.class);
                    break;
                case "DELETE":
                    response = restTestClient.delete().uri(arg1)
                            .exchange().returnResult(String.class);
                    break;
                default:
                    throw new Throwable("Unsupported HTTP method " + method);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Throwable(e);
        }
        assertEquals(HttpStatus.OK, response.getStatus());
    }

    @When("the client receives the response")
    public void theClientReceivesTheResponse() throws Throwable {}

    @Then("the client receives the status code of {int}")
    public void theClientReceivesTheStatusCodeOf(int statusCode) throws Throwable {
        assert response.getStatus() == HttpStatus.valueOf(statusCode);
    }

    @Then("the client receives the payload")
    public void theClientReceivesThePayload() throws Throwable {
        assertNotNull(response.getResponseBody());
    }

    @Then("the client receives the payload {string}")
    public void theClientReceivesThePayload(String arg0) throws Exception {
        InputStream bodyInputStream = new ClassPathResource(arg0).getInputStream();
        String payload = StreamUtils.copyToString(bodyInputStream, Charset.defaultCharset());

        JsonMapper mapper = new JsonMapper();
        NsoStateResponse expectedResponse = mapper.readValue(payload, NsoStateResponse.class);
        NsoStateResponse actualResponse = mapper.readValue(response.getResponseBody(), NsoStateResponse.class);

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

        JsonMapper mapper = new JsonMapper();
        NsoStateResponse actualResponse = mapper.readValue(response.getResponseBody(), NsoStateResponse.class);

        assertTrue(actualResponse.isSynchronized());
    }
}
