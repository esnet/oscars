package net.es.oscars.cuke;

import static net.es.oscars.app.util.PrettyPrinter.prettyLog;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.time.Instant;
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
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPMessage;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ReserveResponseType;
import net.es.oscars.app.Startup;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.nsi.beans.NsiConnectionEventType;
import net.es.oscars.nsi.ent.NsiConnectionEvent;
import net.es.oscars.nsi.svc.NsiAsyncQueue;
import net.es.oscars.nsi.svc.NsiConnectionEventService;
import net.es.oscars.nsi.svc.NsiService;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.soap.NsiProvider;
import net.es.oscars.topo.TopoService;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.pop.TopoPopulator;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.oscars.resv.enums.Phase;

@Slf4j
@Category({UnitTests.class})
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                TopoService.class,
                NsiService.class,
                NsiAsyncQueue.class,
                NsiConnectionEventService.class,
                
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
    private NsiConnectionEventService nsiConnectionEventService;

    @Autowired
    private NsiAsyncQueue queue;

    private ResponseEntity<String> response;

    private String testConnectionId;
    private String nsiResponseString;

    @Before("@NsiProviderSteps")
    public void before() throws Exception {
        
        try {
            startup.setInStartup(false);
            queue.queue.clear();
            topoService.clear();
            nsiService.setErrorCount(0);
            loadTopology();
            releaseAllConnections();

            testConnectionId = "";
            nsiResponseString = "";
        } catch (Exception e) {
            world.add(e);
            log.error("NsiProviderSteps Error - {}", e);
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

    @When("The NSI reserve is requested")
    public void the_NSI_reserve_is_requested() {
        try {
            queue.processQueue();
        } catch (Exception e) {
            world.add(e);
            log.error("NsiProviderSteps Error - {}", e);
        }
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
                log.info("Connection {} has projectId {}", id, conn.getProjectId());
                assert conn.getProjectId() != null;
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

    @Given("The connection is not reserved yet")
    public void A_connection_is_not_reserved_yet() {
        assert connService.getHeld().isEmpty();
    }

    @Given("The connection reservation state is {string}")
    public void A_connection_reservation_state_is_reserve_checking(String expectedReservationState) {
        try {
            // Find the connection
            List<NsiConnectionEvent> connEvents = nsiConnectionEventService.getEventRepo().findByNsiConnectionId(testConnectionId);
            assert connEvents.size() != 0;
            assert connEvents.getLast().getType().equals(NsiConnectionEventType.valueOf(expectedReservationState));
            
        } catch (Exception e) {
            world.add(e);
            log.error("NsiProviderSteps Error - {}", e);
        }
    }

    @When("An NSI connection reserve is requested")
    public void An_NSI_connection_reserve_is_requested() throws Exception {
        nsiResponseString = queueNsiConnection(true, "ABCD-1234-EFGH-5678");
        log.info("NSI RESPONSE was: {}", nsiResponseString);
        ReserveResponseType reserveResponse = deserializeXmlReserveResponseType(nsiResponseString);

        assert !reserveResponse.getConnectionId().isEmpty();

        log.info("NSI connection ID is: " + reserveResponse.getConnectionId());
        testConnectionId = reserveResponse.getConnectionId();

    }

    @When("The resources ARE available")
    public void The_resources_ARE_available() {
        // Write code here that turns the phrase above into concrete actions
    }

    @When("The resources ARE NOT available")
    public void The_resources_ARE_NOT_available() {
        // Write code here that turns the phrase above into concrete actions
    }

    @Then("The NSI mapping and connection object is created")
    public void The_NSI_mapping_and_connection_object_is_created() {
        // Write code here that turns the phrase above into concrete actions
    }

    @Then("The reservation state is now {string}")
    public void The_reservation_state_is_now_reserve_held(String expectedReservationState) {
        
        assert nsiConnectionEventService.getEventRepo().count() != 0;
        List<NsiConnectionEvent> connectionEvents = nsiConnectionEventService.getEventRepo().findByNsiConnectionId(testConnectionId);
        
        assert connectionEvents.size() != 0;

        NsiConnectionEvent connEv = connectionEvents.getLast();
        assert connEv.getType().equals(NsiConnectionEventType.valueOf(expectedReservationState));
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
                .item(0)
                .getFirstChild()
                .getNodeValue()
        );
        
        return reserveResponse;
    }
}
