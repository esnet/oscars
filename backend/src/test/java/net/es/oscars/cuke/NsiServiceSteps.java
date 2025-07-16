package net.es.oscars.cuke;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.requester.ConnectionRequesterPort;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.*;
import net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType;
import net.es.nsi.lib.soap.gen.nsi_2_0.services.point2point.P2PServiceBaseType;
import net.es.nsi.lib.soap.gen.nsi_2_0.services.types.DirectionalityType;
import net.es.nsi.lib.soap.gen.nsi_2_0.services.types.OrderedStpType;
import net.es.nsi.lib.soap.gen.nsi_2_0.services.types.StpListType;
import net.es.oscars.BackendTestConfiguration;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.model.Interval;
import net.es.oscars.nsi.ent.NsiMapping;
import net.es.oscars.nsi.ent.NsiRequesterNSA;
import net.es.oscars.nsi.svc.*;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.*;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.soap.NsiSoapClientUtil;
import net.es.oscars.web.simple.Fixture;
import net.es.oscars.web.simple.Junction;
import net.es.oscars.web.simple.SimpleConnection;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Test steps for NsiService class.
 *  - See draft-nsi-cs-protocol-2dot1-v13.pdf, page 12.
 */
@Slf4j
@Category({UnitTests.class})
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        BackendTestConfiguration.class,
        ConnectionRepository.class,
        ConnService.class,
        ConnectionRequesterPort.class,
        NsiRequestManager.class,
        NsiHeaderUtils.class,
        NsiSoapClientUtil.class,
        NsiQueries.class,
        NsiNotifications.class,
        NsiConnectionEventService.class,
        NsiService.class,
    }
)
public class NsiServiceSteps extends CucumberSteps {
    @Autowired
    private CucumberWorld world;

    // Mock the NsiService constructor arguments
    @MockitoBean
    @Autowired
    private NsiRequestManager mockNsiRequestManager;

    @MockitoBean
    @Autowired
    private NsiHeaderUtils mockNsiHeaderUtils;

    @MockitoBean
    @Autowired
    private NsiStateEngine mockNsiStateEngine;

    @MockitoBean
    private NsiMappingService mockNsiMappingService;

    @MockitoBean
    private NsiSoapClientUtil mockNsiSoapClientUtil;

    @Mock
    ConnectionRequesterPort mockConnectionRequesterPort;

    CommonHeaderType mockCommonHeaderType;
    NsiMapping mockNsiMapping;

    @Mock
    @Autowired
    private NsiQueries mockNsiQueries;
    @Mock
    @Autowired
    private NsiNotifications mockNsiNotifications;

    @MockitoBean
    @Autowired
    private NsiConnectionEventService mockNsiConnectionEventService;

    @Autowired
    private MockSimpleConnectionHelper helper;

    @MockitoBean
    private ConnectionRepository connRepo;
    @MockitoBean
    private ConnService connSvc;

    @MockitoBean
    @Autowired
    private NsiService nsiService;

    // NSA identifier. Identifies the customer (whoever is calling). Customers such as SENSE, etc
    // "localstackv"
    String nsaId = "localstackv";

    String globalReservationId = "GLOBALID";
    String nsiConnectionId = "RES1";
    String oscarsConnectionId = "ABCD";

    int inMbps = 10000;
    int outMbps = 10000;
    int azMbps = 10000;
    int zaMbps = 10000;
    int mbps = 10000;

    boolean reserved = false;
    boolean provisioned = false;
    boolean released = false;

    @Autowired
    private NsiMappingService nsiMappingService;

    @Before("@NsiServiceSteps")
    public void before() throws Throwable {
        setupDatasources();
        setupMockCommonHeaderType();
        setupMockNsiMapping();
        setupMockNsiService();
    }

