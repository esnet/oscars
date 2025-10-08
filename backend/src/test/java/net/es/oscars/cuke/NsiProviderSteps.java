package net.es.oscars.cuke;

import static net.es.oscars.app.util.PrettyPrinter.prettyLog;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.text.StringSubstitutor;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import graphql.util.TraverserContext.Phase;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPMessage;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ReservationStateEnumType;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ReserveResponseType;
import net.es.oscars.app.Startup;
import net.es.oscars.app.util.AsyncCallback;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.nsi.beans.NsiConnectionEventType;
import net.es.oscars.nsi.db.NsiConnectionEventRepository;
import net.es.oscars.nsi.ent.NsiConnectionEvent;
import net.es.oscars.nsi.ent.NsiMapping;
import net.es.oscars.nsi.svc.NsiAsyncQueue;
import net.es.oscars.nsi.svc.NsiConnectionEventService;
import net.es.oscars.nsi.svc.NsiMappingService;
import net.es.oscars.nsi.svc.NsiService;
import net.es.oscars.nsi.svc.NsiStateEngine;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.soap.NsiProvider;
import net.es.oscars.topo.TopoService;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.pop.TopoPopulator;
import net.es.oscars.topo.svc.TopologyStore;

@Slf4j
@Category({UnitTests.class})
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                TopoService.class,
                NsiService.class,
                NsiStateEngine.class,
                NsiAsyncQueue.class,
                NsiConnectionEventService.class,
                NsiConnectionEventRepository.class,
                NsiMappingService.class,
                NsiProvider.class,
        }
)
public class NsiProviderSteps extends CucumberSteps {
    @Autowired
    private CucumberWorld world;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Startup startup;

    @Autowired
    private TopologyStore topoService;

    @Autowired
    private TopoPopulator topoPopulator;

    @Autowired
    private ConnService connService;

    @Autowired
    private NsiService nsiService;

    @Autowired
    private NsiStateEngine nsiStateEngine;

    @Autowired
    private NsiMappingService nsiMappingService;

    @Autowired
    private NsiConnectionEventService nsiConnectionEventService;

    @Autowired NsiConnectionEventRepository nsiConnectionEventRepo;

    @Autowired
    private NsiAsyncQueue queue;

    private ResponseEntity<String> response;

    private String testConnectionId;
    private String nsiResponseString;

    public static boolean isProcessingQueue;
    private static boolean isProcessingQueueSuccess;

    private static List<String> nsiStatesReserve;

    @Before("@NsiProviderSteps")
    public void before() throws Exception {
        
        try {
            startup.setInStartup(false);
            queue.queue.clear();
            topoService.clear();
            nsiService.setErrorCount(0);
            nsiConnectionEventService.getEventRepo().deleteAll();
            nsiConnectionEventRepo.deleteAll();
            loadTopology();
            releaseAllConnections();

            testConnectionId = "";
            nsiResponseString = "";

            isProcessingQueue = false;
            isProcessingQueueSuccess = false;

            nsiStatesReserve = new ArrayList<>();
            nsiStateEngine.setReserveHandler(new AsyncCallback<NsiMapping>() {
                @Override
                public void onSuccess(NsiMapping mapping) {
                    nsiStatesReserve.add(mapping.getReservationState().toString());
                }
                @Override
                public void onFailure(Throwable ex) {
                    world.add((Exception) ex);
                    log.error("NsiProviderSteps -> before(), setReserveHandler.onFailure() triggered, error: {}", ex.getLocalizedMessage());
                }
            });

        } catch (Exception e) {
            world.add(e);
            log.error("NsiProviderSteps Error - {}", e);
        }
    }

    @Given("The reservation state is now {string}")
    public void The_reservation_state_is_now_reserve_held(String expectedReservationState) throws Exception {
        try {
            assert !testConnectionId.isEmpty();
            
            NsiMapping mapping = nsiService.nsiMappingService.getMapping(testConnectionId);
            assert mapping != null;
            ReservationStateEnumType reservationState = mapping.getReservationState();

            log.info("The reservation state is now (expected) {}, actual {}", expectedReservationState, reservationState);
            assert reservationState.equals(ReservationStateEnumType.valueOf(expectedReservationState));
        } catch (Exception e) {
            world.add(e);
            log.error("The reservation state is now... error: {}", e);
            throw e;
        }
    }

