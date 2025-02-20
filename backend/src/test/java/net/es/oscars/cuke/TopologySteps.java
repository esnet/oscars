package net.es.oscars.cuke;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.topo.beans.TopoUrn;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.pop.TopoPopulator;
import net.es.oscars.topo.svc.TopologyStore;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Slf4j
@Category({UnitTests.class})
public class TopologySteps extends CucumberSteps {
    @Autowired
    private TopologyStore topoService;

    @Autowired
    private CucumberWorld world;

    @Autowired
    private TopoPopulator topoPopulator;

    private Topology t;

    @Given("^I load topology from \"([^\"]*)\"$")
    public void i_load_topology_from_and(String arg1) throws Throwable {
        log.info("loading topology from " + arg1);
        t = topoPopulator.loadTopology(arg1);
        if (t == null) throw new Exception(arg1 + " is not a topology");
        topoService.replaceTopology(t);
    }

    @Given("^the current topology is not empty$")
    public void the_current_topology_is_not_empty() throws Throwable {
        assert !(topoService.isTopologyEmpty());
        assert topoService.getCurrentTopology() != null;
    }

    @Given("^I update the topology URN map after import$")
    public void update_topology_map() throws Throwable {
        world.topoBaseline = topoService.getTopoUrnMap();
    }


    @Given("^I clear the topology$")
    public void clear_topo() throws Throwable {
        log.info("clear topology");
        topoService.clear();
        world.topoBaseline = new HashMap<>();
    }

    @Then("^the current topology is empty$")
    public void the_current_topology_is_empty() throws Throwable {

        assert topoService.isTopologyEmpty();
    }

}