package net.es.oscars.cuke;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.NsoProperties;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.esdb.ESDBProxy;
import net.es.oscars.task.EsdbVlanSync;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.ClientResponseField;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;

@Slf4j
@Category({UnitTests.class})
public class EsdbVlanSyncSteps extends CucumberSteps {
    @Autowired
    private EsdbVlanSync esdbVlanSync;

    @Autowired
    private ESDBProxy esdbProxy;

    @Autowired
    private NsoProperties nsoProperties;

    // Mock server (jetty) port is 50123 in testing.properties
    private final int mockPort = 50123;

    @Before("@EsdbVlanSyncSteps")
    public void before() throws IOException {
        log.info("EsdbVlanSyncSteps before() called.");
        esdbVlanSync.setSynchronized(false);
        esdbVlanSync.getStartup().setInStartup(false);
        esdbVlanSync.getStartup().setInShutdown(false);

        // Override with our mock server URL and port
        esdbVlanSync.getEsdbProperties().setGraphqlUri("http://localhost:" + mockPort + "/esdb_api/graphql");
        esdbVlanSync.getEsdbProperties().setUri("http://localhost:" + mockPort + "/esdb_api/v1");
    }

    @Given("The ESDB VLAN task is ready")
    public void esdbVlanTaskIsReady() {
        assert esdbVlanSync != null;
        assert !esdbVlanSync.isSynchronized();
    }

    @When("The ESDB VLAN synchronization is triggered")
    public void theESDBVLANSynchronizationIsTriggered() {
        esdbVlanSync.processingLoop();
    }

    @Then("The ESDB VLAN was synchronized")
    public void theESDBVLANWasSynchronized() {
        assert esdbVlanSync.isSynchronized();
    }

}
