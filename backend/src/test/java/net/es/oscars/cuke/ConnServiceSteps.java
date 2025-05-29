package net.es.oscars.cuke;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.resv.enums.ConnectionMode;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.web.simple.SimpleConnection;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Category({UnitTests.class})
public class ConnServiceSteps extends CucumberSteps {

    @Autowired
    private CucumberWorld world;

    @Autowired
    private ConnService connService;

    private SimpleConnection inConn;
    private ConnectionMode connectionMode;

    private String defaultConnectionId = "";

    @Before("@ConnServiceStep")
    public void before() {
        this.connService = new ConnService();
        this.inConn = new SimpleConnection();
    }
    @Given("The connection ID is set to {string} and the connection mode is set to {string}")
    public void theConnectionIDIsSetToAndTheConnectionModeIsSetTo(String connectionId, String connMode) {
        this.inConn.setConnectionId(connectionId);
        this.connectionMode = ConnectionMode.valueOf(connMode);
    }
    @Given("The connection mode is set to {string}")
    public void theConnectionModeIsSetTo(String connectionMode) {
        this.connectionMode = ConnectionMode.valueOf(connectionMode);
    }

    @Given("The global connection was set to valid parameters")
    public void theGlobalConnectionWasSetToValidParameters() {
        try {
            this.inConn.setConnectionId(this.defaultConnectionId);
            this.inConn.setConnection_mtu(this.connService.getDefaultMtu());

        } catch (Exception e) {
            world.add(e);
        }
    }

    @Given("The scheduled begin time and end time make a valid interval")
    public void theScheduledBeginTimeAndEndTimeMakeAValidInterval() {
        
    }

    @When("The connection ID is set to {string}")
    public void theConnectionIDIsSetTo(String arg0) {
        
    }

    @When("The connection ID is validated")
    public void theConnectionIDIsValidated() {
        
    }

    @When("The MTU is set to {string}")
    public void theMTUIsSetTo(String arg0) {
        
    }

    @When("The MTU is validated")
    public void theMTUIsValidated() {
        
    }

    @When("The description is set to {string}")
    public void theDescriptionIsSetTo(String arg0) {
        
    }

    @When("The description is validated")
    public void theDescriptionIsValidated() {
        
    }

    @When("The scheduled begin time is set to {string}")
    public void theScheduledBeginTimeIsSetTo(String arg0) {
        
    }

    @When("The scheduled begin time is validated")
    public void theScheduledBeginTimeIsValidated() {
        
    }

    @When("The scheduled end time is set to {string}")
    public void theScheduledEndTimeIsSetTo(String arg0) {
        
    }

    @When("The scheduled end time is validated")
    public void theScheduledEndTimeIsValidated() {
        
    }

    @When("The maps are populated with what we request through fixtures")
    public void theMapsArePopulatedWithWhatWeRequestThroughFixtures() {
        
    }

    @When("The maps are populated with what we request through pipes \\(BW only)")
    public void theMapsArePolulatedWithWhatWeRequestThroughPipesBWOnly() {
        
    }

    @When("The VLAN maps are compared to what is available")
    public void theVLANMapsAreComparedToWhatIsAvailable() {
        
    }

    @When("The map is compared to what is available for BW")
    public void theMapIsComparedToWhatIsAvailableForBW() {
        
    }

    @When("The Validity for fixtures is populated")
    public void theValidityForFixturesIsPopulated() {
        
    }

    @When("The Validity for pipes and EROs is populated")
    public void theValidityForPipesAndEROsIsPopulated() {
    }

    @Then("The connection ID is valid")
    public void theConnectionIDIsValid() {

    }

    @Then("The MTU is valid")
    public void theMTUIsValid() {

    }

    @Then("The description is valid")
    public void theDescriptionIsValid() {

    }

    @Then("The global connection params are valid")
    public void theGlobalConnectionParamsAreValid() {

    }

    @Then("The scheduled begin time is valid")
    public void theScheduledBeginTimeIsValid() {
        
    }

    @Then("The scheduled end time is valid")
    public void theScheduledEndTimeIsValid() {
        
    }

    @Then("The fixtures are valid")
    public void theFixturesAreValid() {
        
    }

    @Then("The connection is valid \\(true)")
    public void theConnectionIsValidTrue() {
    }
}
