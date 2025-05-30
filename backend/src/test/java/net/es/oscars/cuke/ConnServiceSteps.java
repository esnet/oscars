package net.es.oscars.cuke;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.model.Interval;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.enums.*;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.resv.svc.ResvService;
import net.es.oscars.topo.beans.PortBwVlan;
import net.es.oscars.web.beans.ConnException;
import net.es.oscars.web.simple.*;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    ResvService mockResvService;
    private Instant beginInstant;
    private Instant endInstant;

    @Before("@ConnServiceSteps")
    public void before() {
        this.validity = null;


        try {
            this.connService = new ConnService();
            // FIXME: Figure out why this isn't being automatically pulled in from testing.properties
            this.connService.setDefaultMtu(9000);
            this.connService.setMinMtu(1500);
            this.connService.setMaxMtu(9000);

            this.connService.setMinDuration(15);

            this.connService.setResvTimeout(900);

            this.inConn = createValidSimpleConnection();

            // Mock ResvService, and have ResvService.available() return a mock availableBwVlanMap list.
            mockResvService = Mockito.mock(ResvService.class);
            Map<String, Connection> held = new HashMap<>();
            // add mock held Connection entries.
            Connection mockConnection = Connection.builder()
                .username(      this.userName )
                .connectionId(  this.connectionId )
                .connection_mtu(this.connection_mtu )
                .serviceId(     this.serviceId )
                .tags(          new ArrayList<>() )
                .description(   this.description )
                .mode(          BuildMode.AUTOMATIC )
                .phase(         Phase.DESIGN )
                .username(      this.userName )
                .state(         State.WAITING )
                .deploymentState(DeploymentState.UNDEPLOYED)
                .deploymentIntent(DeploymentIntent.SHOULD_BE_DEPLOYED)
                .last_modified(this.beginTime)
                .build();
            held.put(mockConnection.getConnectionId(), mockConnection);
            Map<String, PortBwVlan> mockAvailBwVlanMap = new HashMap<>();
            // @TODO: provide a mock avialableBwVlanMap list

            Interval interval = Interval.builder()
                .beginning(this.beginInstant)
                .ending(this.endInstant)
                .build();

            Mockito.when(mockResvService.available(interval, held, connectionId)).thenReturn(mockAvailBwVlanMap);

            this.connService.setResvService( mockResvService );

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
    public void theScheduledBeginTimeIsSetTo() {
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
        if (!validity.isValid()) {
            log.error("The connection is not valid: {}", validity.getMessage());
        }
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
        
        // @TODO: Need a mock list of fixtures, junctions, and pipes
        this.connectionFixtures = new ArrayList<>();
        this.connectionJunctions = new ArrayList<>();
        this.connectionPipes = new ArrayList<>();

        this.createValidSchedule();

        int minMtuDefault = this.connService.getMinMtu();
        log.info("minMtuDefault: " + minMtuDefault);

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

        int duration = this.connService.getMinDuration(); // minDuration default is 15 min

        int intBegin = Long.valueOf(now.getEpochSecond() ).intValue();
        Instant iBegin = Instant.ofEpochSecond(intBegin);

        // Expected end time cannot be <= minDuration, which is 15min.
        // Set it to minDuration + 1.
        Instant iEnd = iBegin.plus(duration + 1, ChronoUnit.MINUTES);
        int intEnd = Long.valueOf(iEnd.getEpochSecond()).intValue();

        this.beginTime = intBegin;
        this.endTime = intEnd;
        this.beginInstant = iBegin;
        this.endInstant = iEnd;
    }
}
