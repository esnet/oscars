package net.es.oscars.cuke;

import io.cucumber.java.en.Given;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.sb.nso.NsoLspStateSyncer;
import net.es.oscars.sb.nso.NsoProxy;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Category({UnitTests.class})
public class NsoLspStateSyncerSteps extends CucumberSteps {
    @Autowired
    private CucumberWorld world;

    @Autowired
    NsoProxy proxy;

    @Autowired
    NsoLspStateSyncer syncer;

    @Given("The NSO LSP service state is loaded")
    public void the_NSO_LSP_service_state_is_loaded() {
        // STUB
    }

    @Given("The NSO LSP service state has {int} instances")
    public void theNSOLSPServiceStateHasInstances(int arg0) {
        // STUB
    }

    @Given("I have retrieved the NSO LSPs")
    public void iHaveRetrievedTheNSOLSPs() {
        // Load the (mock) NSO response payload
        syncer = new NsoLspStateSyncer(proxy);

        // Make sure the file content you expect is mocked in NsoHttpServer!
        syncer.load();
    }
}
