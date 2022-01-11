package net.es.oscars.pss.cuke;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import io.cucumber.spring.CucumberContextConfiguration;
import net.es.oscars.pss.AbstractPssTest;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberContextConfiguration
@CucumberOptions(tags = "@none")
public class CucumberTest extends AbstractPssTest {

}

