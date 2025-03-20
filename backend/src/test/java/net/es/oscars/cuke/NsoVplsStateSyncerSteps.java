package net.es.oscars.cuke;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.sb.nso.NsoProxy;
import net.es.oscars.sb.nso.NsoStateSyncer;
import net.es.oscars.sb.nso.NsoVplsStateSyncer;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.URL;

@Slf4j
@Category({UnitTests.class})
public class NsoVplsStateSyncerSteps extends CucumberSteps {
    NsoVplsStateSyncer syncer;

    @Autowired
    NsoProxy proxy;

    @Given("The list of active OSCARS connections are loaded from {string}")
    public void theListOfActiveOSCARSConnectionsAreLoadedFrom(String arg0) throws Throwable {
        // STUB
//        syncer = new NsoVplsStateSyncer(proxy);
//        syncer.load(arg0);
    }

    @Given("The NSO VPLS service state is loaded")
    public void theNSOVPLSServiceStateIsLoaded() throws Throwable {
        // STUB
//        assert syncer != null;
//        assert syncer.isLoaded();
    }

    @Given("The NSO VPLS service state has {int} instances")
    public void theNSOVPLSServiceStateHasInstances(int arg0) throws Throwable {
//        assert syncer.getRemoteInstanceCount() == arg0;
    }



    @Given("The VPLS instance {string} is present in the NSO VPLS service state")
    public void theVPLSInstanceIsPresentInTheNSOVPLSServiceState(String arg0) throws Throwable {
//        assert syncer.getRemoteState().get(arg0) != null;
    }

    @Given("The VPLS instance {string} is not present in the NSO VPLS service state")
    public void theVPLSInstanceIsNotPresentInTheNSOVPLSServiceState(String arg0) throws Throwable {
        // STUB
    }


    /**
     * Evaluate one or more VPLS IDs
     * @param arg0 One or more comma-delimited VPLS IDs
     */
    @When("I evaluate VPLS {string}")
    public void iEvaluateVPLS(String arg0) {
        // STUB
    }

    /**
     * Mark one or more VPLS IDs for add
     * @param arg0 One or more comma-delimited VPLS IDs
     */
    @When("I add VPLS instance {string}")
    public void iAddVPLSInstance(String arg0) {
        // STUB
    }
    /**
     * Mark one or more VPLS IDs for delete
     * @param arg0 One or more comma-delimited VPLS IDs
     */
    @When("I delete VPLS instance {string}")
    public void iDeleteVPLSInstance(String arg0) {
        // STUB
    }

    /**
     * Mark one or more VPLS IDs for re-deploy
     * @param arg0 One or more comma-delimited VPLS IDs
     */
    @When("I redeploy VPLS instance {string}")
    public void iRedeployVPLSInstance(String arg0) {
        // STUB
    }

    /**
     * Mark one or more VPLS IDs as no-op
     * @param arg0 One or more comma-delimited VPLS IDs
     */
    @When("I no-op VPLS {string}")
    public void iNoOpVPLS(String arg0) {
    }

    @When("I apply VPLS service patch from {string}")
    public void iApplyVPLSServicePatchFrom(String arg0) {
        // STUB
    }

    @Then("VPLS {string} is marked as {string}")
    public void vplsIsMarkedAs(String arg0, String arg1) {
        // STUB
    }

    @Then("The list of VPLS service instances marked {string} equals {string}")
    public void theListOfVPLSServiceInstancesMarkedEquals(String arg0, String arg1) {
        // STUB
    }

    @Then("The NSO VPLS service state now has {int} instances")
    public void theNSOVPLSServiceStateNowHasInstances(int arg0) {
        // STUB
    }


    @Then("The NSO VPLS service is synchronized")
    public void theNSOVPLSServiceIsSynchronized() {
        // STUB
    }

    @Then("The VPLS instance {string} matches {string}")
    public void theVPLSInstanceMatches(String arg0, String arg1) {
        // STUB
    }

    @Then("The list of VPLS service instances equals {string}")
    public void theListOfVPLSServiceInstancesEquals(String arg0) {
        // STUB

    }
}