    private void setupMocks(Connection mockConn) throws Exception {
        reserved = false;
        Instant now = Instant.now();
        Instant endingTime = now.plusSeconds(20 * 60); // 20 minutes

        Schedule mockSchedule = Schedule.builder()
            .connectionId(oscarsConnectionId)
            .beginning(now)
            .ending(endingTime) // 20 min
            .refId(globalReservationId)
            .build();

        // Attempt to call nsiService.reserve() for oscarsConnectionId

        ScheduleType mockScheduleType = nsiMappingService.oscarsToNsiSchedule(mockSchedule);

        mockScheduleType.setStartTime(mockScheduleType.getStartTime());
        mockScheduleType.setEndTime(mockScheduleType.getEndTime());

        ReserveType mockReserveType = new ReserveType();
        ReservationRequestCriteriaType mockCriteriaType = new ReservationRequestCriteriaType();

        mockCriteriaType.setSchedule(mockScheduleType);
        mockCriteriaType.setServiceType("");
        mockCriteriaType.setVersion(1);

        mockReserveType.setConnectionId(oscarsConnectionId);
        mockReserveType.setGlobalReservationId(globalReservationId);
        mockReserveType.setDescription("Test reservation");
        mockReserveType.setCriteria(mockCriteriaType);

        ReservationRequestCriteriaType mockReservationRequestCriteriaType = Mockito.mock(ReservationRequestCriteriaType.class);

        mockNsiMappingService = Mockito.mock(NsiMappingService.class);

        Mockito.when(mockReservationRequestCriteriaType.getVersion()).thenReturn(1);
        Mockito.when(mockReservationRequestCriteriaType.getSchedule()).thenReturn(mockScheduleType);


        // Set the mock data for NsiService.reserve()
        // An NsiMapping object is used
        Mockito.doReturn(
            mockNsiMapping
        ).when(mockNsiMappingService).newMapping(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyInt(),
            Mockito.anyBoolean()
        );

        // Mock NsiMappingService.nsiToOscarsSchedule()
        Interval mockInterval = Interval.builder()
            .beginning(now)
            .ending(endingTime) // 20 minutes from now
            .build();
        Mockito.doReturn(
            mockInterval
        ).when(
            mockNsiMappingService
        ).nsiToOscarsSchedule(
            Mockito.any()
        );

        // Mock NsiMappingService.getOscarsConnection()
        Mockito.doReturn(
            mockConn
        ).when(
            mockNsiMappingService
        ).getOscarsConnection(Mockito.any());

        // ... Fill in mock fixtures and junctions
        List<Fixture> mockFixtures = helper.createFixtures(inMbps, outMbps, mbps);
        List<Junction> mockJunctions = helper.createJunctions();

        Pair<List<Fixture>, List<Junction>> mockFixturesAndJunctions = Pair.of(
            mockFixtures,
            mockJunctions
        );

        Mockito.doReturn(
            mockFixturesAndJunctions
        ).when(
            mockNsiMappingService
        ).simpleComponents(Mockito.any(), Mockito.anyInt());

        Mockito.doReturn(
            mockFixturesAndJunctions
        ).when(
            mockNsiMappingService
        ).fixturesAndJunctionsFor(
            Mockito.any(),
            Mockito.any(),
            Mockito.any()
        );

        // Mock the NsiMappingService.getP2PService() method.
        // Point to Point Service (P2P)
        P2PServiceBaseType mockP2P = new P2PServiceBaseType();
        mockP2P.setSourceSTP("ornl5600-cr6");
        mockP2P.setDestSTP("star-cr6");
        mockP2P.setCapacity(mbps);

        mockP2P.setDirectionality(DirectionalityType.BIDIRECTIONAL);

        MockStpListType mockEros = new MockStpListType();

        mockP2P.setEro(mockEros);
        // mockP2P.setSymmetricPath();

        Optional<P2PServiceBaseType> mockP2PService = Optional.of(mockP2P);
        Mockito.doReturn(
            mockP2PService
        ).when(mockNsiMappingService).getP2PService(
            Mockito.any()
        );

        // Mock the NsiMappingService.save() method
        Mockito.doReturn(
            mockNsiMapping
        ).when(mockNsiMappingService).save(Mockito.any(NsiMapping.class));

        // Mock NsiHeaderUtils, used by the NsiService.reserve() method
        // Mock NsiHeaderUtils.getRequesterNsa()
        NsiRequesterNSA mockRequesterNSA = NsiRequesterNSA
            .builder()
            .id(1L)
            .nsaId(nsaId)
            .callbackUrl("http://localhost:8080/callback")
            .build();


        // Mock the NsiHeaderUtils.getRequesterNsa() method
        mockNsiHeaderUtils = Mockito.mock(NsiHeaderUtils.class);

        Mockito
            .doReturn(mockRequesterNSA)
            .when(mockNsiHeaderUtils)
            .getRequesterNsa(Mockito.anyString());

        // Mock the ConnectionRequesterPort.reserveConfirmed() method
        mockConnectionRequesterPort = Mockito.mock(ConnectionRequesterPort.class);
        GenericAcknowledgmentType mockAcknowledgment = Mockito.mock(GenericAcknowledgmentType.class);
        Mockito
            .doReturn(mockAcknowledgment)
            .when(
                mockConnectionRequesterPort
            )
            .reserveConfirmed(Mockito.any(), Mockito.any());

        // Mock the NsiSoapClientUtil.createRequesterClient() method
        mockNsiSoapClientUtil = Mockito.mock(NsiSoapClientUtil.class);
        Mockito
            .doReturn(mockConnectionRequesterPort)
            .when(mockNsiSoapClientUtil)
            .createRequesterClient(Mockito.any());

        // Let's assert our mock NSI service picks up the
        // mock handlers
        setupMockNsiService();

        // Call the NSI Service reserve() method!
        // Start of the main try-catch block.
        // ... If NsiMapping mapping is null, it will attempt to create
        //     a new NsiMapping object before calling nsiMappingService.save(),
        //     and newReservation = true.
        //
        // ... If NsiMapping mapping is NOT null,
        //     it calls nsiStateEngine.reserve(NsiEvent.RESV_CHECK, mapping)
        //     then calls nsiMappingService.save()
        //
        // ... Start of the inner try-catch block.
        // ... Then it calls holdResult = this.hold(mockReserveType incomingRT, NsiMapping mapping),
        //     which should be mocked for this test. It can throw an exception.
        //
        // ... If holdResult.getSuccess() is true,
        //     nsiStateEngine.reserve(NsiEvent.RESV_CF, mapping) called
        //     and nsiMappingService.save(mapping) is called.
        //     NsiRequest is built, and nsiRequestManager.addInFlightRequest(nsiRequest) is called.
        //     nsiConnectionEventService.save( NsiConnectionEvent ) is called.
        //     this.reserveConfirmCallback() is called.
        //
        //     The reserve() function should return at this point.
        //     An exception may be thrown.
        //
        //     If the reserve() function did not return, an exception was thrown.
        //     Execution continues.
        //
        // ... If holdResult.getSuccess() is false,
        //     holdResult.getErrorMessage() is called,
        //     holdResult.getErrorCode() is called,
        //     holdResult.getTvps() is called.
        // ... End of the inner try-catch block.
        //
        // ... Execution continues.
        //
        // ... If execution continues, an exception occurred, and
        //     nsiConnectionEventService.save( NsiConnectionEvent ) is called.
        //     nsiStateEngine.reserve(NsiEvent.RESV_FL, mapping) is called.
        //     nsiMappingService.save(mapping) is called.
        //
        // ... If a NEW reservation was true, nsiMappingService.delete(mapping) is called.
        //
        // ... this.errCallback(NsiEvent.RESV_FL, nsaId, nsiConnectionId, mapping,
        //         errorMessage, errorCode, tvps, header.getCorrelationId())  is called.
        //
        // ... The function returns.
        //
        // ... If the main try-catch-block catches an exception


        reserved = nsiService.reserve(
            mockCommonHeaderType,
            mockNsiMapping,
            mockReserveType
        );
    }

