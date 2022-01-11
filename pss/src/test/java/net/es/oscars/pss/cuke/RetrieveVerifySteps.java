package net.es.oscars.pss.cuke;

import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.dto.topo.DeviceModel;
import net.es.oscars.pss.beans.VerifyException;
import net.es.oscars.pss.ctg.UnitTests;
import net.es.oscars.pss.svc.ConfigCollector;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;



@Category({UnitTests.class})
@Slf4j
public class RetrieveVerifySteps extends CucumberSteps {
    @Autowired
    private CucumberWorld world;

    @Autowired
    private ConfigCollector verifier;

    private String config;

    @When("^I retrieve config for \"([^\"]*)\" model \"([^\"]*)\"$")
    public void i_retrieve_config_for_model(String device, DeviceModel model, String profile) throws Throwable {
        try {
            config = verifier.collectConfig(device, model, profile);
//            log.info(config);

        } catch (VerifyException ex) {
            world.add(ex);
            throw ex;

        }
    }


}