    @Given("The NSI connection is queued for asynchronous reservation while not including a projectId")
    public void the_nsi_connection_is_queued_for_asynchronous_reservation_while_not_including_a_project_id() throws Throwable {
        try {
            nsiResponseString = queueNsiConnection();
        } catch (Exception e) {
            world.add(e);
            log.error("NsiProviderSteps Error - {}", e);
        }
    }

    @Given("The NSI connection is queued for asynchronous reservation while including a projectId")
    public void the_NSI_connection_is_queued_for_asynchronous_reservation_while_including_a_projectId() {
        try {
            nsiResponseString = queueNsiConnection(true, "ABCD-1234-EFGH-5678");
        } catch (Exception e) {
            world.add(e);
            log.error("NsiProviderSteps Error - {}", e);
        }
    }

    @Given("The NSI connection is queued for asynchronous reservation while including a blank projectId")
    public void the_NSI_connection_is_queued_for_asynchronous_reservation_while_including_a_blank_projectId() {
        try {
            nsiResponseString = queueNsiConnection(true, "");
        } catch (Exception e) {
            world.add(e);
            log.error("NsiProviderSteps Error - {}", e);
        }
    }

    @When("The NSI queue size is {}")
    public void the_NSI_queue_size_is(int expectedSize) {
        try {
            assert queue.queue != null && !queue.queue.isEmpty();
            log.info("NSI QUEUE size: {}", queue.queue.size());
            assert queue.queue.size() == expectedSize;
        } catch (Exception e) {
            world.add(e);
            log.error("NsiProviderSteps Error - {}", e);
        }
    }
    
    @Given("The connection is not reserved yet")
    public void A_connection_is_not_reserved_yet() {
        assert connService.getHeld().isEmpty();
    }

    @When("The NSI queue is processed")
    public void the_NSI_queue_is_processed() throws Exception {
        log.debug("NSI queue is processed - queue.processQueue() called. NOTE, this is a multi-threaded (async) operation! Do we have a callback event handler?");
        try {
            NsiProviderSteps.isProcessingQueue = true;
            queue.processQueueHandler = new AsyncCallback<String>() {
                @Override
                public void onSuccess(String message) {
                    log.info("NSI queue.processQueueCallback(), onSuccess event: {}", message);
                    NsiProviderSteps.isProcessingQueue = false;
                    NsiProviderSteps.isProcessingQueueSuccess = true;
                }

                @Override
                public void onFailure(Throwable thrown) {
                    world.add((Exception) thrown);
                    log.error("NSI queue.processQueueCallback(), onFailure event: {}", thrown);
                    NsiProviderSteps.isProcessingQueue = false;

                }
            };
            
            queue.processQueue();

            while (NsiProviderSteps.isProcessingQueue) {
                // NO-OP
            }
        } catch (Exception ex) {
            world.add(ex);
            log.error("The NSI queue is processed: Error - {}", ex.getLocalizedMessage());
            NsiProviderSteps.isProcessingQueue = false;
        }

        assert NsiProviderSteps.isProcessingQueueSuccess == true;
    }

    @When("An NSI connection reserve is requested")
    public void An_NSI_connection_reserve_is_requested() throws Exception {
        try {
            nsiResponseString = queueNsiConnection(true, "ABCD-1234-EFGH-5678");
            log.info("NSI RESPONSE was: {}", nsiResponseString);
            ReserveResponseType reserveResponse = deserializeXmlReserveResponseType(nsiResponseString);

            assert !reserveResponse.getConnectionId().isEmpty();

            log.info("NSI connection reserve is requested, connectionID is: " + reserveResponse.getConnectionId());
            
            // the_NSI_queue_is_processed();

            testConnectionId = reserveResponse.getConnectionId();
            // nsiConnectionEventRepo.findAll().forEach((nsiConnEv) -> {
            //     log.info("NSI connection reserve is requested, event repo event is: ", reserveResponse);
            // });

        } catch (Exception e) {
            world.add(e);
            log.error("An NSI connection reserve is requested, error: {}", e);
            throw e;
        }
    }