    private void setupMockCommonHeaderType() {
        mockCommonHeaderType = new CommonHeaderType();

        mockCommonHeaderType.setProtocolVersion("1.0");
        mockCommonHeaderType.setCorrelationId("CORRID");
        mockCommonHeaderType.setRequesterNSA(nsaId);
        mockCommonHeaderType.setProviderNSA(nsaId);
        mockCommonHeaderType.setReplyTo("test@localhost");
    }

    private void setupMockNsiMapping() {
        setupMockNsiMapping(
            LifecycleStateEnumType.CREATED,
            ProvisionStateEnumType.PROVISIONING,
            ReservationStateEnumType.RESERVE_START
        );
    }
    private void setupMockNsiMapping(
        LifecycleStateEnumType lifecycleState,
        ProvisionStateEnumType provisionState,
        ReservationStateEnumType reservationState
    ) {
        mockNsiMapping = NsiMapping.builder()
            .nsiGri("MOCKGRI")
            .dataplaneVersion(1)
            .deployedDataplaneVersion(1)
            .lifecycleState(lifecycleState)
            .provisionState(provisionState)
            .nsiConnectionId(nsiConnectionId)
            .oscarsConnectionId(oscarsConnectionId)
            .nsaId(nsaId)
            .reservationState(reservationState)
            .build();
    }

