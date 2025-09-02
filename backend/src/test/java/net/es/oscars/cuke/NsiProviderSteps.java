package net.es.oscars.cuke;

import static net.es.oscars.app.util.PrettyPrinter.prettyLog;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

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

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.nsi.svc.NsiAsyncQueue;
import net.es.oscars.nsi.svc.NsiConnectionEventService;
import net.es.oscars.nsi.svc.NsiService;
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
    private NsiAsyncQueue queue;

    private ResponseEntity<String> response;

    @Before("@NsiProviderSteps")
    public void before() throws Exception {
        
        try {
            startup.setInStartup(false);
            queue.queue.clear();
            topoService.clear();
            nsiService.setErrorCount(0);
            loadTopology();
            releaseAllConnections();
        } catch (Exception e) {
            world.add(e);
            log.error("NsiProviderSteps Error - {}", e);
        }
    }

    @Given("The NSI connection is queued for asynchronous reservation while not including a projectId")
    public void the_nsi_connection_is_queued_for_asynchronous_reservation_while_not_including_a_project_id() throws Throwable {
        try {
            queueNsiConnection();
        } catch (Exception e) {
            world.add(e);
            log.error("NsiProviderSteps Error - {}", e);
        }
    }

    @Given("The NSI connection is queued for asynchronous reservation while including a projectId")
    public void the_NSI_connection_is_queued_for_asynchronous_reservation_while_including_a_projectId() {
        try {
            queueNsiConnection(true, "ABCD-1234-EFGH-5678");
        } catch (Exception e) {
            world.add(e);
            log.error("NsiProviderSteps Error - {}", e);
        }
    }

    @Given("The NSI connection is queued for asynchronous reservation while including a blank projectId")
    public void the_NSI_connection_is_queued_for_asynchronous_reservation_while_including_a_blank_projectId() {
        try {
            queueNsiConnection(true, "");
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

    private void loadTopology() throws Exception {
        String topoPath = "topo/esnet.json";
        Topology t = topoPopulator.loadTopology(topoPath);
        if (t == null) throw new Exception(topoPath + " is not a topology");
        topoService.replaceTopology(t);
    }

    private void queueNsiConnection() throws Exception {
        queueNsiConnection(false, null);
    }

    private void queueNsiConnection(boolean withProjectId, String projectIdValue) throws Exception {
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
    }

    public void releaseAllConnections() {
        connService.getHeld().clear();
    }
}
