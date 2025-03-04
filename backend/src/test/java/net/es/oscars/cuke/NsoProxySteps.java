package net.es.oscars.cuke;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.opentelemetry.api.OpenTelemetry;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.NsoProperties;
import net.es.oscars.app.props.StartupProperties;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.sb.nso.NsoProxy;
import net.es.oscars.sb.nso.rest.LiveStatusOutput;
import net.es.oscars.sb.nso.rest.LiveStatusRequest;
import org.junit.experimental.categories.Category;

import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@Category({UnitTests.class})
public class NsoProxySteps extends CucumberSteps {

    @Autowired
    private CucumberWorld world;
    @Autowired
    private NsoProxy proxy;
    @Autowired
    RestTemplateBuilder restTemplateBuilder;
    @Mock
    RestTemplate restTemplate;

    private NsoProperties nsoProps;

    String liveStatus = "";
    @Before("@NsoProxySteps")
    public void before() throws Exception{
        log.info("---------- NsoProxySteps.java before");

        nsoProps = new NsoProperties();
        nsoProps.setUri("http://localhost:8080");
        nsoProps.setUsername("test");
        nsoProps.setPassword("test");

        restTemplateBuilder = mock(RestTemplateBuilder.class);
        restTemplate = mock(RestTemplate.class);

        String filePath = "http/nso.esnet-status.nokia-show.post.response.json";
        log.info("loading " + filePath);
        ObjectMapper mapper = new ObjectMapper();
        var jsonFile = new ClassPathResource(filePath).getFile();

        LiveStatusOutput mockOutput = mapper.readValue(jsonFile, LiveStatusOutput.class);
        LiveStatusRequest liveStatusRequest = new LiveStatusRequest("loc1-cr6", "service fdb-info");

        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        when(
                restTemplate.postForObject(
                        "http://localhost:8080/restconf/data/esnet-status:esnet-status/nokia-show",
                        liveStatusRequest,
                        LiveStatusOutput.class
                )
        ).thenReturn(mockOutput);


    }
    @After("@NsoProxySteps")
    public void after() {
    }

    @Given("^I instantiate the proxy$")
    public void i_instantiate_the_proxy() throws Throwable {
        StartupProperties startupProperties = new StartupProperties();
        OpenTelemetry openTelemetry = OpenTelemetry.noop();

        log.info("nso props, uri is " + nsoProps.getUri());

        proxy = new NsoProxy(
                nsoProps,
                startupProperties,
                restTemplateBuilder,
                openTelemetry
        );
    }

    @When("^The getLiveStatusShow method is called with device \"([^\"]*)\" and arguments \"([^\"]*)\"$")
    public void theGetLiveStatusShowMethodIsCalledWithDeviceAndArguments(String arg0, String arg1) {
        LiveStatusRequest liveStatusRequest = new LiveStatusRequest(arg0, arg1);
        liveStatus = proxy.getLiveStatusShow(liveStatusRequest);
    }

    @Then("the resulting ESNet status report matches the ALU NED format")
    public void theResultingESNetStatusReportMatchesTheALUNEDFormat() {
        assert !liveStatus.isEmpty();
    }

}