    @When("The NSI mapping and connection object is created")
    public void The_NSI_mapping_and_connection_object_is_created() throws Exception {
        try {
            log.debug("Checking the NSI Mapping and connection object.");
            // Grab the mapping using the NSI connection ID.
            boolean hasMapping = nsiMappingService.hasMapping(testConnectionId);
            log.info("NSI mapping service, check if has mapping for {}. Has? {}", testConnectionId, hasMapping);
            assert hasMapping;
            NsiMapping nsiMapping = nsiMappingService.getMapping(testConnectionId);
            log.info("nsiMapping is {}", nsiMapping);
            assert nsiMapping != null;

            // Note, the ConnService class expects OSCARS connection IDs.
            // Thankfully, the NsiMapping object has a way to get that for us.
            Optional<Connection> optConn = nsiService
                .getConnSvc()
                .findConnection(
                    nsiMapping
                        .getOscarsConnectionId() // Find by OSCARS connection ID
                );
            assert optConn.isPresent();
        } catch (Exception e) {
            world.add(e);
            log.error("The NSI mapping and connection object is created, error: {}", e);
            throw e;
        }
    }

    @When("An NSI connection commit is requested")
    public void An_NSI_connection_commit_is_requested() {
        // Write code here that turns the phrase above into concrete actions
    }

    @Then("The NSI connection does not have a projectId")
    public void the_NSI_connection_does_not_have_a_projectId() {
        try {
            queue.queue.forEach(element -> {
                prettyLog(element);
            });
        } catch (Exception e) {
            world.add(e);
            log.error("NsiProviderSteps Error - {}", e);
        }
    }

    @Then("The NSI connection is put on hold")
    public void the_NSI_connection_is_put_on_hold() {
        try {
            assert connService.getHeld().size() == 1;
        } catch (Exception e) {
            world.add(e);
            log.error("NsiProviderSteps Error - {}", e);
        }
    }

    @Then("The NSI connection has a projectId")
    public void the_NSI_connection_has_a_projectId() {
        try {
            connService.getHeld().forEach((id, conn) -> {
                log.info("Connection {} has projectId {}", id, conn.getProjectIds());
                assert conn.getProjectIds() != null;
            });
        } catch (Exception e) {
            world.add(e);
            log.error("NsiProviderSteps Error - {}", e);
        }
    }

    @Then("The NSI provider encountered {int} errors")
    public void the_NSI_provider_encountered_errors(int errorCount) {
        assert nsiService.getErrorCount() == errorCount;
        assert world.getExceptions().size() == errorCount;
    }

    @Then("The connection phase is {string}")
    public void The_connection_phase_is(String strExpectedPhase) throws Exception {
        try {
            Phase expectedPhase = Phase.valueOf(strExpectedPhase);

        } catch (Exception e) {
            // TODO: handle exception
            world.add(e);
            log.error("Test failed to compare connection phase. error: ", e);
        }
    }

    @Then("The resources are no longer available for something else")
    public void The_resources_are_no_longer_available_for_something_else() {
        // Write code here that turns the phrase above into concrete actions
    }

    @Then("The resources are available for something else")
    public void The_resources_are_available_for_something_else() {
        // Write code here that turns the phrase above into concrete actions
    }

    @Then("The reserveConfirmed message callback is triggered")
    public void The_reserveConfirmed_message_callback_is_triggered() {
        // Write code here that turns the phrase above into concrete actions
    }

    @Then("The reserveFailed message callback is triggered")
    public void The_reserveFailed_message_callback_is_triggered() {
        // Write code here that turns the phrase above into concrete actions
    }