    private void setupDatasources() throws Exception {
        Connection mockConn = helper.generateMockConnection();
        setupDatasources(mockConn);
    }
    private void setupDatasources(Connection mockConn) throws Exception {
        setupMockConnRepo(mockConn);
        setupMockConnSvc(mockConn);
    }
    private void setupMockConnRepo() throws Exception {
        Connection mockConnection = helper.generateMockConnection();
        setupMockConnRepo(mockConnection);
    }
    private void setupMockConnRepo(Connection mockConn) {
        connRepo = Mockito.mock(ConnectionRepository.class);
        Mockito
            .when(
                connRepo.save(Mockito.any(Connection.class))
            )
            .thenReturn(
                mockConn
            );
    }

    private void setupMockConnSvc() throws Exception {
        Connection mockConn = helper.generateMockConnection(oscarsConnectionId);
        setupMockConnSvc(mockConn);
    }
    private void setupMockConnSvc(Connection mockConn) throws Exception {
        connSvc = Mockito.mock(ConnService.class);

        Optional<Connection> mockConnOpt = Optional.of(mockConn);
        Mockito.when(
            connSvc.findConnection(Mockito.anyString())
        ).thenReturn(
            mockConnOpt
        );
        // Mock ConnService.verifyModification()
        Mockito.when(
            connSvc.verifyModification(Mockito.any(Connection.class))
        ).thenReturn(
            helper.createTrueValidity()
        );
        // Mock ConnService.modifySchedule()
        Mockito
            .doNothing()
            .when(
                connSvc
            )
            .modifySchedule(
                Mockito.any(Connection.class),
                Mockito.any(Instant.class),
                Mockito.any(Instant.class)
            );
        // Mock ConnService.modifyBandwidth()
        Mockito
            .doNothing()
            .when(connSvc)
            .modifyBandwidth(
                Mockito.any(Connection.class),
                Mockito.anyInt()
            );
        // Mock ConnService.findAvailableMaxBandwidth
        Mockito
            .when(
                connSvc.findAvailableMaxBandwidth(
                    Mockito.any(Connection.class),
                    Mockito.any(Components.class),
                    Mockito.any(Interval.class)
                )
            )
            .thenReturn(
                mbps
            );

        // Mock ConnService.validate()
        Mockito
            .when(
                connSvc.validate(
                    Mockito.any(SimpleConnection.class),
                    Mockito.any(ConnectionMode.class)
                )
            )
            .thenReturn(
                helper.createTrueValidity()
            );

        // Mock ConnService.holdConnection()
        // Mock ConnService.holdConnection(), returns Tuple <SimpleConnection, Connection>
        Pair<SimpleConnection, Connection> mockHoldConnection = Pair.of(
            helper.createSimpleConnection(
                oscarsConnectionId,
                inMbps,
                outMbps,
                azMbps,
                zaMbps,
                mbps
            ),
            mockConn
        );
        Mockito
            .when(
                connSvc.holdConnection(Mockito.any(SimpleConnection.class))
            )
            .thenReturn(
                mockHoldConnection
            );

        connSvc.setConnRepo(connRepo);
    }

