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
import net.es.topo.common.model.oscars1.IntRange;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;

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
    Map<String, Connection> held = new HashMap<>();
    private Instant beginInstant;
    private Instant endInstant;

    @Before("@ConnServiceSteps")
    public void before() {
        this.validity = null;
        createTestConnection();
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
        // Set to at least one minute over the minium time required
        this.createValidSchedule();
        this.inConn.setBegin(this.beginTime);
        this.inConn.setEnd(this.endTime);
    }
    @Given("The schedule is set to an invalid time")
    public void theScheduleIsSetToAnInvalidTime() {
        // Set to the minimum time required, minus one minute
        this.createInvalidSchedule();
        this.inConn.setBegin(this.beginTime);
        this.inConn.setEnd(this.endTime);
    }

    @Given("The connection attempts to reserve {int} Mbps in, {int} Mbps out, {int} Mbps from a to z, {int} Mbps from z to a, {int} Mbps set")
    public void theConnectionAttemptsToReserveMbps(int mbpsIn, int mbpsOut, int azMbps, int zaMbps, int mbps) throws Exception {
        this.inConn = createSimpleConnection(mbpsIn, mbpsOut, azMbps, zaMbps, mbps);
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
    @Then("The connection is not valid")
    public void theConnectionIsNotValid() {
        if (validity.isValid()) {
            log.error("The connection was expected to be invalid.");
            assert validity != null;
            assert !validity.isValid();
        }
    }


    private SimpleConnection createValidSimpleConnection() throws Exception {
        return  this.createSimpleConnection(
            1000,
            1000,
            1000,
            1000,
            1000
        );
    }
    private SimpleConnection createSimpleConnection(int inMbps, int outMpbs, int azMbps, int zaMbps, int mbps) throws Exception {
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

        // Fixtures come in pairs
        this.connectionFixtures.add(
            Fixture.builder()
                .port("ornl5600-cr6:1/1/c31/1")
                .inMbps(inMbps)
                .outMbps(outMpbs)
                .vlan(5)
                .junction("ornl5600-cr6") // Name of the router
                .mbps(mbps)
                .build()
        );
        this.connectionFixtures.add(
            Fixture.builder()
                .port("star-cr6:1/1/c55/1")
                .inMbps(inMbps)
                .outMbps(outMpbs)
                .vlan(5)
                .junction("star-cr6") // Name of the router
                .mbps(mbps)
                .build()
        );

        // Junctions pair one or more fixtures together.
        // Junctions represent a single router.
        //
        // J <-------> F
        //
        // J <-|
        //     |--> F
        //     |--> F
        //
        // J <-|
        //     |--> F
        //     |--> F
        //     |--> F
        this.connectionJunctions = new ArrayList<>();
        this.connectionJunctions.add(
            Junction.builder()
                .device("ornl5600-cr6")
                .build()
        );
        this.connectionJunctions.add(
            Junction.builder()
                .device("star-cr6")
                .build()
        );

        // Pipes connection junctions together.
        // Pipes represent one or more LSPs.
        // Pipes represent the primary LSP. Implemented by at least two (2) directional LSPs (a to z).
        //
        //      (Pipe)
        // J <---E - B - E-----> J
        // |                     |
        // F                     F
        //
        // Pipes have a list of EROs
        // An ERO example. Pattern: Router-port-port-Router. Pattern repeats from A to Z.
        //  ornl5600-cr6          // Start with A itself
        //  ornl5600-cr6:1/2/c1/1 //
        //  denv-cr6:2/1/c3/2     // A to M
        //  denv-cr6              // Intermediate router M
        //  denv-cr6:2/1/c4/2     // M to Z
        //  star-cr6:1/1/2/1      //
        //  star-cr6              // end with router Z
        //
        this.connectionPipes = new ArrayList<>();

        // ...Build Pipe EROs BEGIN
        List<String> eros = new ArrayList<>();



        // ...  Starts from a
        eros.add("ornl5600-cr6");
        // ...  Add backbone links here.
        eros.add("ornl5600-cr6:1/2/c1/1");
        eros.add("denv-cr6:2/1/c3/2"); // Port on intermediate router
        eros.add("denv-cr6");          // Intermediate router
        eros.add("denv-cr6:2/1/c4/2"); // Other port on intermediate router
        eros.add("star-cr6:1/1/2/1");  // Different port(s) than those used for fixtures
        // ...  Ends at z
        eros.add("star-cr6"); // Router
        // ... Build Pipe EROs END

        this.connectionPipes.add(
            Pipe.builder()
                .a("ornl5600-cr6") // Router at a
                .z("star-cr6") // Router at z
                .mbps(mbps)
                .azMbps(azMbps)
                .zaMbps(zaMbps)
                .ero(eros)
                .build()
        );

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
            .last_modified( this.beginTime )
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

    private void createInvalidSchedule() {
        Instant now = Instant.now();

        int duration = this.connService.getMinDuration(); // minDuration default is 15 min

        int intBegin = Long.valueOf(now.getEpochSecond() ).intValue();
        Instant iBegin = Instant.ofEpochSecond(intBegin);

        // Expected end time cannot be <= minDuration, which is 15min.
        // Set it to minDuration - 1 to create an interval we would consider invalid.
        Instant iEnd = iBegin.plus(duration - 1, ChronoUnit.MINUTES);
        int intEnd = Long.valueOf(iEnd.getEpochSecond()).intValue();

        this.beginTime = intBegin;
        this.endTime = intEnd;
        this.beginInstant = iBegin;
        this.endInstant = iEnd;
    }

    private void createTestConnection() {
        this.createTestConnection(1000, 1000);
    }
    private void createTestConnection(int ingressBandwidth, int egressBandwidth) {
        try {
            this.connService = new ConnService();
            // @FIXME: Figure out why this isn't being automatically pulled in from testing.properties
            this.connService.setDefaultMtu(9000);
            this.connService.setMinMtu(1500);
            this.connService.setMaxMtu(9000);

            this.connService.setMinDuration(15);
            this.connService.setResvTimeout(900);

            this.inConn = createValidSimpleConnection();

            // Mock ResvService, and have ResvService.available() return a mock availableBwVlanMap list.
            mockResvService = Mockito.mock(ResvService.class);

            // Mock held connections.
            // @TODO: Must be length zero or more for validation to pass?

            // ...add mock held Connection entries.
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
                .deploymentIntent(DeploymentIntent.SHOULD_BE_UNDEPLOYED)
                .last_modified(this.beginTime)
                .build();
            this.held.put(mockConnection.getConnectionId(), mockConnection);

            // Build available BwVlanMap BEGIN
            Map<String, PortBwVlan> mockAvailBwVlanMap = new HashMap<>();
            // ... Provide a mock availableBwVlanMap list (length of zero or more)
            // ... BwVlanMap includes fixtures and eros, but are not order dependent!

            Set<IntRange> vlanRanges = new HashSet<>();
            vlanRanges.add(
                IntRange.builder()
                    .floor(2)
                    .ceiling(100)
                    .build()
            );

            // Fixture port: ornl5600-cr6:1/1/c31/1
            // Fixture port: star-cr6:1/1/c55/1
            //
            // EROs
            //  ornl5600-cr6          // Start with A itself
            //  ornl5600-cr6:1/2/c1/1 // (Needs BwVlanMap)
            //  denv-cr6:2/1/c3/2     // A to M (Needs BwVlanMap)
            //  denv-cr6              // Intermediate router M
            //  denv-cr6:2/1/c4/2     // M to Z (Needs BwVlanMap)
            //  star-cr6:1/1/2/1      // (Needs BwVlanMap)
            //  star-cr6              // end with router Z

            String[] portsToBwVlanMap = new String[] {
                "ornl5600-cr6:1/2/c1/1",
                "denv-cr6:2/1/c3/2",
                "denv-cr6:2/1/c4/2",
                "star-cr6:1/1/2/1"
            };
            // ... Fixture port on Router A
            mockAvailBwVlanMap.put(
                "ornl5600-cr6:1/1/c31/1",
                PortBwVlan.builder()
                    .egressBandwidth(egressBandwidth)
                    .ingressBandwidth(ingressBandwidth)
                    // These are EROs, and don't care about VLAN stuff
                    .vlanExpression("2:100") // we are asking for VLAN 5. Available VLANs are from 2 to 100, inclusive.
                    .vlanRanges( vlanRanges ) // Floor: 2, Ceiling: 100
                    .build()
            );
            for (String portToBwVlanMap : portsToBwVlanMap) {
                mockAvailBwVlanMap.put(
                    portToBwVlanMap,
                    PortBwVlan.builder()
                        .egressBandwidth(egressBandwidth)
                        .ingressBandwidth(ingressBandwidth)
                        // These are EROs, and don't care about VLAN stuff
                        .vlanExpression("") // we are asking for VLAN 5. Available VLANs are from 2 to 100, inclusive.
                        .vlanRanges( new HashSet<>() ) // Floor: 2, Ceiling: 100
                        .build()
                );
            }
            // ... Fixture port on Router Z
            mockAvailBwVlanMap.put(
                "star-cr6:1/1/c55/1",
                PortBwVlan.builder()
                    .egressBandwidth(egressBandwidth)
                    .ingressBandwidth(ingressBandwidth)
                    // These are EROs, and don't care about VLAN stuff
                    .vlanExpression("2:100") // we are asking for VLAN 5. Available VLANs are from 2 to 100, inclusive.
                    .vlanRanges( vlanRanges ) // Floor: 2, Ceiling: 100
                    .build()
            );
            // Build available BwVlanMap END

            Interval interval = Interval.builder()
                .beginning(this.beginInstant)
                .ending(this.endInstant)
                .build();


            Mockito
                .when(
                    mockResvService.available(
                        any(Interval.class),
                        any(Map.class),
                        any(String.class)
                    )
                )
                .thenReturn(mockAvailBwVlanMap);
            // Set the held list, too!
            this.connService.setHeld(held);
            this.connService.setResvService( mockResvService );

        } catch (Exception e) {
            world.add(e);
            log.error("ConnServiceSteps.before() encountered an exception. Exception: {}", e.getLocalizedMessage());
        }
    }
}
