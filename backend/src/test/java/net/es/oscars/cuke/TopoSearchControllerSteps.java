package net.es.oscars.cuke;

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
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.enums.*;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.resv.svc.ResvService;
import net.es.oscars.topo.beans.PortBwVlan;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.beans.v2.EdgePort;
import net.es.oscars.topo.pop.TopoPopulator;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.oscars.web.beans.v2.PortSearchRequest;
import net.es.oscars.web.rest.v2.TopoSearchController;
import net.es.oscars.web.simple.SimpleConnection;
import net.es.oscars.web.simple.Validity;
import net.es.topo.common.model.oscars1.IntRange;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
@Category({UnitTests.class})
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        BackendTestConfiguration.class,
        TopologyStore.class,
        ConnectionRepository.class,
        ConnService.class,
        ResvService.class,
        TopoSearchController.class,
    }
)
public class TopoSearchControllerSteps {
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
    private ConnService connService;

    @MockitoBean
    private ResvService resvService;

    @Autowired
    @MockitoBean
    private TopologyStore topologyStore;

    @Autowired
    private TopoPopulator topoPopulator;
    private Topology topology;

    @Autowired
    private TopoSearchController controller;

    private ResponseEntity<String> response;

    @Before("@TopoSearchControllerSteps")
    public void before() throws Exception {
        // Reset stuff
        clear();

        // Setup mock data sources
        setupDatasources();

        // Mock startup
        startup.setInStartup(false);
    }

    private void clear() {
        response = null;
    }

    private void setupDatasources() throws Exception {
        setupMockConnRepo();
        setupMockConnSvc();
        setupMockResvService();
        setupMockTopologyStore("topo/esnet.json");
    }

    private void setupMockConnRepo() {
        connRepo = Mockito.mock(ConnectionRepository.class);
        Connection mockConn = generateMockConnection();
        Optional<Connection> mockConnOpt = Optional.of(mockConn);

        // Mock connRepo.findByConnectionId
        Mockito.when(
            connRepo
                .findByConnectionId(Mockito.anyString())
        ).thenReturn(
            mockConnOpt
        );

        controller.setConnRepo(connRepo);
    }
    private Connection generateMockConnection() {
        return Connection.builder()
            .connectionId("ABCD")
            .phase(Phase.HELD)
            .mode(BuildMode.AUTOMATIC)
            .state(State.WAITING)
            .deploymentState(DeploymentState.UNDEPLOYED)
            .deploymentIntent(DeploymentIntent.SHOULD_BE_DEPLOYED)
            .username("test")
            .description("test description")
            .connection_mtu(10000)
            .last_modified( ((Long) Instant.now().getEpochSecond()).intValue() )
            .projectId("ABCD-1234-EFGH-5678")
            .build();
    }
    private void setupMockConnSvc() throws Exception {
        connService = Mockito.mock(ConnService.class);

        // Mock ConnService.extendHold()
        Mockito
            .when(
                connService.extendHold(Mockito.anyString()
                ))
            .thenReturn(
                Instant.now()
            );
        // Mock ConnService.validate()
        Mockito
            .when(
                connService.validate(
                    Mockito.any(SimpleConnection.class),
                    Mockito.any(ConnectionMode.class)
                )
            )
            .thenReturn(
                Validity.builder()
                    .valid(true)
                    .message("valid test message")
                    .build()
            );

        // Mock ConnService.holdConnection(), returns Tuple <SimpleConnection, Connection>
        Pair<SimpleConnection, Connection> mockHoldConnection = Pair.of(
            helper.createSimpleConnection(
                10000,
                10000,
                10000,
                10000,
                10000
            ),
            generateMockConnection()
        );
        Mockito
            .when(
                connService.holdConnection(Mockito.any(SimpleConnection.class))
            )
            .thenReturn(
                mockHoldConnection
            );

        connService.setConnRepo(connRepo);
        controller.setConnService(connService);
    }

