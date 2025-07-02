package net.es.oscars.cuke;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.resv.enums.*;
import net.es.oscars.web.beans.ConnException;
import net.es.oscars.web.simple.*;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;


@Slf4j
@Category({UnitTests.class})
public class ConnServiceSteps extends CucumberSteps {

    @Autowired
    private CucumberWorld world;

    @Autowired
    private MockSimpleConnectionHelper helper;

    private Validity validity;

    @Before("@ConnServiceSteps")
    public void before() throws Exception {
        this.validity = null;
        helper.createTestConnection();
    }
    @Given("The connection ID is set to {string} and the connection mode is set to {string}")
    public void theConnectionIDIsSetToAndTheConnectionModeIsSetTo(String connectionId, String connMode) {
        helper.setConnectionId( connectionId );
        helper.setConnectionMode( ConnectionMode.valueOf(connMode.toUpperCase()) );
        helper.getInConn().setConnectionId(connectionId);
    }
    @Given("The connection ID is set to {string}")
    public void theConnectionIdIsSetTo(String connectionId) {
        helper.setConnectionId(connectionId);
        helper.getInConn().setConnectionId(connectionId);
    }

    @Given("The build mode is set to {string}")
    public void theModeIsSetTo(String mode) {
        helper.setBuildMode( BuildMode.valueOf(mode.toUpperCase()) );
        helper.getInConn().setMode(helper.getBuildMode());
    }

    @When("The MTU is set to {int}")
    public void theMTUIsSetTo(int mtu) {
        helper.setConnection_mtu(mtu);
        helper.getInConn().setConnection_mtu(mtu);
    }

    @Given("The description is set to {string}")
    public void theDescriptionIsSetTo(String description) {
        helper.setDescription(description);
        helper.getInConn().setDescription(description);
    }

    @Given("The description is set to null")
    public void theDescriptionIsSetToNull() {
        helper.setDescription( null );
        helper.getInConn().setDescription(helper.getDescription());
    }

    @Given("The schedule is set to a valid time")
    public void theScheduleIsSetToAValidTime() {
        // Set to at least one minute over the minium time required
        helper.createValidSchedule();
        helper.getInConn().setBegin(helper.getBeginTime());
        helper.getInConn().setEnd(helper.getEndTime());
    }
    @Given("The schedule is set to an invalid time")
    public void theScheduleIsSetToAnInvalidTime() {
        // Set to the minimum time required, minus one minute
        helper.createInvalidSchedule();
        helper.getInConn().setBegin(helper.getBeginTime());
        helper.getInConn().setEnd(helper.getEndTime());
    }

    @Given("The schedule is set to a valid time with a {long} year interval")
    public void theScheduleIsSetToAValidTimeWithAYearInterval(long durationYears) {
        // Set to at least one minute over the minium time required
        int durationMinutes = (int) (durationYears * 525600); // Years * Minutes per year
        helper.createValidSchedule(durationMinutes);
        helper.getInConn().setBegin(helper.getBeginTime());
        helper.getInConn().setEnd(helper.getEndTime());
    }

    @Given("The connection attempts to reserve {int} Mbps in, {int} Mbps out, {int} Mbps from a to z, {int} Mbps from z to a, {int} Mbps set")
    public void theConnectionAttemptsToReserveMbps(int mbpsIn, int mbpsOut, int azMbps, int zaMbps, int mbps) throws Exception {
        helper.setInConn(helper.createSimpleConnection(mbpsIn, mbpsOut, azMbps, zaMbps, mbps));
    }

    @When("The connection is validated")
    public void theConnectionIsValidated() {
        try {
            this.validity = helper
                .getConnService()
                .validate(
                    helper.getInConn(),
                    helper.getConnectionMode()
                );

            if (!this.validity.isValid()) {
               log.error(this.validity.getMessage());
            }
        } catch (ConnException e) {
            world.add(e);
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
    @Then("The connection is valid")
    public void theConnectionIsValidTrue() {
        if (!validity.isValid()) {
            log.error("The connection is not valid: {}", validity.getMessage());
        }
        assert validity != null;
        assert validity.isValid();
    }
    @Then("The connection is not valid")
    public void theConnectionIsNotValid() {
        if (validity.isValid()) {
            log.error("The connection was expected to be invalid.");
            assert validity != null;
            assert !validity.isValid();
        }
    }

}
