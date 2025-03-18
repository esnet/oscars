package net.es.oscars.cuke;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import org.junit.experimental.categories.Category;

@Slf4j
@Category({UnitTests.class})
public class NsoStateSyncerSteps extends CucumberSteps {

    @Given("The NSO VPLS service state is loaded")
    public void theNSOVPLSServiceStateIsLoaded() throws Throwable {
        // STUB
    }

    @Given("The NSO VPLS service state has {int} instances")
    public void theNSOVPLSServiceStateHasInstances(int arg0) {
        // STUB
    }

    @Given("The list of active OSCARS connections are loaded from {string}")
    public void theListOfActiveOSCARSConnectionsAreLoadedFrom(String arg0) {
        // STUB
    }

    @Given("The VPLS instance {string} is present in the NSO VPLS service state")
    public void theVPLSInstanceIsPresentInTheNSOVPLSServiceState(String arg0) {
        // STUB
    }

    @Given("The VPLS instance {string} is not present in the NSO VPLS service state")
    public void theVPLSInstanceIsNotPresentInTheNSOVPLSServiceState(String arg0) {
        // STUB
    }


    @When("I evaluate VPLS {string}")
    public void iEvaluateVPLS(String arg0) {
        // STUB
    }

    @When("I add VPLS instance {string}")
    public void iAddVPLSInstance(String arg0) {
        // STUB
    }

    @When("I delete VPLS instance {string}")
    public void iDeleteVPLSInstance(String arg0) {
        // STUB
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
