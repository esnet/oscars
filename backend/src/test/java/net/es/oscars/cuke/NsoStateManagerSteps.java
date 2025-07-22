package net.es.oscars.cuke;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.sb.nso.*;
import net.es.oscars.sb.nso.dto.NsoStateWrapper;
import net.es.oscars.sb.nso.exc.NsoStateManagerException;
import net.es.oscars.sb.nso.rest.NsoResponseErrorHandler;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.NsoVPLS;
import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

@Slf4j
@Category({UnitTests.class})
public class NsoStateManagerSteps extends CucumberSteps {
    @Autowired
    private CucumberWorld world;

    @Autowired
    NsoStateManager stateManager;

    MockNsoResponseErrorHandler mockErrorHandler = new MockNsoResponseErrorHandler();

    @Before("@NsoStateManager")
    public void before() {
        stateManager.getNsoVplsStateSyncer().clear();
        stateManager.getNsoLspStateSyncer().clear();
    }

    @Given("The NSO state manager loads VPLS and LSP states")
    public void the_state_manager_loads_VPLS_and_LSP_states() throws NsoStateManagerException {
        NsoProxy.setRestErrorHandler(mockErrorHandler);
        NsoProxy.setPatchErrorHandler(mockErrorHandler);
        stateManager.clear();
        assert stateManager.load();
    }

    @Given("The invalid VPLS instances don't exist")
    public void theInvalidVplsInstancesDontExists() {
        List<String> knownInvalidVpls = new ArrayList<>() {};

        knownInvalidVpls.add("DOE-IN");
        knownInvalidVpls.add("DOE-IN-FNE");
        knownInvalidVpls.add("SPaRC-NMS");
        knownInvalidVpls.add("SPaRC");

        // make sure to skip unmanaged / invalid VPLS
        for (String vplsName : knownInvalidVpls) {
            Assert.assertNull(stateManager.getNsoVplsStateSyncer().findLocalEntryByName(vplsName));
            Assert.assertNull(stateManager.getNsoVplsStateSyncer().findRemoteEntryByName(vplsName));
        }
    }

    @Given("The NSO VPLS service state is loaded into the state manager")
    public void theNSOVPLSServiceStateIsLoadedIntoTheStateManager() {
        assert stateManager.getNsoVplsStateSyncer().isLoaded();
    }

    @Given("The NSO VPLS service state has {int} instances in the state manager")
    public void theNSOVPLSServiceStateHasInstancesInTheStateManager(int arg0) {
        System.out.println("vpls instances: "+stateManager.getNsoVplsStateSyncer().getLocalState().size());

        assert stateManager.getNsoVplsStateSyncer().getLocalState().size() == arg0;
    }

    @Given("The NSO LSP service state is loaded by the state manager")
    public void theNSOLSPServiceStateIsLoadedByTheStateManager() {
        assert stateManager.getNsoLspStateSyncer().isLoaded();
    }

    @Given("The NSO LSP service state has {int} instances in the state manager")
    public void theNSOLSPServiceStateHasInstancesInTheStateManager(int arg0) {
        System.out.println("lsp instances: "+stateManager.getNsoLspStateSyncer().getLocalState().size());
        assert stateManager.getNsoLspStateSyncer().getLocalState().size() == arg0;
    }

    @Given("The VPLS instance {string} is present in the state manager NSO VPLS service state")
    public void theVPLSInstanceIsPresentInTheStateManagerNSOVPLSServiceState(String arg0) {
        assert stateManager.getNsoVplsStateSyncer().findLocalEntryByName(arg0) != null;
    }

    @When("The LSP with name {string} and device {string} from JSON file {string} is added")
    public void anLSPIsAdded(String lspName, String lspDevice, String lspJsonFile) throws Exception {
        NsoLSP lsp = loadLspFromJson(lspName, lspDevice, lspJsonFile);
        assert lsp != null;

        stateManager.putLsp(lsp);
    }
    @Given("The LSP with name {string} and device {string} from JSON file {string} is added to VPLS {string} as SDP entry {string}")
    public void anLSPIsAddedToAnExistingVPLS(String lspName, String lspDevice, String lspJsonFile, String vplsName, String entryAZ) throws Exception {

        // load an LSP from JSON file, add to existing LSP state
        anLSPIsAdded(lspName, lspDevice, lspJsonFile);

        // find the VPLS
        NsoStateWrapper<NsoVPLS> vplsWrapped = stateManager.getNsoVplsStateSyncer().findLocalEntryByName(vplsName);
        assert vplsWrapped != null;

        NsoVPLS vpls = vplsWrapped.getInstance();

        // Assert the LSP info
        NsoLSP lsp = loadLspFromJson(lspName, lspDevice, lspJsonFile);
        assert stateManager.validateVplsHasLspAssociation(vpls, lsp, entryAZ);

    }

