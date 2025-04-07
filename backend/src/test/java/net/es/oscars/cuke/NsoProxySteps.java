package net.es.oscars.cuke;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.sb.nso.LiveStatusOperationalStateCacheManager;
import net.es.oscars.sb.nso.NsoProxy;
import net.es.oscars.sb.nso.rest.*;
import org.junit.experimental.categories.Category;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.ArrayList;

@Slf4j
@Category({UnitTests.class})
public class NsoProxySteps extends CucumberSteps {

    @Autowired
    private NsoProxy proxy;

    @Autowired
    LiveStatusOperationalStateCacheManager liveStatusOperationalStateCacheManager;

    String liveStatus = "";
    RestClientException restClientException;

    MacInfoServiceResult macInfoServiceResult;

    ArrayList<LiveStatusSapResult> sapResults;
    ArrayList<LiveStatusSdpResult> sdpResults;
    ArrayList<LiveStatusLspResult> lspResults;

    @When("^The getLiveStatusShow method is called with device \"([^\"]*)\" and arguments \"([^\"]*)\"$")
    public void theGetLiveStatusShowMethodIsCalledWithDeviceAndArguments(String arg0, String arg1) throws Throwable {
        LiveStatusRequest liveStatusRequest = new LiveStatusRequest(arg0, arg1);
        liveStatus = proxy.getLiveStatusShow(liveStatusRequest);
        // log.info(liveStatus);
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
    @Then("The resulting esnet-status response contains {string}")
    public void theResultingEsnetStatusResponseContains(String arg) {
        assert liveStatus.contains(arg);
    }

    @Then("The resulting esnet-status response is not an error message")
    public void theResultingEsnetStatusResponseContains() {
        assert !liveStatus.contains("esnet-status error");
    }

    @Then("The resulting esnet-status response is empty")
    public void theResultingEsnetStatusResponseIsEmpty() {
        assert liveStatus.isEmpty();
    }

}
