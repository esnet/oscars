package net.es.oscars.cuke;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.opentelemetry.api.OpenTelemetry;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.NsoProperties;
import net.es.oscars.app.props.StartupProperties;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.sb.nso.LiveStatusOperationalStateCacheManager;
import net.es.oscars.sb.nso.NsoProxy;
import net.es.oscars.sb.nso.rest.*;
import net.es.topo.common.dto.nso.IetfRestconfErrorResponse;
import org.junit.experimental.categories.Category;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@Category({UnitTests.class})
public class NsoProxySteps extends CucumberSteps {

    @Autowired
    private NsoProxy proxy;

    @Autowired
    LiveStatusOperationalStateCacheManager liveStatusOperationalStateCacheManager;

    @Mock
    RestTemplate restTemplate;

    private NsoProperties nsoProps;

    String liveStatus = "";
    RestClientException restClientException;

    MacInfoServiceResult macInfoServiceResult;

    ArrayList<LiveStatusSapResult> sapResults;
    ArrayList<LiveStatusSdpResult> sdpResults;
    ArrayList<LiveStatusLspResult> lspResults;



    @Before("@NsoProxySteps")
    public void before() throws Exception{
        log.info("---------- NsoProxySteps.java before");

        restTemplate = mock(RestTemplate.class);

        ObjectMapper mapper = new ObjectMapper();

        Map<String, String> argToResponseFilePath = Map.of(
                "router mpls lsp", "http/nso.esnet-status.nokia-show.router-mpls-lsp.response.json",
                "service id 7115 sdp", "http/nso.esnet-status.nokia-show.service-sdp.response.json",
                "service id 7115 sap", "http/nso.esnet-status.nokia-show.service-sap.response.json",
                "service fdb-info", "http/nso.esnet-status.nokia-show.service-fdb-info.response.json",
                "service id 7093 fdb detail", "http/nso.esnet-status.nokia-show.service-fdb-detail.response.json"
        );
        argToResponseFilePath.forEach((command, filePath) -> {
            LiveStatusRequest request = new LiveStatusRequest("loc1-cr6", command);

            try {
                log.info("loading {}", filePath);
                File jsonFile = new ClassPathResource(filePath).getFile();
                LiveStatusOutput response = mapper.readValue(jsonFile, LiveStatusOutput.class);

                when(
                        restTemplate.postForObject(
                                "http://localhost:8080/restconf/data/esnet-status:esnet-status/nokia-show",
                                request,
                                LiveStatusOutput.class
                        )
                ).thenReturn(response);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        // we inject our rest template to the NSO proxy component

        proxy.setRestTemplate(restTemplate);

    }

    @Before("@NsoProxyUnhappySteps")
    public void beforeUnhappy() throws Exception{
        log.info("---------- NsoProxySteps.java before (unhappy path)");

        restTemplate = mock(RestTemplate.class);

        ObjectMapper mapper = new ObjectMapper();

        Map<String, String> argToResponseFilePath = Map.of(
                "service fdb-info", "http/nso.esnet-status.nokia.missing-device.response.json"
        );
        argToResponseFilePath.forEach((command, filePath) -> {
            LiveStatusRequest request = new LiveStatusRequest("does-not-exist-cr123", command);

            try {
                log.info("loading {}", filePath);
                File jsonFile = new ClassPathResource(filePath).getFile();
                IetfRestconfErrorResponse response = mapper.readValue(jsonFile, IetfRestconfErrorResponse.class);

                // May need to mock when rest template .exchange() is called to answer with headers (HTTP 400)
                String url = "http://localhost:8080/restconf/data/esnet-status:esnet-status/nokia-show";
                String expectedResponse = Files.readString(jsonFile.toPath());

                when(restTemplate.exchange(
                        eq(url),
                        eq(HttpMethod.POST),
                        Mockito.any(HttpEntity.class),
                        Mockito.eq(String.class)
                )).thenAnswer(invocation -> {
                    HttpEntity<?> actualRequest = invocation.getArgument(2);
                    HttpHeaders actualHeaders = actualRequest.getHeaders();

                    // @TODO assert headers
                    assertEquals("application/yang-data+json", actualHeaders.getFirst("Content-Type"));
                    return new ResponseEntity<>(expectedResponse, HttpStatus.BAD_REQUEST);
                });
                when(
                        restTemplate.postForObject(
                                url,
                                request,
                                IetfRestconfErrorResponse.class
                        )
                ).thenThrow(new RestClientException("expected exception"));

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        // we inject our rest template to the NSO proxy component

        proxy.setRestTemplate(restTemplate);

    }


    @When("^The getLiveStatusShow method is called with device \"([^\"]*)\" and arguments \"([^\"]*)\"$")
    public void theGetLiveStatusShowMethodIsCalledWithDeviceAndArguments(String arg0, String arg1) {
        LiveStatusRequest liveStatusRequest = new LiveStatusRequest(arg0, arg1);
        liveStatus = proxy.getLiveStatusShow(liveStatusRequest);
    }

    @When("^The getLiveStatusShow method is called with non-existent device \"([^\"]*)\" and arguments \"([^\"]*)\"$")
    public void theGetLiveStatusShowMethodIsCalledWithNonExistentDeviceAndArguments(String arg0, String arg1) {
        LiveStatusRequest liveStatusRequest = new LiveStatusRequest(arg0, arg1);
        try {
            liveStatus = proxy.getLiveStatusShow(liveStatusRequest);
        } catch (RestClientException ex) {
            liveStatus = "";
            restClientException = ex;
        }
    }

    @Then("The resulting esnet-status response is not empty")
    public void theResultingESNetStatusReportMatchesTheALUNEDFormat() {
        assert !liveStatus.isEmpty();
    }


    @When("I get macs for device {string} and service id {int}")
    public void getMacs(String arg0, Integer arg1) {
        macInfoServiceResult = liveStatusOperationalStateCacheManager.getMacs(arg0, arg1, Instant.now());
    }

    @When("I get SDPs for device {string} and service id {int}")
    public void getSDPs(String arg0, Integer arg1) {
        sdpResults = liveStatusOperationalStateCacheManager.getSdp(arg0, arg1, Instant.now());
    }

    @When("I get SAPs for device {string} and service id {int}")
    public void getSAPs(String arg0, Integer arg1) {
        sapResults = liveStatusOperationalStateCacheManager.getSap(arg0, arg1, Instant.now());
    }

    @When("I get LSPs for device {string}")
    public void getLSPs(String arg0) {
        lspResults = liveStatusOperationalStateCacheManager.getLsp(arg0, Instant.now());
        for (LiveStatusLspResult lspResult : lspResults) {
            assert lspResult.getStatus().equals(true);
        }
    }

    @Then("The resulting MAC report status is true")
    public void theResultingMacReportStatusTrue() {
        assert macInfoServiceResult.getStatus().equals(true);
    }

    @Then("The resulting SDP report contains {int} sdps")
    public void theResultingSdpReportContains(int arg0) {
        assert sdpResults.size() == arg0;
    }

    @Then("The resulting LSP report contains {int} lsps")
    public void theResultingLspReportContains(int arg0) {
        assert lspResults.size() == arg0;
    }

    @Then("The resulting SAP report contains {int} saps")
    public void theResultingSapReportContains(int arg0) {
        assert sapResults.size() == arg0;
    }


    @Then("The resulting esnet-status response is empty")
    public void theResultingEsnetStatusResponseIsEmpty() {
        assert liveStatus.isEmpty();
    }
}
