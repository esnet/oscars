package net.es.oscars.pss.cuke;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.pss.beans.*;
import net.es.oscars.pss.ctg.UnitTests;
import net.es.oscars.pss.prop.PssProps;
import net.es.oscars.pss.svc.UrnMappingService;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Category({UnitTests.class})
@Slf4j
public class UrnMappingSteps extends CucumberSteps {
    @Autowired
    private CucumberWorld world;

    @Autowired
    private UrnMappingService mappingService;

    @Autowired
    private PssProps properties;

    @Then("^the router address of \"([^\"]*)\" is \"([^\"]*)\" on profile \"([^\"]*)\"$")
    public void the_router_address_of_is(String urn, String addr, String profile) {
        PssProfile pssProfile = PssProfile.find(properties, profile);

        try {
            assertThat(mappingService.getRouterAddress(urn, pssProfile), is(addr));
        } catch (UrnMappingException ex) {
            world.add(ex);
        }
    }


    @When("^I set the suffix \"([^\"]*)\" on profile \"([^\"]*)\"$")
    public void i_set_the_suffix_on_profile(String suffix, String profile) throws Throwable {
        PssProfile pssProfile = PssProfile.find(properties, profile);
        pssProfile.getUrnMapping().setSuffix(suffix);
    }

    @Given("^I added a mapping from \"([^\"]*)\" to \"([^\"]*)\" on profile \"([^\"]*)\"$")
    public void i_added_a_mapping_from_to_on_profile(String urn, String addr, String profile) throws Throwable {
        PssProfile pssProfile = PssProfile.find(properties, profile);
        UrnMappingEntry e = UrnMappingEntry.builder().address(addr).urn(urn).build();
        pssProfile.getUrnMapping().getMatch().add(e);
    }


    @Given("^I have cleared all mappings on profile \"([^\"]*)\"$")
    public void i_have_cleared_all_mappings_on_profile(String profile) throws Throwable {
        PssProfile pssProfile = PssProfile.find(properties, profile);
        pssProfile.getUrnMapping().getMatch().clear();
    }

    @When("^I set the mapping method to \"([^\"]*)\" on profile \"([^\"]*)\"$")
    public void i_set_the_mapping_method_to_on_profile(UrnMappingMethod method, String profile) throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        PssProfile pssProfile = PssProfile.find(properties, profile);
        pssProfile.getUrnMapping().setMethod(method);
    }

    @When("^I ask for the router address of \"([^\"]*)\" on profile \"([^\"]*)\"$")
    public void i_ask_for_the_router_address_of(String urn, String profile) throws Throwable {
        PssProfile pssProfile = PssProfile.find(properties, profile);
        try {
            mappingService.getRouterAddress(urn, pssProfile);
        } catch (UrnMappingException | NoSuchElementException ex) {
            world.add(ex);
        }
    }

}