    private void setupMockNsiService() throws Exception {
        try {
            // @TODO setup any mock data and adapters here.
            // @TODO How do we handle NsiService.errCallback?
            // @TODO How do we handle NsiService.okCallback?
//            nsiService = Mockito.mock(NsiService.class);
            nsiService.setResvTimeout(900);
            nsiService.setProviderNsa("urn:ogf:network:es.net:2013:nsa");

            nsiService.connRepo = connRepo;
            nsiService.connSvc = connSvc;

            nsiService.nsiRequestManager = mockNsiRequestManager;
            nsiService.nsiHeaderUtils = mockNsiHeaderUtils;
            nsiService.nsiStateEngine = mockNsiStateEngine;

            nsiService.nsiMappingService = mockNsiMappingService;
            nsiService.nsiQueries = mockNsiQueries;
            nsiService.nsiNotifications = mockNsiNotifications;
            nsiService.nsiConnectionEventService = mockNsiConnectionEventService;

            nsiService.setNsiSoapClientUtil(mockNsiSoapClientUtil);

        } catch (Exception ex) {
            world.add(ex);
            throw ex;
        }
    }

    private void setupForProvision() throws Exception {
        provisioned = false;

        // Before provision, the connection stuff needs to be Phase.RESERVED
        Connection mockConn = helper.generateMockConnection(
            oscarsConnectionId,
            Phase.RESERVED,
            BuildMode.AUTOMATIC,
            State.WAITING,
            DeploymentState.UNDEPLOYED,
            DeploymentIntent.SHOULD_BE_DEPLOYED,
            mbps
        );

        // Pass in Schedule object
        Schedule schedule = helper.createValidSchedule();

        Archived mockArchived = generateMockArchived(oscarsConnectionId, inMbps, outMbps, mbps, mbps, schedule);
        mockConn.setArchived(mockArchived);

        setupDatasources(mockConn);
        setupMockCommonHeaderType();
        setupMockNsiMapping(
            LifecycleStateEnumType.CREATED,
            ProvisionStateEnumType.RELEASED,
            ReservationStateEnumType.RESERVE_START
        );
        setupMockNsiService();

        setupMocks(mockConn);
    }

