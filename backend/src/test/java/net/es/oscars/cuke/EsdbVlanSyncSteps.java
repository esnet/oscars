package net.es.oscars.cuke;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.NsoProperties;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.esdb.ESDBProxy;
import net.es.oscars.task.EsdbDataSync;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;

@Slf4j
@Category({UnitTests.class})
public class EsdbVlanSyncSteps extends CucumberSteps {
    @Autowired
    private EsdbDataSync esdbDataSync;

    @Autowired
    private ESDBProxy esdbProxy;

    @Autowired
    private NsoProperties nsoProperties;

    // Mock server (jetty) port is 50123 in testing.properties
    private final int mockPort = 50123;

    @Before("@EsdbVlanSyncSteps")
    public void before() throws IOException {
        log.info("EsdbVlanSyncSteps before() called.");
        esdbDataSync.setSynchronized(false);
        esdbDataSync.getStartup().setInStartup(false);
        esdbDataSync.getStartup().setInShutdown(false);

        // Override with our mock server URL and port
        esdbDataSync.getEsdbProperties().setGraphqlUri("http://localhost:" + mockPort + "/esdb_api/graphql");
        esdbDataSync.getEsdbProperties().setUri("http://localhost:" + mockPort + "/esdb_api/v1");
    }

    @Given("The ESDB VLAN task is ready")
    public void esdbVlanTaskIsReady() {
        assert esdbDataSync != null;
        assert !esdbDataSync.isSynchronized();
    }

    @When("The ESDB VLAN synchronization is triggered")
    public void theESDBVLANSynchronizationIsTriggered() {
        esdbDataSync.processingLoop();
    }

    @Then("The ESDB VLAN was synchronized")
    public void theESDBVLANWasSynchronized() {
        assert esdbDataSync.isSynchronized();
    }

}
