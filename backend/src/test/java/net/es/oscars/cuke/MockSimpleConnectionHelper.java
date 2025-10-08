package net.es.oscars.cuke;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.model.Interval;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.*;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.resv.svc.ResvService;
import net.es.oscars.topo.beans.PortBwVlan;
import net.es.oscars.web.simple.*;
import net.es.topo.common.model.oscars1.IntRange;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;

@Slf4j
@Component
@Data
public class MockSimpleConnectionHelper {

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

    ResvService mockResvService;
    Map<String, Connection> held = new HashMap<>();
    private Instant beginInstant;
    private Instant endInstant;

    private String projectId = null;

    public SimpleConnection createValidSimpleConnection() throws Exception {
        return  this.createSimpleConnection(
            1000,
            1000,
            1000,
            1000,
            1000
        );
    }

    public List<Fixture> createFixtures(int inMbps, int outMpbs, int mbps) throws Exception {
        List<Fixture> connectionFixtures = new ArrayList<>();

        // Fixtures come in pairs
        connectionFixtures.add(
            Fixture.builder()
                .port("ornl5600-cr6:1/1/c31/1")
                .inMbps(inMbps)
                .outMbps(outMpbs)
                .vlan(5)
                .junction("ornl5600-cr6") // Name of the router
                .mbps(mbps)
                .build()
        );
        connectionFixtures.add(
            Fixture.builder()
                .port("star-cr6:1/1/c55/1")
                .inMbps(inMbps)
                .outMbps(outMpbs)
                .vlan(5)
                .junction("star-cr6") // Name of the router
                .mbps(mbps)
                .build()
        );

        return connectionFixtures;
    }
    List<Junction> createJunctions() throws Exception {
        List<Junction> connectionJunctions = new ArrayList<>();
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
        connectionJunctions = new ArrayList<>();
        connectionJunctions.add(
            Junction.builder()
                .device("ornl5600-cr6")
                .build()
        );
        connectionJunctions.add(
            Junction.builder()
                .device("star-cr6")
                .build()
        );

        return connectionJunctions;
    }

    public List<Pipe> createPipes(int azMbps, int zaMbps, int mbps, List<String> eros) throws Exception {
        List<Pipe> connectionPipes = new ArrayList<>();
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

        connectionPipes.add(
            Pipe.builder()
                .a("ornl5600-cr6") // Router at a
                .z("star-cr6") // Router at z
                .mbps(mbps)
                .azMbps(azMbps)
                .zaMbps(zaMbps)
                .ero(eros)
                .build()
        );

        return connectionPipes;
    }

    public SimpleConnection createSimpleConnection(int inMbps, int outMpbs, int azMbps, int zaMbps, int mbps) throws Exception {
        return createSimpleConnection("ABCD", inMbps, outMpbs, azMbps, zaMbps, mbps);
    }
    public SimpleConnection createSimpleConnection(String connectionId, int inMbps, int outMbps, int azMbps, int zaMbps, int mbps) throws Exception {
        return createSimpleConnection(connectionId, inMbps, outMbps, azMbps, zaMbps, mbps, null);
    }
    public SimpleConnection createSimpleConnection(String connectionId, int inMbps, int outMpbs, int azMbps, int zaMbps, int mbps, String projectId) throws Exception {
        // Create a valid SimpleConnection object
        this.connectionId = connectionId;
        this.connection_mtu = 9000;
        this.serviceId = "testservice";
        this.simpleTags = new ArrayList<>();
        this.description = "test description";
        this.buildMode = BuildMode.AUTOMATIC;
        this.phase = Phase.DESIGN;
        this.userName = "testuser";
        this.state = State.WAITING;
        this.projectId = projectId;

        // @TODO: Need a mock list of fixtures, junctions, and pipes
        this.connectionFixtures = createFixtures(inMbps, outMpbs, mbps);


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
        this.connectionJunctions = createJunctions();

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

        // Will need EROs to create pipes
        // ...Build Pipe EROs BEGIN
        List<String> eros = createEro();
        // ... Build Pipe EROs END
        this.connectionPipes = createPipes(azMbps, zaMbps, mbps, eros);


        this.createValidSchedule();

        int minMtuDefault = this.connService.getMinMtu();
        log.info("minMtuDefault: " + minMtuDefault);
        Set<String> projectIds = new HashSet<>();
        projectIds.add(projectId);

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
            .validity( this.createTrueValidity() )
            .projectIds(projectIds)
            .build();
    }