    private Archived generateMockArchived(String connectionId, int ingressMbps, int egressMbps, int azBandwidthMbps, int zaBandwidthMbps, Schedule schedule) {
        // @TODO create a Vlan object
        Vlan vlan1 = Vlan.builder()
            .id(1L)
            .connectionId(connectionId)
            .schedule(schedule)
            .urn("ornl5600-cr6:1/1/c31/1")
            .vlanId(2188)
            .build();

        Vlan vlan2 = Vlan.builder()
            .id(2L)
            .connectionId(connectionId)
            .schedule(schedule)
            .urn("star-cr6:1/1/c55/1")
            .vlanId(4073)
            .build();

        // @TODO fill in fixtures, junctions, pipes
        List<VlanFixture> vlanFixtures = new ArrayList<>();
        List<VlanJunction> vlanJunctions = new ArrayList<>();
        List<VlanPipe> vlanPipes = new ArrayList<>();

        VlanJunction junctionA = VlanJunction.builder()
            .id(1L)
            .refId("ornl5600-cr6")
            .connectionId(connectionId)
            .deviceUrn("ornl5600-cr6")
            .vlan(vlan1)
            .build();
        VlanJunction junctionZ = VlanJunction.builder()
            .id(2L)
            .refId("star-cr6")
            .connectionId(connectionId)
            .deviceUrn("star-cr6")
            .vlan(vlan2)
            .build();

        VlanFixture fixture1 = VlanFixture.builder()
            .connectionId(connectionId)
            .junction(junctionA)
            .portUrn("ornl5600-cr6:1/1/c31/1")
            .ingressBandwidth(ingressMbps)
            .egressBandwidth(egressMbps)
            .vlan(vlan1)
            .strict(false)
            .schedule(schedule)
//            .commandParams()
            .build();

        VlanFixture fixture2 = VlanFixture.builder()
            .connectionId(connectionId)
            .junction(junctionZ)
            .portUrn("star-cr6:1/1/c55/1")
            .ingressBandwidth(egressMbps)
            .egressBandwidth(ingressMbps)
            .vlan(vlan2)
            .strict(false)
            .schedule(schedule)
//            .commandParams()
            .build();

        List<EroHop> azERO = new ArrayList<>();
        List<EroHop> zaEro = new ArrayList<>();

        EroHop eroHopA = EroHop.builder()
            .id(1L)
            .urn("ornl5600-cr6:1/1/c31/1")
            .build();

        EroHop eroHopZ = EroHop.builder()
            .id(2L)
            .urn("star-cr6:1/1/c55/1")
            .build();

        azERO.add(
            eroHopA
        ); // Start
        azERO.add(
            eroHopZ
        ); // End

        zaEro.add(
            eroHopZ
        ); // End
        zaEro.add(
            eroHopA
        ); // Start

        vlanPipes.add(
            VlanPipe.builder()
                .connectionId(connectionId)
                .protect(false)
                .a(junctionA)
                .z(junctionZ)
                .azERO(azERO)
                .zaERO(zaEro)
                .azBandwidth(azBandwidthMbps)
                .zaBandwidth(zaBandwidthMbps)
                .schedule(schedule)
                .build()
        );

        return Archived.builder()
            .connectionId(connectionId)
            .cmp(
                Components.builder()
                    .pipes(vlanPipes)
                    .fixtures(vlanFixtures)
                    .junctions(vlanJunctions)
                    .build()
            )
            .schedule(
                schedule
            )
            .build();
    }

    private void setupForRelease() throws Exception {
        released = false;

        // Before provision, the connection stuff needs to be Phase.RESERVED
        Connection mockConn = helper.generateMockConnection(
            oscarsConnectionId,
            Phase.RESERVED,
            BuildMode.AUTOMATIC,
            State.ACTIVE,
            DeploymentState.DEPLOYED,
            DeploymentIntent.SHOULD_BE_UNDEPLOYED,
            mbps
        );

        // Pass in Schedule object
        Schedule schedule = helper.createValidSchedule();

        Archived mockArchived = generateMockArchived(oscarsConnectionId, inMbps, outMbps, mbps, mbps, schedule);
        mockConn.setArchived(mockArchived);

        setupDatasources(mockConn);
        setupMockCommonHeaderType();
        setupMockNsiMapping(
            LifecycleStateEnumType.CREATED,
            ProvisionStateEnumType.PROVISIONED,
            ReservationStateEnumType.RESERVE_START
        );
        setupMockNsiService();

        setupMocks(mockConn);
    }

    private void mockProvision() throws Exception {
        provisioned = nsiService.provision(
            mockCommonHeaderType,
            mockNsiMapping
        );
    }