    private void setupMockResvService() throws Exception {
        resvService = Mockito.mock(ResvService.class);

        // Mock resvService.available()
        Set<IntRange> mockVlanRanges = new HashSet<>();
        mockVlanRanges.add(
            IntRange.builder()
                .floor(2)
                .ceiling(5)
                .build()
        );

        mockVlanRanges.add(
            IntRange.builder()
                .floor(7)
                .ceiling(4095)
                .build()
        );

        Mockito
            .doReturn(
                Map.of(
                    "ornl5600-cr6:1/1/c31/1",
                    PortBwVlan.builder()
                        .vlanRanges(mockVlanRanges)
                        .vlanExpression("")
                        .ingressBandwidth(400000)
                        .egressBandwidth(400000)
                        .build()
                )
            )
            .when(resvService)
            .available(
                Mockito.any(Interval.class),
                Mockito.any(),
                Mockito.anyString()
            );

        // Mock resvService.vlanUsage()
        Map<String, Map<Integer, Set<String>>> mockVlanUsageResult = new HashMap<>();
        String portUrn = "ornl5600-cr6:1/1/c31/1";
        Integer vlanId = 1176;
        String connectionId = "ABCD";

        mockVlanUsageResult.put(portUrn, new HashMap<>());
        mockVlanUsageResult.get(portUrn).put(vlanId, new HashSet<>());
        mockVlanUsageResult.get(portUrn).get(vlanId).add(connectionId);

        Mockito
            .doReturn(
                mockVlanUsageResult
            )
            .when(resvService)
            .vlanUsage(
                Mockito.any(Interval.class),
                Mockito.any(),
                Mockito.anyString()
            );

        controller.setResvService(resvService);
    }

    private void setupMockTopologyStore(String sourceFilePath) throws Exception {
        topologyStore = Mockito.mock(TopologyStore.class);


        // Provide mock data for topologyStore.getCurrentTopology()
        // ... Load mock topology data
        log.info("TopoSearchControllerSteps.setupMockTopologyStore() - Loading topology from " + sourceFilePath);
        topology = topoPopulator.loadTopology(sourceFilePath);
        if (topology == null) throw new Exception(sourceFilePath + " is not a topology");
        topologyStore.replaceTopology(topology);

        controller.setTopologyStore(topologyStore);
    }
    @Given("The client executes POST with PortSearchRequest payload on TopoSearchController path {string}")
    public void theClientExecutesPOSTWithPortSearchRequestPayloadOnTopoSearchControllerPath(String httpPath) throws Exception {
        HttpMethod method = HttpMethod.POST;
        try {
            log.info("Executing " + method + " on HoldController path " + httpPath);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            Instant now = Instant.now();

            PortSearchRequest portSearchRequest = PortSearchRequest.builder()
                .term("")
                .interval(
                    Interval.builder()
                        .beginning(now)
                        .ending(now.plusSeconds(60 * 20)) // 20 minutes
                        .build()
                )
                .device("ornl5600-cr6")
                .connectionId("ABCD")
                .build();

            String payload = mapper.writeValueAsString(portSearchRequest);

            HttpEntity<String> entity = new HttpEntity<>(payload, headers);

            response = restTemplate.exchange(httpPath, method, entity, String.class);
        } catch (Exception ex) {
            world.add(ex);
            log.error(ex.getLocalizedMessage(), ex);
        }
    }

    @When("The client receives a response from TopoSearchController")
    public void theClientReceivesAResponseFromTopoSearchController() {
        assert(response != null);
    }

    @Then("The client receives a TopoSearchController response status code of {int}")
    public void theClientReceivesATopoSearchControllerResponseStatusCodeOf(int statusCode) {
        log.info("response status code: " + response.getStatusCode());
        assertEquals(statusCode, response.getStatusCode().value());
    }

    @Then("The TopoSearchController response is a valid list of EdgePort objects")
    public void theTopoSearchControllerResponseIsAValidListOfEdgePortObjects() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        assertNotNull(response.getBody());
        String payload = response.getBody();

        EdgePort[] entries = mapper.readValue(payload, EdgePort[].class);
        List<EdgePort> list = Arrays.asList(entries);

        assertNotNull(list);
        assert !list.isEmpty();
    }
}
