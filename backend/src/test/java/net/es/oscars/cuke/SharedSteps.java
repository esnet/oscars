package net.es.oscars.cuke;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.resv.db.*;
import net.es.oscars.topo.db.DeviceRepository;
import net.es.oscars.topo.db.AdjcyRepository;
import net.es.oscars.topo.db.PortRepository;
import net.es.oscars.topo.db.VersionRepository;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@Slf4j
@Category({UnitTests.class})
public class SharedSteps {
    @Autowired
    private CucumberWorld world;

    @Autowired
    private ScheduleRepository schedRepo;
    @Autowired
    private PipeRepository pipeRepo;
    @Autowired
    private FixtureRepository fixtureRepo;
    @Autowired
    private JunctionRepository junctionRepo;
    @Autowired
    private VlanRepository vlanRepo;
    @Autowired
    private DesignRepository designRepo;
    @Autowired
    private DeviceRepository deviceRepo;
    @Autowired
    private AdjcyRepository adjcyRepo;
    @Autowired
    private PortRepository portRepo;
    @Autowired
    private VersionRepository versionRepo;


    @Then("^the \"([^\"]*)\" repository has (\\d+) entries$")
    public void the_repository_has_entries(String repoName, int num) throws Throwable {
        if (repoName.equals("junction")) {
        }
        if (repoName.equals("pipe")) {
            assert pipeRepo.findAll().size() == num;
        }
        if (repoName.equals("schedule")) {
            assert schedRepo.findAll().size() == num;
        }
        if (repoName.equals("fixture")) {
            assert fixtureRepo.findAll().size() == num;
        }
        if (repoName.equals("vlanId")) {
            assert vlanRepo.findAll().size() == num;
        }
        if (repoName.equals("design")) {
            assert designRepo.findAll().size() == num;
        }
        if (repoName.equals("adjacency")) {
            assert adjcyRepo.findAll().size() == num;
        }
        if (repoName.equals("device")) {
            assert deviceRepo.findAll().size() == num;
        }
        if (repoName.equals("port")) {
            assert portRepo.findAll().size() == num;
        }
        if (repoName.equals("version")) {
            assert versionRepo.findAll().size() == num;
        }

    }

    @When("^I clear the \"([^\"]*)\" repository$")
    public void i_clear_the_repo(String repoName) throws Throwable {
        if (repoName.equals("junction")) {
            junctionRepo.deleteAll();
        }
        if (repoName.equals("pipe")) {
            pipeRepo.deleteAll();
        }
        if (repoName.equals("schedule")) {
            schedRepo.deleteAll();
        }
        if (repoName.equals("fixture")) {
            fixtureRepo.deleteAll();
        }
        if (repoName.equals("vlanId")) {
            vlanRepo.deleteAll();
        }
        if (repoName.equals("design")) {
            designRepo.deleteAll();
        }
        if (repoName.equals("adjacency")) {
            adjcyRepo.deleteAll();
        }
        if (repoName.equals("device")) {
            deviceRepo.deleteAll();
        }
        if (repoName.equals("port")) {
            portRepo.deleteAll();
        }
        if (repoName.equals("version")) {
            versionRepo.deleteAll();
        }


    }



    @Given("^I have initialized the world$")
    public void i_have_initialized_the_world() throws Throwable {
        this.world.getExceptions().clear();
        this.world.topoBaseline = new HashMap<>();
        this.world.reservedVlans = new ArrayList<>();
        this.world.bwMaps = new HashMap<>();
        this.world.bwBaseline = new HashMap<>();
    }

    @Given("^The world is expecting an exception$")
    public void the_world_is_expecting_an_exception() throws Throwable {
        this.world.expectException();
    }

    @Then("^I did not receive an exception$")
    public void i_did_not_receive_an_exception() throws Throwable {
        for (Exception ex: this.world.getExceptions()) {
            log.error(ex.getMessage(), ex);
        }
        assertThat(this.world.getExceptions().isEmpty(), is(true));
    }

    @Then("^I did receive an exception$")
    public void i_did_receive_an_exception() throws Throwable {
        assertThat(this.world.getExceptions().isEmpty(), is(false));
    }


}

