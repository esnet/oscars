package net.es.oscars.cuke;

import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.EsdbProperties;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.dto.esdb.gql.GraphqlEsdbOrganization;
import net.es.oscars.dto.esdb.gql.GraphqlEsdbOrganizationType;
import net.es.oscars.esdb.ESDBProxy;

import java.util.List;

import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

import static net.es.oscars.resv.svc.validators.ConnServiceProjectIdValidate.getOrgTypeUiid;

@Slf4j
@Category({UnitTests.class})
public class EsdbOrgSteps extends CucumberSteps {

    @Autowired
    private ESDBProxy esdbProxy;

    @Autowired
    private EsdbProperties esdbProps;

    @Autowired
    private CucumberWorld world;

    @Value("${nso.mockPort}")
    private int mockPort = 50123;


    private List<GraphqlEsdbOrganization> esdbOrganizationList;
    private List<GraphqlEsdbOrganizationType> esdbOrganizationTypeList;

    @Before("@EsdbOrgSteps")
    public void before() throws IOException {
        // Override with our mock server URL and port
        esdbProps.setUri("http://localhost:" + mockPort + "/esdb_api/v1");
        esdbProps.setGraphqlUri("http://localhost:" + mockPort + "/esdb_api/graphql");
        world.getExceptions().clear();
    }


    @When("I retrieve the organization types from ESDB")
    public void retrieveOrganizationsTypes() throws IOException {
        try {
            this.esdbOrganizationTypeList = esdbProxy.gqlOrganizationTypeList();
        } catch (Exception e) {
            world.add(e);
        }
    }


    @When("I retrieve organizations from ESDB with {string} type")
    public void retrieveOrganizationTypes(String orgTypeName){
        try {
            System.out.println(orgTypeName);
            String orgType = getOrgTypeUiid(orgTypeName, esdbProxy);
            this.esdbOrganizationList = esdbProxy.gqlOrganizationList(orgType);
        } catch (Exception e) {
            world.add(e);
        }
    }

    @Then("There is an organization type with the name {string}")
    public void orgTypeExists(String orgTypeName) {
        assert this.esdbOrganizationTypeList != null;
        String orgType = getOrgTypeUiid(orgTypeName, esdbProxy);
        assert orgType != null;
    }

    @Then("The number of organizations was greater than {int}")
    public void orgListSize(int num) {
        assert this.esdbOrganizationList != null;
        assert this.esdbOrganizationList.size() > num;
    }

    @Then("The number of organization types was greater than {int}")
    public void orgTypeListSize(int num) {
        assert this.esdbOrganizationTypeList != null;
        assert this.esdbOrganizationTypeList.size() > num;
    }
}
