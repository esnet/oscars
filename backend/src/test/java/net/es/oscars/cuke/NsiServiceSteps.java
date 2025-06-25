package net.es.oscars.cuke;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.*;
import net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.model.Interval;
import net.es.oscars.nsi.beans.NsiReserveResult;
import net.es.oscars.nsi.ent.NsiMapping;
import net.es.oscars.nsi.svc.*;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.soap.NsiSoapClientUtil;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.time.Instant;
import java.util.Date;

@Slf4j
@Category({UnitTests.class})
public class NsiServiceSteps extends CucumberSteps {
    @Autowired
    private CucumberWorld world;

    @Autowired
    @Mock
    private NsiService nsiService;

    // Mock the NsiService constructor arguments
    @Mock
    @Autowired
    private ConnectionRepository mockConnectionRepository;
    @Mock
    @Autowired
    private NsiRequestManager mockNsiRequestManager;
    @Mock
    @Autowired
    private NsiHeaderUtils mockNsiHeaderUtils;
    @Mock
    @Autowired
    private NsiStateEngine mockNsiStateEngine;
    @Mock
    @Autowired
    private ConnService mockConnService;
    @Mock
    @Autowired
    private NsiMappingService mockNsiMappingService;
    @Mock
    @Autowired
    private NsiSoapClientUtil mockNsiSoapClientUtil;
    @Mock
    @Autowired
    private ObjectMapper mockObjectMapper;
    @Mock
    @Autowired
    private NsiQueries mockNsiQueries;
    @Mock
    @Autowired
    private NsiNotifications mockNsiNotifications;
    @Mock
    @Autowired
    private NsiConnectionEventService mockNsiConnectionEventService;

    @Before("@NsiServiceSteps")
    public void before() {
        setupMockNsiService();
    }

    private void setupMockNsiService() {
        // @TODO setup any mock data and adapters here.
        // @TODO How do we handle NsiService.errCallback?
        // @TODO How do we handle NsiService.okCallback?
        nsiService = Mockito.mock(NsiService.class);
        nsiService.connRepo = mockConnectionRepository;
        nsiService.nsiRequestManager = mockNsiRequestManager;
        nsiService.nsiHeaderUtils = mockNsiHeaderUtils;
        nsiService.nsiStateEngine = mockNsiStateEngine;
        nsiService.connSvc = mockConnService;
        nsiService.nsiMappingService = mockNsiMappingService;
        nsiService.nsiSoapClientUtil = mockNsiSoapClientUtil;
        nsiService.jacksonObjectMapper = mockObjectMapper;
        nsiService.nsiQueries = mockNsiQueries;
        nsiService.nsiNotifications = mockNsiNotifications;
        nsiService.nsiConnectionEventService = mockNsiConnectionEventService;
    }

    @Given("The NSI Service expects an exception")
    public void theNsiServiceExpectAnException() {
        world.expectException();
    }
    @Given("The NSI Service class is instantiated")
    public void theNsiServiceClassIsInstantiated() {
        assert nsiService != null;
    }

    @When("The NSI Service submits a reservation for NSA {string}, connection ID {string}, global reservation ID {string}, NSI connection ID {string}, OSCARS connection ID {string}")
    public void theNsiServiceSubmitsAReservationForConnectionID(String nsa, String connectionId, String globalReservationId, String nsiConnectionId, String oscarsConnectionId) {

        // @TODO Attempt to call nsiService.reserve() for connectionId
        CommonHeaderType mockCommonHeaderType = Mockito.mock(CommonHeaderType.class);
        NsiMapping mockNsiMapping = Mockito.mock(NsiMapping.class);
        ReserveType mockReserveType = Mockito.mock(ReserveType.class);
        ScheduleType mockScheduleType = Mockito.mock(ScheduleType.class);
        ReservationRequestCriteriaType mockReservationRequestCriteriaType = Mockito.mock(ReservationRequestCriteriaType.class);

        mockNsiMappingService = Mockito.mock(NsiMappingService.class);

        try {

            Mockito.when(mockReservationRequestCriteriaType.getVersion()).thenReturn(1);
            Mockito.when(mockReservationRequestCriteriaType.getSchedule()).thenReturn(mockScheduleType);

            Mockito.when(mockCommonHeaderType.getRequesterNSA()).thenReturn(nsa);

            // a.k.a incomingRT
            Mockito.when(mockReserveType.getConnectionId()).thenReturn(connectionId);
            Mockito.when(mockReserveType.getGlobalReservationId()).thenReturn(globalReservationId);
            Mockito.when(mockReserveType.getCriteria()).thenReturn(mockReservationRequestCriteriaType);

            Mockito.when(mockNsiMapping.getNsiConnectionId()).thenReturn(nsiConnectionId);
            Mockito.when(mockNsiMapping.getOscarsConnectionId()).thenReturn(oscarsConnectionId);

            // Set the mock data for NSI Service reserve()
            Mockito.doReturn(
                // @TODO Set mock NsiMapping result object
                NsiMapping.builder()
                    .nsiGri("MOCKGRI")
                    .dataplaneVersion(1)
                    .deployedDataplaneVersion(1)
                    .lifecycleState(LifecycleStateEnumType.CREATED)
                    .provisionState(ProvisionStateEnumType.PROVISIONING)
                    .nsiConnectionId(nsiConnectionId)
                    .oscarsConnectionId(oscarsConnectionId)
                    .nsaId(nsa)
                    .reservationState(ReservationStateEnumType.RESERVE_START)
                    .build()
            ).when(mockNsiMappingService).newMapping(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyInt(),
                Mockito.anyBoolean()
            );

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

            // Mock the NsiService.hold() method, as it is called by reserve()
//            Mockito.mock(nsiService).hold(Mockito.any(), Mockito.any());
            Mockito.doReturn(
                NsiReserveResult.builder()
                    .success(true)
                    .build()
            ).when(nsiService).hold(Mockito.any(), Mockito.any());

            nsiService.reserve(
                mockCommonHeaderType,
                mockNsiMapping,
                mockReserveType
            );
        } catch (Exception ex) {
            world.add(ex);
        }
    }

    @Then("The NSI Service made the reservation successfully.")
    public void theNsiServiceMadeTheReservationSuccessfully() {
        // @TODO Check the nsiService reservation results
    }
}
