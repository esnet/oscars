package net.es.oscars.cuke;

import io.cucumber.junit.Cucumber;
import io.cucumber.spring.CucumberContextConfiguration;
import net.es.oscars.AbstractBackendTest;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberContextConfiguration
public class CucumberTest extends AbstractBackendTest {

}