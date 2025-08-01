package net.es.oscars.cuke;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.model.L2VPN;
import net.es.oscars.resv.enums.ConnectionMode;
import net.es.oscars.resv.svc.L2VPNService;
import net.es.oscars.topo.beans.TopoException;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.pop.ConsistencyException;
import net.es.oscars.topo.pop.TopoPopulator;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.oscars.web.beans.ConnException;
import net.es.oscars.web.beans.ConnectionFilter;
import net.es.oscars.web.beans.v2.L2VPNList;
import net.es.oscars.web.beans.v2.ValidationResponse;
import net.es.topo.common.devel.DevelUtils;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Category({UnitTests.class})
public class L2vpnSteps extends CucumberSteps {
    private L2VPN request;
    private ValidationResponse validationResponse;
    private boolean gotSubmitException;
    private boolean gotListException;
    private L2VPNList l2VPNList;

    @Autowired
    private TopoPopulator topoPopulator;

    @Autowired
    private TopologyStore topologyStore;

    @Autowired
    private L2VPNService l2VPNService;

    @Autowired
    private ObjectMapper objectMapper;


    @Before("@L2vpnSteps")
    public void prepare()  {
        try {
            Topology t = topoPopulator.loadTopology("topo/esnet.json");
            topologyStore.replaceTopology(t);
        } catch (TopoException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Given("I have loaded the {string} L2VPN request")
    public void iHaveLoadedTheL2VPNRequest(String path) throws IOException {
        var jsonFile = new ClassPathResource(path).getFile();

        request = objectMapper.readValue(
                jsonFile,
                L2VPN.class
        );
        // modify the schedule
        request.getSchedule().setBeginning(Instant.now().plus(1, ChronoUnit.MINUTES));
        request.getSchedule().setEnding(Instant.now().plus(30, ChronoUnit.MINUTES));
    }

    @When("I validate the new L2VPN request")
    public void iValidateTheL2VPNRequest() {
        validationResponse = l2VPNService.validate(request, ConnectionMode.NEW);
    }


    @When("I submit the L2VPN request")
    public void iSubmitTheL2VPNRequest() {
        gotSubmitException = false;
        try {
            l2VPNService.createOrReplace(request);
        } catch (ConnException | ConsistencyException e) {
            gotSubmitException = true;
        }
    }

    @Then("the L2VPN request validates successfully")
    public void theL2VPNRequestValidatesSuccessfully() {
        assert validationResponse.isValid();
    }

    @Then("the L2VPN request does not validate successfully")
    public void theL2VPNRequestDoesNotValidateSuccessfully() {
        assert !validationResponse.isValid();
    }

    @Then("the L2VPN request did not throw an exception")
    public void theL2VPNRequestNoException() {
        assert !gotSubmitException;
    }

    @Then("the L2VPN request did throw an exception")
    public void theL2VPNRequestDidThrowAnException() {
        assert gotSubmitException;
    }

    @Given("I have cleared all L2VPNs")
    public void iHaveClearedAllLV2PNs() {
        l2VPNService.clear();
    }

    @When("I list all L2VPNs")
    public void iListAllL2VPNs() {
        gotListException = false;
        ConnectionFilter filter = ConnectionFilter.builder()
                .sizePerPage(-1)
                .build();
        try {
            l2VPNList = l2VPNService.list(filter);
        } catch (ConnException | ConsistencyException e) {
            gotListException = true;
        }
    }


    @Then("the L2VPN list size is {int}")
    public void theLVPNListSizeIs(int arg) {
        assert l2VPNList.getL2vpns().size() == arg;
    }

    @Then("the L2VPN list contains {string}")
    public void theL2VPNListContains(String arg1) {
        AtomicBoolean found = new AtomicBoolean(false);
        l2VPNList.getL2vpns().forEach(l2vpn -> {
            if (l2vpn.getName().equals(arg1)) {
                found.set(true);
            }
        });
        assert found.get();
    }

    @Then("the L2VPN list did not throw an exception")
    public void theL2VPNListDidNotThrowAnException() {
        assert !gotListException;
    }
}
