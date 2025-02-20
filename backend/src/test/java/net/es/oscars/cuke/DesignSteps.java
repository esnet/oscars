package net.es.oscars.cuke;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.resv.beans.DesignResponse;
import net.es.oscars.resv.ent.Design;
import net.es.oscars.resv.db.*;
import net.es.oscars.resv.svc.DesignService;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.File;
import java.util.Optional;

@Slf4j
@Category({UnitTests.class})
public class DesignSteps extends CucumberSteps {

    @Autowired
    private Jackson2ObjectMapperBuilder builder;

    @Autowired
    private DesignRepository designRepo;

    @Autowired
    private CucumberWorld world;

    @Autowired
    private DesignService designService;

    @Given("^I load a design from \"([^\"]*)\"$")
    public void my_JSON_formatted_design_is_at(String path) {
        ObjectMapper mapper = builder.build();
        try {
            File f = SharedSteps.loadResource(path).getFile();
            world.design = mapper.readValue(f, Design.class);
            // log.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(design));
        } catch (Exception ex) {
            // log.error("caught: "+ex.getMessage());
            // ex.printStackTrace();
            world.add(ex);
        }
    }

    @Then("^I can persist the design")
    public void i_can_persist_the_design() throws Throwable {
        designRepo.save(world.design);
    }

    @When("^I load the design from the repository$")
    public void i_load_the_design() throws Throwable {
        Optional<Design> maybeDesign = designRepo.findByDesignId(world.design.getDesignId());
        assert maybeDesign.isPresent();

        // ObjectMapper mapper = builder.build();
        // log.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(design));
    }


    @Then("^I \"([^\"]*)\" verify the design against baseline$")
    public void i_verify_the_design_against_baseline(String maybe) throws Throwable {
        DesignResponse resp = designService.verifyDesign(world.design);
        // log.info(resp.toString());
        if (maybe.equals("can")) {
            assert resp.isValid();
        } else {
            assert !resp.isValid();
        }
    }



}