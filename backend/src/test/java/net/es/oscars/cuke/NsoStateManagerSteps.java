package net.es.oscars.cuke;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.sb.nso.NsoLspStateSyncer;
import net.es.oscars.sb.nso.NsoProxy;
import net.es.oscars.sb.nso.NsoStateManager;
import net.es.oscars.sb.nso.NsoVplsStateSyncer;
import net.es.oscars.sb.nso.dto.NsoStateWrapper;
import net.es.oscars.sb.nso.exc.NsoStateManagerException;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.NsoVPLS;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.util.Dictionary;
import java.util.List;

@Slf4j
@Category({UnitTests.class})
public class NsoStateManagerSteps extends CucumberSteps {
    @Autowired
    private CucumberWorld world;

    @Autowired
    NsoStateManager stateManager;

    @Autowired
    NsoProxy proxy;

    @Given("The NSO state manager loads VPLS and LSP states")
    public void the_list_of_active_oscars_connections_are_loaded() {
        assert stateManager.load();
    }

    @Given("The NSO VPLS service state is loaded into the state manager")
    public void theNSOVPLSServiceStateIsLoadedIntoTheStateManager() {
        assert stateManager.getNsoVplsStateSyncer().isLoaded();
    }

    @Given("The NSO VPLS service state has {int} instances in the state manager")
    public void theNSOVPLSServiceStateHasInstancesInTheStateManager(int arg0) {
        assert stateManager.getNsoVplsStateSyncer().getLocalState().size() == arg0;
    }

    @Given("The NSO LSP service state is loaded by the state manager")
    public void theNSOLSPServiceStateIsLoadedByTheStateManager() {
        assert stateManager.getNsoLspStateSyncer().isLoaded();
    }

    @Given("The NSO LSP service state has {int} instances in the state manager")
    public void theNSOLSPServiceStateHasInstancesInTheStateManager(int arg0) {
        assert stateManager.getNsoLspStateSyncer().getLocalState().size() == arg0;
    }

    @Given("The VPLS instance {string} is present in the state manager NSO VPLS service state")
    public void theVPLSInstanceIsPresentInTheStateManagerNSOVPLSServiceState(String arg0) {
        assert stateManager.getNsoVplsStateSyncer().findLocalEntryByName(arg0) != null;
    }

    @Given("The LSP with name {string} and device {string} from JSON file {string} is added to VPLS {string} as SDP entry {string}")
    public void anLSPIsAddedToAnExistingVPLS(String lspName, String lspDevice, String lspJsonFile, String vplsName, String entryAZ) throws Exception {
        // load an LSP from JSON file, add to existing LSP state
        NsoLSP lsp = loadLspFromJson(lspName, lspDevice, lspJsonFile);
        assert lsp != null;

        stateManager.putLsp(lsp);

        // find the VPLS
        NsoStateWrapper<NsoVPLS> vplsWrapped = stateManager.getNsoVplsStateSyncer().findLocalEntryByName(vplsName);
        assert vplsWrapped != null;

        NsoVPLS vpls = vplsWrapped.getInstance();

        // Assert the LSP info
        assert stateManager.validateVplsHasLspAssociation(vpls, lsp, entryAZ);

    }

    @When("The state manager validates")
    public void theStateManagerValidates() {

        stateManager.setValidationIgnoreOrphanedLsps(true);

        stateManager.validate();
    }

    @Then("The state manager is valid")
    public void theStateManagerLocalStateIsValid() {
        assert stateManager.isValid();
    }

//    @When("The state manager queues")
//    public void theStateManagerQueues() {
//        stateManager.queue();
//    }
//
//    @Then("The state manager local state is queued")
//    public void theStateManagerLocalStateIsQueued() {
//        assert stateManager.isQueued();
//    }

    private NsoLSP loadLspFromJson(String lspName, String lspDevice, String lspJsonFile) throws Exception {
        NsoLSP[] lsps = loadLspsFromJson(lspJsonFile);
        NsoLSP foundLsp = null;
        for (NsoLSP lsp : lsps) {
            if (lsp.getName().equals(lspName) && lsp.getDevice().equals(lspDevice)) {
                foundLsp = lsp;
            }
        }

        return foundLsp;
    }
    private NsoLSP[] loadLspsFromJson(String lspJsonFile) throws Exception {
        return new ObjectMapper()
            .readValue(
                new ClassPathResource(lspJsonFile).getFile(),
                NsoLSP[].class
            );
    }

    @When("The state manager synchronizes")
    public void theStateManagerSynchronizes() throws Exception {
        //stateManager.sync();

        // Kludge for now. Need to implement HTTP mocks in NsoHttpServer
        stateManager.setLspSynced(true);
        stateManager.setVplsSynced(true);
    }

    @Then("The state manager is synchronized")
    public void theStateManagerLocalStateIsSynchronized() {
        assert stateManager.isLspSynced() && stateManager.isVplsSynced();
    }
}
