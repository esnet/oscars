package net.es.oscars.cuke;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.nso.NsoHttpServer;
import net.es.oscars.sb.nso.LiveStatusOperationalStateCacheManager;
import net.es.oscars.sb.nso.NsoProxy;
import net.es.oscars.sb.nso.rest.*;
import net.es.topo.common.dto.nso.FromNsoCheckSync;
import net.es.topo.common.dto.nso.enums.NsoCheckSyncState;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.ArrayList;

@Slf4j
@Category({UnitTests.class})
public class NsoCheckSyncSteps extends CucumberSteps {

    @Autowired
    private NsoProxy proxy;

    private NsoCheckSyncState state;


    @Given("I mock check-sync to return {string}")
    public void iMockCheckSyncToReturn(String arg0) {
        NsoHttpServer.statusCode = HttpServletResponse.SC_OK;
        NsoHttpServer.nsoCheckSyncResult = FromNsoCheckSync.builder()
                .output(FromNsoCheckSync.CheckSyncOutput.builder()
                        .info("")
                        .result(NsoCheckSyncState.get(arg0))
                        .build())
                .build();
    }

    @When("The check-sync method is called")
    public void theCheckSyncMethodIsCalled() {
        this.state = proxy.checkSync("something");
    }

    @Then("The resulting check-sync response result is {string}")
    public void theResultingCheckSyncResponseResultIs(String arg0) {
        assert this.state.getValue().equals(arg0);
    }

    @Given("I mock the check-sync to return an empty body")
    public void iMockTheCheckSyncToReturnAnEmptyBody() {
        NsoHttpServer.statusCode = HttpServletResponse.SC_OK;
        NsoHttpServer.nsoCheckSyncResult = null;
    }

    @Given("I mock the check-sync to return null output")
    public void iMockTheCheckSyncToReturnNullOutput() {
        NsoHttpServer.statusCode = HttpServletResponse.SC_OK;
        NsoHttpServer.nsoCheckSyncResult = FromNsoCheckSync.builder()
                .output(null)
                .build();
    }

    @Given("I mock the check-sync to return null result")
    public void iMockTheCheckSyncToReturnAnEmptyResult() {
        NsoHttpServer.statusCode = HttpServletResponse.SC_OK;
        NsoHttpServer.nsoCheckSyncResult = FromNsoCheckSync.builder()
                .output(FromNsoCheckSync.CheckSyncOutput.builder()
                        .info(null)
                        .result(null)
                        .build())
                .build();
    }

    @Given("I mock the check-sync to return an NSO error")
    public void iMockTheCheckSyncToReturnAnNSOError() {
        NsoHttpServer.statusCode = HttpServletResponse.SC_OK;
        NsoHttpServer.nsoCheckSyncResult = FromNsoCheckSync.builder()
                .output(FromNsoCheckSync.CheckSyncOutput.builder()
                        .info("something bad happened")
                        .result(NsoCheckSyncState.ERROR)
                        .build())
                .build();
    }

    @Given("I mock the check-sync to return a REST error")
    public void iMockTheCheckSyncToReturnARESTError() {
        NsoHttpServer.statusCode = 500;
    }

}
