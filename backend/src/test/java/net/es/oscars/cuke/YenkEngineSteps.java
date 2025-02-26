package net.es.oscars.cuke;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PCEException;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.model.Interval;
import net.es.oscars.pce.PceService;
import net.es.oscars.pce.YenkEngine;
import net.es.oscars.pce.beans.PathConstraint;
import net.es.oscars.resv.ent.VlanJunction;
import net.es.oscars.resv.ent.VlanPipe;
import net.es.oscars.resv.svc.ResvService;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.pop.TopoPopulator;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.oscars.web.beans.PceRequest;
import net.es.oscars.web.beans.PceResponse;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

@Slf4j
@Category({UnitTests.class})
public class YenkEngineSteps extends CucumberSteps {
    @Autowired
    private CucumberWorld world;
    @Autowired
    private TopologyStore topologyStore;
    @Autowired
    private TopoPopulator topoPopulator;

    @Autowired
    private YenkEngine yenkEngine;
    @Autowired
    ResvService resvService;

    PceResponse pceResponse;

    @Given("^I instantiate the engine by loading topology from \"([^\"]*)\"$")
    public void i_instantiate_the_engine(String arg1) throws Throwable {
        Topology topology = topoPopulator.loadTopology(arg1);

        if (topology == null) throw new Exception(arg1 + " is not a topology");

        topologyStore.replaceTopology(topology);
        yenkEngine = new YenkEngine(topologyStore);

    }

    /**
     * The YenkEngine class implements the Engine interface.
     *  - The Engine.calculatePaths() method is implemented,
     *    and returns a validated PceResponse object.
     * @throws Throwable Can throw an exception.
     */
    @When("^The paths are calculated from path \"([^\"]*)\" with device urn \"([^\"]*)\" with port \"([^\"]*)\" to path \"([^\"]*)\" with device urn \"([^\"]*)\" with port \"([^\"]*)\"$")
    public void the_paths_are_calculated(String arg1, String arg2, String arg3, String arg4, String arg5, String arg6) throws Throwable {
        // For testing, manually load from JSON file source

        String pathA = arg1;
        String deviceA = arg2;
        String portA = arg3;
        String pathB = arg4;
        String deviceB = arg5;
        String portB = arg6;

        PceRequest request = new PceRequest();
        Interval interval = new Interval();
        Instant start = Instant.now();
        Instant end = start.plusMillis(1000);
        interval.setBeginning(start);
        interval.setEnding(end);
        ArrayList<String> include = new ArrayList<>();


        if (pathA.isEmpty()) {
            throw new Exception(pathA + " is not a path");
        }

        if (deviceA.isEmpty()) {
            throw new Exception(deviceA + " is not a urn");
        }

        if (portA.isEmpty()) {
            throw new Exception(portA + " is not a port");
        }

        if (pathB.isEmpty()) {
            throw new Exception(pathB + " is not a path");
        }

        if (deviceB.isEmpty()) {
            throw new Exception(deviceB + " is not a urn");
        }

        if (portB.isEmpty()) {
            throw new Exception(portB + " is not a port");
        }

        // Set up the test request
        request.setA(pathA);
        request.setZ(pathB); // MUST be different from A

        include.add(deviceA);
        include.add(portA);
        include.add(portB);
        include.add(deviceB);

        request.setAzBw(1000);
        request.setZaBw(1000);
        request.setInterval(interval);
        request.setInclude(include);
        request.setExclude(new HashSet<>());

        if (request.getA().equals(request.getZ())) {
            throw new PCEException("invalid path request: " + request.getA() + " is the same as Z " + request.getZ());
        }


        VlanJunction aj = VlanJunction.builder()
                .refId(request.getA())
                .deviceUrn(request.getA())
                .build();
        VlanJunction zj = VlanJunction.builder()
                .refId(request.getZ())
                .deviceUrn(request.getZ())
                .build();

        VlanPipe bwPipe = VlanPipe.builder()
                .a(aj)
                .z(zj)
                .protect(false)
                .azBandwidth(request.getAzBw())
                .zaBandwidth(request.getZaBw()).build();

        Map<String, Integer> availIngressBw = resvService.availableIngBws(request.getInterval());
        Map<String, Integer> availEgressBw = resvService.availableEgBws(request.getInterval());

        PathConstraint constraint = PathConstraint.builder()
                .ero(request.getInclude())
//                .exclude(request.getExclude())
                .build();

        pceResponse = yenkEngine.calculatePaths(bwPipe, availIngressBw, availEgressBw, constraint);
    }

    @Then("I did receive a PceResponse")
    public void i_did_receive_a_pce_response() {
        assert pceResponse != null;
        assert pceResponse.getShortest() != null;
        assert pceResponse.getFits() != null;

        assert !pceResponse.getShortest().getAzEro().isEmpty();
        assert !pceResponse.getShortest().getZaEro().isEmpty();
        assert !pceResponse.getShortest().getAzEro().getFirst().getUrn().isEmpty();
        assert !pceResponse.getShortest().getZaEro().getFirst().getUrn().isEmpty();
        assert pceResponse.getShortest().getAzEro().getFirst() == pceResponse.getShortest().getZaEro().getLast();
    }
}