    @Given("I had changed LSP instance in the state manager with name {string} and device {string} to name {string} and device {string} from {string}")
    public void iHadChangedLSPInstanceInTheStateManagerWithNameAndDeviceToNameAndDeviceFrom(
        String lspNameA, String lspDeviceA,
        String lspNameB, String lspDeviceB,
        String lspJsonFile
    ) throws Exception {
        assert Files.exists(new ClassPathResource(lspJsonFile).getFile().toPath());

        NsoLSP[] lsps = loadLspsFromJson(lspJsonFile);
        NsoLSP newLsp = null;
        NsoStateWrapper<NsoLSP> existingLsp = stateManager.getNsoLspStateSyncer().findLocalEntryByName(lspNameA + "," + lspDeviceA);

        for (NsoLSP lsp : lsps) {
            if (lsp.getName().equals(lspNameB) && lsp.getDevice().equals(lspDeviceB)) {
                newLsp = lsp;
                break;
            }
        }

        assert newLsp != null;
        // We aren't expecting the associated VPLS to end up empty, so don't delete it!
        stateManager.deleteLsp(existingLsp.getInstance(), false);
        stateManager.putLsp(newLsp);

    }

    @Given("I had deleted LSP instance in the state manager with name {string} and device {string}")
    public void iHadMarkedLSPInstanceInTheStateManagerWithNameAndDeviceAs(String lspName, String lspDevice) throws NsoStateManagerException {

        int id = (lspName + "," + lspDevice).hashCode();
        NsoStateWrapper<NsoLSP> lsp = stateManager.getNsoLspStateSyncer().findLocalEntryById(id);

        assert lsp != null;

        stateManager.deleteLsp(lsp.getInstance());
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

    @Then("The state manager is not valid")
    public void theStateManagerIsNotValid() {
        assert !stateManager.isValid();
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

    @When("The state manager synchronizes")
    public void theStateManagerSynchronizes() throws Exception {
        stateManager.sync();
    }

    @Then("The state manager is synchronized")
    public void theStateManagerLocalStateIsSynchronized() {
        // Only assert true if dirty.
        assert !stateManager.getNsoLspStateSyncer().isDirty() || stateManager.isLspSynced();
        assert !stateManager.getNsoVplsStateSyncer().isDirty() || stateManager.isVplsSynced();
    }

    @Then("The VPLS {string} in the state manager was marked {string}")
    public void theVPLSInTheStateManagerWasMarked(String arg0, String arg1) {

        int vcId = stateManager.getNsoVplsStateSyncer().getLocalVcIdByName(arg0);
        NsoStateSyncer.State markedAs = stateManager.getNsoVplsStateSyncer().getLocalState().get(vcId).state;
        NsoStateSyncer.State expectedState = NsoStateSyncer.State.valueOf(arg1.replaceAll("-", "").toUpperCase());

        log.info("NsoStateManagerSteps expect VPLS {} was marked as {}, actual state was {}", arg0, expectedState, markedAs);
        assert expectedState == markedAs;
    }

    @When("The VPLS instance {string} is evaluated")
    public void theVPLSInstanceIsEvaluated(String arg0) {
        int vcId = stateManager.getNsoVplsStateSyncer().getLocalVcIdByName(arg0);
        stateManager.getNsoVplsStateSyncer().evaluate(vcId);
    }

    @When("The VPLS instance {string} from JSON file {string} is put in the state manager")
    public void theVPLSInstanceIsReplacedInTheStateManagerWithFromJSONFile(String vplsReplacementName, String vplsJsonFile) throws Exception {
        NsoVPLS replacementVpls = loadSingleVplsFromJson(vplsReplacementName, vplsJsonFile);

        assert replacementVpls != null;

        try {
            stateManager.putVpls(replacementVpls);
        } catch (NsoStateManagerException e) {
            log.error("NsoStateManagerSteps put VPLS {} into state manager failed.", replacementVpls.getName(), e);
            world.add(e);
        }
    }

    static class MockNsoResponseErrorHandler extends NsoResponseErrorHandler {
        List<ClientHttpResponse> clientHttpResponseList = new ArrayList<>();

        @Override
        public void handleError(URI url, HttpMethod method, ClientHttpResponse httpResponse) throws IOException {
            // just log stuff
            log.error("NsoResponseErrorHandler: Error handling " + url + " " + method + " " + httpResponse.getStatusCode());

            if (httpResponse.getStatusCode().is5xxServerError()) {
                log.error("...server error status text: {}", httpResponse.getStatusText());
                clientHttpResponseList.add(httpResponse);
            } else if (httpResponse.getStatusCode().is4xxClientError()) {
                log.error("...client error status text: {}", httpResponse.getStatusText());
                clientHttpResponseList.add(httpResponse);
            }

        }
    }

    private NsoLSP loadLspFromJson(String lspName, String lspDevice, String lspJsonFile) throws Exception {
        NsoLSP[] lsps = loadLspsFromJson(lspJsonFile);
        NsoLSP foundLsp = null;
        for (NsoLSP lsp : lsps) {
            if (lsp.getName().equals(lspName) && lsp.getDevice().equals(lspDevice)) {
                foundLsp = lsp;
                break;
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

    private NsoVPLS loadSingleVplsFromJson(String vplsName, String vplsJsonFile) throws Exception {
        NsoVPLS[] allVpls = loadAllVplsFromJson(vplsJsonFile);
        NsoVPLS foundVpls = null;
        for (NsoVPLS vpls : allVpls) {
            if (vpls.getName().equals(vplsName)) {
                foundVpls = vpls;
                break;
            }
        }
        return foundVpls;
    }

    private NsoVPLS[] loadAllVplsFromJson(String vplsJsonFile) throws Exception {
        return new ObjectMapper()
            .readValue(
                new ClassPathResource(vplsJsonFile).getFile(),
                NsoVPLS[].class
            );
    }
}