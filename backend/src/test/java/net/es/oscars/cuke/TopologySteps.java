package net.es.oscars.cuke;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.db.DeviceRepository;
import net.es.oscars.topo.db.AdjcyRepository;
import net.es.oscars.topo.db.PortRepository;
import net.es.oscars.topo.db.VersionRepository;
import net.es.oscars.topo.pop.TopoPopulator;
import net.es.oscars.topo.svc.TopoService;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Slf4j
@Category({UnitTests.class})
public class TopologySteps extends CucumberSteps {
    @Autowired
    private TopoService topoService;

    @Autowired
    private DeviceRepository deviceRepo;
    @Autowired
    private AdjcyRepository adjcyRepo;
    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private CucumberWorld world;

    @Autowired
    private PortRepository portRepo;

    @Autowired
    private TopoPopulator topoPopulator;

    private Topology t;

    @Given("^I load topology from \"([^\"]*)\" and \"([^\"]*)\"$")
    public void i_load_topology_from_and(String arg1, String arg2) throws Throwable {
        this.t = topoPopulator.loadTopology(arg1, arg2);
    }

    @Given("^I update the topology URN map after import$")
    public void update_topology_map() throws Throwable {

        topoService.updateInMemoryTopo();
        world.topoBaseline = topoService.getTopoUrnMap();
        // log.info(world.topoBaseline.toString());
    }


    @Given("^I clear the topology$")
    public void clear_topo() throws Throwable {

        log.info("clearing adjacencies");
        adjcyRepo.deleteAll();
        adjcyRepo.flush();

        log.info("clearing devices");
        deviceRepo.deleteAll();
        deviceRepo.flush();

        log.info("clearing ports");
        portRepo.deleteAll();
        portRepo.flush();

        log.info("clearing versions");
        versionRepo.deleteAll();
        versionRepo.flush();
        world.topoBaseline = new HashMap<>();
    }

    @Then("^the current topology is empty$")
    public void the_current_topology_is_empty() throws Throwable {
        Topology c = topoService.currentTopology();
        assert c.getDevices().values().size() == 0;
        assert c.getPorts().values().size() == 0;
        assert c.getAdjcies().size() == 0;
    }


    @When("^I merge the new topology$")
    public void i_merge_the_new_topology() throws Throwable {
        topoService.bumpVersion();
        topoPopulator.replaceDbTopology(this.t);
    }

}