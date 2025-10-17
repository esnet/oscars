package net.es.oscars.cuke;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.EsdbProperties;
import net.es.oscars.app.props.ValidationProperties;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.dto.esdb.gql.GraphqlEsdbOrganization;
import net.es.oscars.dto.esdb.gql.GraphqlEsdbOrganizationType;
import net.es.oscars.esdb.ESDBProxy;
import net.es.oscars.resv.svc.validators.ConnServiceProjectIdValidate;
import net.es.oscars.web.simple.SimpleConnection;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.validation.Errors;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Category({UnitTests.class})
public class ProjectIdSteps extends CucumberSteps {

    @Autowired
    private ESDBProxy esdbProxy;

    @Autowired
    private ValidationProperties validationProperties;

    @Autowired
    private EsdbProperties esdbProps;

    @Autowired
    private CucumberWorld world;

    @Value("${nso.mockPort}")
    private int mockPort = 50123;
    private boolean orcIdValid = false;

    private SimpleConnection simpleConnection;
    private Errors validationErrors;

    private List<GraphqlEsdbOrganization> esdbOrganizationList;
    private List<GraphqlEsdbOrganizationType> esdbOrganizationTypeList;

    @Before("@ProjectIdSteps")
    public void before() throws IOException {
        // Override with our mock server URL and port
        esdbProps.setUri("http://localhost:" + mockPort + "/esdb_api/v1");
        esdbProps.setGraphqlUri("http://localhost:" + mockPort + "/esdb_api/graphql");

        esdbProxy.setEsdbProperties(esdbProps);
    }

    @When("I perform orcid validation on {string}")
    public void performOrcidValidation(String orcid) {
        orcIdValid = ConnServiceProjectIdValidate.validateOrcId(orcid);
    }

    @Then("the orcid validation succeeded")
    public void theOrcidValidationSucceeded() {
        assert orcIdValid;
    }

    @Then("the orcid validation failed")
    public void theOrcidValidationFailed() {
        assert !orcIdValid;
    }

    @When("I set the project id validation mode to {string}")
    public void iSetTheProjectIdValidationModeTo(String mode) {
        try {
            ValidationProperties.ProjectIdValidationMode pivm = ValidationProperties.ProjectIdValidationMode.fromString(mode);
            assert pivm != null;
            validationProperties.setProjectIdMode(pivm);
        } catch (Exception e) {
            world.add(e);
        }
    }



    @When("I load a SimpleConnection from {string}")
    public void iLoadASimpleConnectionFrom(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream stream = new ClassPathResource(path).getInputStream();
        this.simpleConnection = mapper.readValue(stream, SimpleConnection.class);

    }

    @When("I validate projectIds for the SimpleConnection")
    public void iValidateProjectIdsForTheSimpleConnection() {
        ConnServiceProjectIdValidate connServiceProjectIdValidate = new ConnServiceProjectIdValidate(validationProperties, esdbProxy);
        this.validationErrors = connServiceProjectIdValidate.validateObject(simpleConnection);
    }

    @Then("the projectIds validation succeeded")
    public void theProjectIdsValidationSucceeded() {
        assert !validationErrors.hasErrors();

    }

    @Then("the projectIds validation failed")
    public void theProjectIdsValidationFailed() {
        assert validationErrors.hasErrors();
    }

}
