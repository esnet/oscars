package net.es.oscars.cuke;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.BackendTestConfiguration;
import net.es.oscars.app.Startup;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.model.Interval;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.Components;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.ent.Held;
import net.es.oscars.resv.enums.*;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.web.beans.*;
import net.es.oscars.web.rest.ModifyController;
import net.es.oscars.web.simple.Validity;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

@Slf4j
@Category({UnitTests.class})
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        BackendTestConfiguration.class,
        ConnectionRepository.class,
        ConnService.class,
        ModifyController.class,
    }
)
public class ModifyControllerSteps extends CucumberSteps {
    @Autowired
    private CucumberWorld world;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MockSimpleConnectionHelper helper;

    @Autowired
    private Startup startup;

    @MockitoBean
    private ConnectionRepository connRepo;

    @MockitoBean
    private ConnService connSvc;

    @Autowired
    private ModifyController controller;

    private ResponseEntity<String> response;

    private void setupDatasources() throws Exception {
        setupMockConnRepo();
        setupMockConnSvc();
    }
    private void setupMockConnRepo() {
        connRepo = Mockito.mock(ConnectionRepository.class);
        Connection mockConnection = helper.generateMockConnection();
        Mockito
            .when(
                connRepo.save(Mockito.any(Connection.class))
            )
            .thenReturn(
                mockConnection
            );

        controller.setConnRepo(connRepo);
    }
    private void clear() {
        response = null;
    }

    private void setupMockConnSvc() throws Exception {
        connSvc = Mockito.mock(ConnService.class);
        Connection mockConn = helper.generateMockConnection();
        Optional<Connection> mockConnOpt = Optional.of(mockConn);
        // Mock ConnService.findConnection()
        Mockito.when(
            connSvc.findConnection(Mockito.anyString())
        ).thenReturn(
            mockConnOpt
        );
        // Mock ConnService.verifyModification()
        Mockito.when(
            connSvc.verifyModification(Mockito.any(Connection.class))
        ).thenReturn(
            Validity.builder()
                .valid(true)
                .message("valid test message")
                .build()
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
                10000
            );

        connSvc.setConnRepo(connRepo);
        controller.setConnSvc(connSvc);
    }

    @Before("@ModifyControllerSteps")
    public void before() throws Exception {
        // Reset stuff
        clear();

        // Setup mock data sources
        MockitoAnnotations.openMocks(this);
        setupDatasources();

        // Mock startup
        startup.setInStartup(false);
    }
    @Given("The client executes POST with a new description payload on ModifyController path {string}")
    public void theClientExecutesPOSTWithANewDescriptionPayloadOnModifyControllerPath(String httpPath) throws Exception {
        HttpMethod method = HttpMethod.POST;
        try {
            log.info("Executing " + method + " on ModifyController path " + httpPath);
            ObjectMapper mapper = new ObjectMapper();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            DescriptionModifyRequest descriptionModifyRequest = DescriptionModifyRequest.builder()
                .connectionId("ABCD")
                .description("A new description")
                .build();

            String payload = mapper.writeValueAsString(descriptionModifyRequest);

            HttpEntity<String> entity = new HttpEntity<>(payload, headers);

            response = restTemplate.exchange(httpPath, method, entity, String.class);
        } catch (Exception ex) {
            world.add(ex);
            log.error(ex.getLocalizedMessage(), ex);
        }
    }

    @Given("The client executes POST with a ScheduleRangeRequest payload on ModifyController path {string}")
    public void theClientExecutesPOSTWithAScheduleRangeRequestPayloadOnModifyControllerPath(String httpPath) throws Exception {
        HttpMethod method = HttpMethod.POST;
        try {
            log.info("Executing " + method + " on ModifyController path " + httpPath);
            ObjectMapper mapper = new ObjectMapper();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            ScheduleRangeRequest scheduleRangeRequest = ScheduleRangeRequest.builder()
                .connectionId("ABCD")
                .type(ScheduleModifyType.BEGIN)
                .build();

            String payload = mapper.writeValueAsString(scheduleRangeRequest);

            HttpEntity<String> entity = new HttpEntity<>(payload, headers);

            response = restTemplate.exchange(httpPath, method, entity, String.class);
        } catch (Exception ex) {
            world.add(ex);
            log.error(ex.getLocalizedMessage(), ex);
        }
    }

