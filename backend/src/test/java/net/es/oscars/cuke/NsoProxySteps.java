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
import org.junit.experimental.categories.Category;

import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;

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


    @When("^The getLiveStatusShow method is called with device \"([^\"]*)\" and arguments \"([^\"]*)\"$")
    public void theGetLiveStatusShowMethodIsCalledWithDeviceAndArguments(String arg0, String arg1) {
        LiveStatusRequest liveStatusRequest = new LiveStatusRequest(arg0, arg1);
        liveStatus = proxy.getLiveStatusShow(liveStatusRequest);
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


}
