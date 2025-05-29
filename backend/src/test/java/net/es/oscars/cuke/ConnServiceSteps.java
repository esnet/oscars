package net.es.oscars.cuke;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.resv.enums.BuildMode;
import net.es.oscars.resv.enums.ConnectionMode;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.enums.State;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.web.beans.ConnException;
import net.es.oscars.web.simple.*;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Category({UnitTests.class})
public class ConnServiceSteps extends CucumberSteps {

    @Autowired
    private CucumberWorld world;

    @Autowired
    private ConnService connService;

    private SimpleConnection inConn;
    private ConnectionMode connectionMode;

    private String userName;
    private int beginTime;
    private int endTime;
    private String connectionId;
    private String serviceId;
    private List<SimpleTag> simpleTags;
    private String description;
    private BuildMode buildMode;
    private Phase phase = Phase.DESIGN;

    private State state;
    private List<Fixture> connectionFixtures;
    private List<Junction> connectionJunctions;
    private List<Pipe> connectionPipes;
    private int connection_mtu;
    private Validity validity;

    @Before("@ConnServiceSteps")
    public void before() {
        this.validity = null;
        this.connService = new ConnService();
        try {
            this.inConn = createValidSimpleConnection();
        } catch (Exception e) {
            world.add(e);
            log.error("ConnServiceSteps.before() encountered an exception. Exception: {}", e.getLocalizedMessage());
        }
    }
    @Given("The connection ID is set to {string} and the connection mode is set to {string}")
    public void theConnectionIDIsSetToAndTheConnectionModeIsSetTo(String connectionId, String connMode) {
        this.connectionId = connectionId;
        this.connectionMode = ConnectionMode.valueOf(connMode.toUpperCase());
        this.inConn.setConnectionId(connectionId);
    }
    @Given("The connection ID is set to {string}")
    public void theConnectionIdIsSetTo(String connectionId) {
        this.connectionId = connectionId;
        this.inConn.setConnectionId(connectionId);
    }

    @Given("The build mode is set to {string}")
    public void theModeIsSetTo(String mode) {
        this.buildMode = BuildMode.valueOf(mode.toUpperCase());
        this.inConn.setMode(this.buildMode);
    }

    @When("The MTU is set to {int}")
    public void theMTUIsSetTo(int mtu) {
        this.connection_mtu = mtu;
        this.inConn.setConnection_mtu(mtu);
    }

    @Given("The description is set to {string}")
    public void theDescriptionIsSetTo(String description) {
        this.description = description;
        this.inConn.setDescription(description);
    }

    @Given("The schedule is set to a valid time")
    public void theScheduledBeginTimeIsSetTo(String arg0) {
        this.createValidSchedule();
        this.inConn.setBegin(this.beginTime);
        this.inConn.setEnd(this.endTime);
    }

    @When("The connection is validated")
    public void theConnectionIsValidated() {
        try {
            this.validity = this
                .connService
                .validate(
                    this.inConn,
                    this.connectionMode
                );
        } catch (ConnException e) {
            throw new RuntimeException(e);
        }
    }
    @Then("The connection is valid")
    public void theConnectionIsValidTrue() {
        assert validity != null;
        assert validity.isValid();
    }


    private SimpleConnection createValidSimpleConnection() throws Exception {
        // Create a valid SimpleConnection object
        this.connectionId = "ABCD";
        this.connection_mtu = 9000;
        this.serviceId = "testservice";
        this.simpleTags = new ArrayList<>();
        this.description = "test description";
        this.buildMode = BuildMode.AUTOMATIC;
        this.phase = Phase.DESIGN;
        this.userName = "testuser";
        this.state = State.WAITING;
        this.connectionFixtures = new ArrayList<>();
        this.connectionJunctions = new ArrayList<>();
        this.connectionPipes = new ArrayList<>();

        this.createValidSchedule();

        return SimpleConnection.builder()
            .username(      this.userName )
            .begin(         this.beginTime )
            .end(           this.endTime )
            .connectionId(  this.connectionId )
            .connection_mtu(this.connection_mtu )
            .serviceId(     this.serviceId )
            .tags(          this.simpleTags )
            .description(   this.description )
            .mode(          this.buildMode )
            .phase(         this.phase )
            .username(      this.userName )
            .state(         this.state )
            .fixtures(      this.connectionFixtures)
            .junctions(     this.connectionJunctions )
            .pipes(         this.connectionPipes)
            .build();
    }

    private void createValidSchedule() {
        Instant now = Instant.now();

        int duration = this.connService.getMinDuration() * 60;
        int intBegin = Long.valueOf(now.getEpochSecond()).intValue();
        int intEnd = intBegin + duration;

        this.beginTime = intBegin;
        this.endTime = intEnd;
    }
}