    @Given("The client executes POST with a BandwidthModifyRequest payload on ModifyController path {string}")
    public void theClientExecutesPOSTWithABandwidthModifyRequestPayloadOnModifyControllerPath(String httpPath) throws Exception {
        HttpMethod method = HttpMethod.POST;
        try {
            log.info("Executing " + method + " on ModifyController path " + httpPath);
            ObjectMapper mapper = new ObjectMapper();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            ScheduleModifyRequest scheduleModifyRequest = ScheduleModifyRequest.builder()
                .connectionId("ABCD")
                .type(ScheduleModifyType.BEGIN)
                .timestamp(Instant.now().toEpochMilli())
                .build();

            String payload = mapper.writeValueAsString(scheduleModifyRequest);

            HttpEntity<String> entity = new HttpEntity<>(payload, headers);

            response = restTemplate.exchange(httpPath, method, entity, String.class);
        } catch (Exception ex) {
            world.add(ex);
            log.error(ex.getLocalizedMessage(), ex);
        }
    }

    @Given("The client executes POST with a BandwidthRangeRequest payload on ModifyController path {string}")
    public void theClientExecutesPOSTWithABandwidthRangeRequestPayloadOnModifyControllerPath(String httpPath) throws Exception {
        HttpMethod method = HttpMethod.POST;
        try {
            log.info("Executing " + method + " on ModifyController path " + httpPath);
            ObjectMapper mapper = new ObjectMapper();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            BandwidthRangeRequest bandwidthRangeRequest = BandwidthRangeRequest.builder()
                .connectionId("ABCD")
                .build();

            String payload = mapper.writeValueAsString(bandwidthRangeRequest);

            HttpEntity<String> entity = new HttpEntity<>(payload, headers);

            response = restTemplate.exchange(httpPath, method, entity, String.class);
        } catch (Exception ex) {
            world.add(ex);
            log.error(ex.getLocalizedMessage(), ex);
        }
    }

    @When("The client receives a response from ModifyController")
    public void theClientReceivesAResponseFromModifyController() {
        assert response != null;
    }

    @Then("The client receives a ModifyController response status code of {int}")
    public void theClientReceivesAModifyControllerResponseStatusCodeOf(int statusCode) {
        log.info("response status code: " + response.getStatusCode());
        assertEquals(statusCode, response.getStatusCode().value());
    }

    @Then("The ModifyController response is a valid ScheduleRangeResponse object")
    public void theModifyControllerResponseIsAValidScheduleRangeResponseObject() throws JsonProcessingException {
        assert response != null;
        ObjectMapper mapper = new ObjectMapper();
        String payload = response.getBody();
        ScheduleRangeRequest scheduleRangeRequest = mapper.readValue(
            payload,
            ScheduleRangeRequest.class
        );
        assert scheduleRangeRequest != null;
    }


    @Then("The ModifyController response is a valid ModifyResponse object")
    public void theModifyControllerResponseIsAValidModifyResponseObject() throws JsonProcessingException {
        assert response != null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String payload = response.getBody();
        log.info("response: " + payload);
        ModifyResponse modifyResponse = mapper.readValue(
            payload,
            ModifyResponse.class
        );
        assert modifyResponse != null;
    }

    @Then("The ModifyController response is a valid BandwidthRangeResponse object")
    public void theModifyControllerResponseIsAValidBandwidthRangeResponseObject() throws JsonProcessingException {
        assert response != null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String payload = response.getBody();
        log.info("response: " + payload);
        BandwidthRangeResponse bandwidthRangeResponse = mapper.readValue(
            payload,
            BandwidthRangeResponse.class
        );
        assert bandwidthRangeResponse != null;
    }
}