    private void mockRelease() throws Exception {
        released = nsiService.release(
            mockCommonHeaderType,
            mockNsiMapping
        );
    }

    @Given("The NSI Service expects an exception")
    public void theNsiServiceExpectAnException() {
        world.expectException();
    }
    @Given("The NSI Service class is instantiated")
    public void theNsiServiceClassIsInstantiated() {
        assert nsiService != null;
    }

    // * NSI Reserve steps BEGIN *
    @When("The NSI Service submits a reservation for OSCARS connection ID {string}, global reservation ID {string}, NSI connection ID {string}")
    public void theNsiServiceSubmitsAReservationForConnectionID(String oscarsConnectionId, String globalReservationId, String nsiConnectionId) throws Throwable {
        try {

            // Part of the NSA protocol. An L2VPN identifier that may be on more than one NSI provider ("Fabric", etc).
            // We keep it, but don't actually use it for any tracking. Not guaranteed to be unique.
            // This must be a UUID.
            this.globalReservationId = globalReservationId;

            // Generated in OSCARS. Should be unique for that L2VPN instance.
            // It may be a UUID.
            this.nsiConnectionId = nsiConnectionId;

            // The OSCARS connection ID
            this.oscarsConnectionId = oscarsConnectionId;

            Connection mockConn = helper.generateMockConnection(this.oscarsConnectionId);

            setupMocks(mockConn);
        } catch (Exception ex) {

            world.add(ex);
            throw ex;
        }
    }

    @Then("The NSI Service made the reservation successfully.")
    public void theNsiServiceMadeTheReservationSuccessfully() {
        assert reserved;

    }
    // * NSI Reserve steps END *

    // * NSI Provision steps BEGIN *
    @Given("The NSI Service has a reserved connection")
    public void theNSIServiceHasAReservedConnection() throws Throwable {
        try {
            this.globalReservationId = "GLOBALID";
            this.nsiConnectionId = "RES1";
            this.oscarsConnectionId = "ABCD";

            setupForProvision();
        } catch (Exception ex) {
            world.add(ex);
            throw ex;
        }
    }

    @When("The NSI Service submits a provision request for a reserved connection")
    public void theNSIServiceSubmitsAProvisionRequestForAReservedConnection() throws Throwable {
        try {
             mockProvision();
        } catch (Exception ex) {
            world.add(ex);
            throw ex;
        }
    }

    @Then("The NSI Service made the provision request successfully.")
    public void theNSIServiceMadeTheProvisionRequestSuccessfully() {
        assert provisioned;
    }
    // * NSI Provision steps END *

    // * NSI Release steps BEGIN *
    @Given("The NSI Service class has a provisioned connection")
    public void theNSIServiceClassHasAProvisionedConnection() throws Throwable {
        try {

            this.globalReservationId = "GLOBALID";
            this.nsiConnectionId = "RES1";
            this.oscarsConnectionId = "ABCD";

            setupForRelease();
        } catch (Exception ex) {
            world.add(ex);
            throw ex;
        }
    }

    @When("The NSI Service submits a release request for a provisioned connection")
    public void theNSIServiceSubmitsAReleaseRequestForAProvisionedConnection() throws Throwable {
        try {
            mockRelease();
        } catch (Exception ex) {
            world.add(ex);
            throw ex;
        }
    }


    @Then("The NSI Service made the release request successfully.")
    public void theNSIServiceMadeTheReleaseRequestSuccessfully() {
    }
    // * NSI Release steps END *
}

class MockStpListType extends StpListType {
    @Override
    public List<OrderedStpType> getOrderedSTP() {
        List<OrderedStpType> orderedStpList = new ArrayList<>();

        OrderedStpType oStp = new OrderedStpType();
        oStp.setStp("urn:ogf:network:es.net:2013:nsa");
        oStp.setOrder(0);

        orderedStpList.add(oStp);

        return orderedStpList;
    }
}