    public Validity createTrueValidity() {
        return Validity.builder()
            .valid(true)
            .message("valid test message")
            .build();
    }

    public Validity createFalseValidity() {
        return Validity.builder()
            .valid(false)
            .message("invalid test message")
            .build();
    }

    public List<String> createEro() {
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

        return eros;
    }

    public Schedule createValidSchedule() {
        this.createValidSchedule(this.connService.getMinDuration());
        return Schedule.builder()
            .id(1L)
            .connectionId(this.connectionId)
            .beginning(this.beginInstant)
            .ending(this.endInstant)
            .phase(this.phase)
            .refId("abc123-cr6")
            .build();
    }
    public void createValidSchedule(int durationMinutes) {
        Instant now = Instant.now();

        int intBegin = Long.valueOf(now.getEpochSecond() ).intValue();
        Instant iBegin = Instant.ofEpochSecond(intBegin);

        // Expected end time cannot be <= minDuration, which is 15min.
        // CHANGED Behavior: Set it to minDuration + 1, instead of minDuration.
        Instant iEnd = iBegin.plus(durationMinutes + 1, ChronoUnit.MINUTES);
        int intEnd = Long.valueOf(iEnd.getEpochSecond()).intValue();

        this.beginTime = intBegin;
        this.endTime = intEnd;
        this.beginInstant = iBegin;
        this.endInstant = iEnd;
    }

    public void createInvalidSchedule() {
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

    public void createTestConnection() throws Exception {
        this.createTestConnection(1000, 1000);
    }
    public void createTestConnection(int ingressBandwidth, int egressBandwidth) throws Exception {

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
        Set<String> projectIds = new HashSet<>();
        projectIds.add(this.projectId);

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
            .projectIds(projectIds)
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
    }

    public Connection generateMockConnection() {
        return generateMockConnection(
            "ABCD",
            Phase.HELD,
            BuildMode.AUTOMATIC,
            State.WAITING,
            DeploymentState.UNDEPLOYED,
            DeploymentIntent.SHOULD_BE_DEPLOYED,
            10000
        );
    }
    public Connection generateMockConnection(String connectionId) {
        return generateMockConnection(
            connectionId,
            Phase.HELD,
            BuildMode.AUTOMATIC,
            State.WAITING,
            DeploymentState.UNDEPLOYED,
            DeploymentIntent.SHOULD_BE_DEPLOYED,
            10000
        );
    }

    public Connection generateMockConnection(String mockConnectionId, Phase phase, BuildMode buildMode, State state, DeploymentState deploymentState, DeploymentIntent deploymentIntent, int connection_mtu) {
        return generateMockConnection(
            mockConnectionId,
            phase,
            buildMode,
            state,
            deploymentState,
            deploymentIntent,
            connection_mtu,
            null
        );
    }
    public Connection generateMockConnection(String mockConnectionId, Phase phase, BuildMode buildMode, State state, DeploymentState deploymentState, DeploymentIntent deploymentIntent, int connection_mtu, String projectId) {
        Set<String> projectIds = new HashSet<>();
        projectIds.add(projectId);

        return Connection.builder()
            .id(1L)
            .held( // Required, or /protected/modify/description result will become an HTTP 500 Internal Server Error
                Held.builder()
                    .connectionId(mockConnectionId)
                    .cmp(
                        Components.builder()
                            .id(1L)
                            .fixtures(new ArrayList<>())
                            .junctions(new ArrayList<>())
                            .pipes(new ArrayList<>())
                            .build()
                    )
                    .expiration(Instant.now().plusSeconds(60 * 20)) // 20 minutes from now
                    .schedule(createValidSchedule())
                    .build()
            )
            .connectionId(mockConnectionId)
            .phase(phase)
            .mode(buildMode)
            .state(state)
            .deploymentState(deploymentState)
            .deploymentIntent(deploymentIntent)
            .username("test")
            .description("test description")
            .connection_mtu(connection_mtu)
            .last_modified( ((Long) Instant.now().getEpochSecond()).intValue() )
            .projectIds(projectIds)
            .build();
    }
}
