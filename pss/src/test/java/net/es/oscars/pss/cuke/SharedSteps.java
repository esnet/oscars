package net.es.oscars.pss.cuke;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import net.es.oscars.pss.beans.*;
import net.es.oscars.pss.ctg.UnitTests;
import net.es.oscars.pss.prop.PssProps;
import net.es.oscars.pss.prop.RancidProps;
import net.es.oscars.pss.prop.UrnMappingProps;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Category({UnitTests.class})
public class SharedSteps {
    @Autowired
    private CucumberWorld world;

    @Autowired
    private PssProps pssProps;


    @Autowired
    private PssTestConfig pssTestConfig;

    @Given("^I create profile \"([^\"]*)\"$")
    public void i_create_profile(String name) throws Throwable {
        UrnMappingProps urnMappingProps = UrnMappingProps.builder()
                .method(UrnMappingMethod.IDENTITY)
                .match(new ArrayList<>())
                .suffix("")
                .build();

        RancidProps rancid = RancidProps.builder()
                .cloginrc("")
                .perform(false)
                .dir("")
                .host("")
                .identityFile("")
                .username("")
                .sshOptions(new ArrayList<>())
                .build();

        PssProfile pssProfile = PssProfile.builder()
                .profile(name)
                .rancid(rancid)
                .urnMapping(urnMappingProps)
                .build();
        pssProps.getProfiles().add(pssProfile);
    }

    @Given("^I clear all profiles")
    public void i_clear_all_profiles() throws Throwable {
        pssProps.getProfiles().clear();
    }

    @Given("^I have initialized the world$")
    public void i_have_initialized_the_world() throws Throwable {
        world.getExceptions().clear();
    }

    @Given("^The world is expecting an exception$")
    public void the_world_is_expecting_an_exception() throws Throwable {
        world.expectException();
    }

    @Then("^I did not receive an exception$")
    public void i_did_not_receive_an_exception() throws Throwable {
        assertThat(world.getExceptions().isEmpty(), is(true));
    }

    @Then("^I did receive an exception$")
    public void i_did_receive_an_exception() throws Throwable {
        assertThat(world.getExceptions().isEmpty(), is(false));
    }
    @Then("^I set rancid perform to true on profile \"([^\"]*)\"$")
    public void i_set_the_rancid_perform_property_to_true_on_profile(String profile) throws Throwable {
        PssProfile pssProfile = PssProfile.find(pssProps, profile);
        pssProfile.getRancid().setPerform(true);

    }

    @Then("^I set rancid host to \"([^\"]*)\" on profile \"([^\"]*)\"$")
    public void i_set_the_rancid_host_to_on_profile(String host, String profile) throws Throwable {
        PssProfile pssProfile = PssProfile.find(pssProps, profile);
        pssProfile.getRancid().setHost(host);
    }


    @Then("^I set rancid dir to \"([^\"]*)\" on profile \"([^\"]*)\"$")
    public void i_set_rancid_dir_to_on_profile(String dir, String profile) throws Throwable {
        PssProfile pssProfile = PssProfile.find(pssProps, profile);
        pssProfile.getRancid().setDir(dir);
    }

    @Then("^I set rancid username to \"([^\"]*)\" on profile \"([^\"]*)\"$")
    public void i_set_rancid_username_to_on_profile(String username, String profile) throws Throwable {
        PssProfile pssProfile = PssProfile.find(pssProps, profile);
        pssProfile.getRancid().setUsername(username);
    }

    @Then("^I set rancid cloginrc to \"([^\"]*)\" on profile \"([^\"]*)\"$")
    public void i_set_rancid_cloginrc_to_on_profile(String cloginrc, String profile) throws Throwable {
        PssProfile pssProfile = PssProfile.find(pssProps, profile);
        pssProfile.getRancid().setCloginrc(cloginrc);
    }







    @Then("^I set the test specification directory to \"([^\"]*)\"$")
    public void i_set_the_test_specification_directory_to(String directory) throws Throwable {
        pssTestConfig.setCaseDirectory(directory);
    }

    @Given("^I have warned the user this is a live test$")
    public void i_have_warned_the_user_this_is_a_live_test() throws Throwable {
        System.out.println("==============================================================================");
        System.out.println("Ready to run control plane tests! These WILL attempt to contact routers.");
        System.out.println("Make sure you have configured test.properties correctly. ");
        System.out.println("Starting in 3 seconds. Ctrl-C to abort.");
        System.out.println("==============================================================================");
        Thread.sleep(3000);

    }

}