    @Then("The latest connection event type is {string}")
    public void The_latest_connection_event_is(String expectedNsiConnectionEventType) {
        try {
            assert nsiConnectionEventService.getEventRepo().count() != 0;
            List<NsiConnectionEvent> connectionEvents = nsiConnectionEventService
                .getEventRepo()
                .findByNsiConnectionId(testConnectionId);
            
            assert connectionEvents.size() != 0;

            NsiConnectionEvent connEv = connectionEvents.getLast();
            boolean isState = connEv
                .getType()
                .equals(
                    NsiConnectionEventType
                        .valueOf(expectedNsiConnectionEventType)
                );
            log.info("The latest connection event type  expected is {}, actual {}", expectedNsiConnectionEventType, connEv.getType());
            assert isState;
        } catch (Exception e) {
            world.add(e);
            log.error("The latest connection event is ..., error: {}", e);
            throw e;
        }
    }

    @Then("The reservation state path was {string}")
    public void The_reservation_state_path_was(String flow) {
        List<String> states = Arrays.asList(flow.replace(" ", "").split("->"));

        assert nsiStatesReserve.equals(states);
    }

    

    private void loadTopology() throws Exception {
        String topoPath = "topo/esnet.json";
        Topology t = topoPopulator.loadTopology(topoPath);
        if (t == null) throw new Exception(topoPath + " is not a topology");
        topoService.replaceTopology(t);
    }

    private String queueNsiConnection() throws Exception {
        return queueNsiConnection(false, null);
    }

    private String queueNsiConnection(boolean withProjectId, String projectIdValue) throws Exception {
        HttpMethod method = HttpMethod.POST;
        String xmlDataTemplate = "http/nsi.reserve.template.xml";
        Map<String, String> valuesMap = new HashMap<>();
        Instant now = Instant.now();

        log.info("NOW time used is {}", now.toString());

        Instant startTime = now;
        Instant endTime = now.plusSeconds(60 * 20);

        log.info("START time will be {}", startTime.toString());
        log.info("END time will be {}", endTime.toString());

        valuesMap.put("startTime",  startTime.toString() );
        valuesMap.put("endTime", endTime.toString() );

        if (withProjectId) {
            xmlDataTemplate = "http/nsi.reserve.template.xml";
            valuesMap.put("projectId", projectIdValue);
        } else {
            xmlDataTemplate = "http/nsi.reserve.no-project-id.template.xml";
        }

        String url = "/services/provider?ServiceName=NsiProviderService&PortName=NsiProviderPort&PortTypeName=ConnectionProviderPort";
        InputStream bodyInputStream = new ClassPathResource(xmlDataTemplate).getInputStream();
        String payload = StreamUtils.copyToString(bodyInputStream, Charset.defaultCharset());

        StringSubstitutor sub = new StringSubstitutor(valuesMap);

        payload = sub.replace(payload);

        response = new ResponseEntity<>("", HttpStatus.NOT_FOUND);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        response = restTemplate.exchange(url, method, entity, String.class);

        assert response.getStatusCode() == HttpStatus.OK;
        String body = response.getBody();

        return body;
    }

    public void releaseAllConnections() {
        connService.getHeld().clear();
    }

    public ReserveResponseType deserializeXmlReserveResponseType(String xml) throws Exception {
        SOAPMessage soapMessage = MessageFactory.newInstance().createMessage(null, new ByteArrayInputStream(xml.getBytes()));
        SOAPBody soapBody = soapMessage.getSOAPBody();
        Document doc = soapBody.extractContentAsDocument();
        Element element = doc.getDocumentElement();

        // Don't want to deal with marshallizing/unmarshalizing just to extract the ReserveResponse element from the Envelope...
        // Grab what we need and set the connectionId accordingly.

        ReserveResponseType reserveResponse = new ReserveResponseType();
        reserveResponse.setConnectionId(
            element.getElementsByTagName("connectionId")
                .item(0) // use the first <connectionId> tag
                .getFirstChild() // Not sure why we are forced to "get first child", but this is how we get t
                .getNodeValue()
        );
        
        return reserveResponse;
    }
}
