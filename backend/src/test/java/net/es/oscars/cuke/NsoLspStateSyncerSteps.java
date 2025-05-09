package net.es.oscars.cuke;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.sb.nso.NsoLspStateSyncer;
import net.es.oscars.sb.nso.NsoProxy;
import net.es.oscars.sb.nso.NsoStateSyncer;
import net.es.oscars.sb.nso.dto.NsoStateWrapper;
import net.es.oscars.sb.nso.exc.NsoStateSyncerException;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.enums.NsoService;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.util.Dictionary;
import java.util.Hashtable;

@Slf4j
@Category({UnitTests.class})
public class NsoLspStateSyncerSteps extends CucumberSteps {
    @Autowired
    private CucumberWorld world;

    @Autowired
    NsoProxy proxy;

    @Autowired
    NsoLspStateSyncer syncer;


    @Before("@NsoLspSyncSteps")
    public void before() {
        syncer.clear();
    }
    @Given("The NSO LSP service state is loaded")
    public void the_NSO_LSP_service_state_is_loaded() {
        assert syncer.isLoaded();
    }

    @Given("The NSO LSP service state has {int} instances")
    public void theNSOLSPServiceStateHasInstances(int arg0) {
        log.info("NSO LSP service state has {} instances, expect {}", syncer.getLocalState().size(), arg0);
        assert syncer.getLocalState().size() == arg0;
    }

    @Given("I have retrieved the NSO LSPs")
    public void iHaveRetrievedTheNSOLSPs() throws RuntimeException {
        // Load the (mock) NSO response payload
        syncer = new NsoLspStateSyncer(proxy);

        // Make sure the file content you expect is mocked in NsoHttpServer!
        assert syncer.load();
    }

    private NsoLSP[] loadJson(String lspJsonFile) throws Exception {
        return new ObjectMapper()
            .readValue(
                new ClassPathResource(lspJsonFile).getFile(),
                NsoLSP[].class
            );
    }

    private NsoLSP findLSP(String lspName, String lspDevice, String lspJsonFile) throws Exception {
        NsoLSP[] lsps = loadJson(lspJsonFile);
        NsoLSP foundLsp = null;
        for (NsoLSP lsp : lsps) {
            if (lsp.getName().equals(lspName) && lsp.getDevice().equals(lspDevice)) {
                foundLsp = lsp;
                break;
            }
        }
        return foundLsp;
    }
    @Given("I had added LSP instance name {string} with device {string} from {string}")
    public void iHadAddedLSPInstanceNameWithDeviceFromToVPLS(String lspName, String lspDevice, String lspJsonFile) throws Exception {
        assert Files.exists(new ClassPathResource(lspJsonFile).getFile().toPath());
        assert !lspDevice.isEmpty();
        assert !lspName.isEmpty();

        // OSCARS managed VPLS only!
        assert lspName.matches("(\\w+)-(WRK|PRT)-(.*)");

        int originalSize = syncer.getLocalState().size();
        boolean found = false;

        // Add the LSP
        NsoLSP addLsp = findLSP(lspName, lspDevice, lspJsonFile);

        found = addLsp != null;

        log.info("Found LSP with instance key {}? {}", lspName + "," + lspDevice, found);
        assert found;

        // Found it. Add it to the local state, mark it as ADD
        Dictionary<Integer, NsoStateWrapper<NsoLSP>> state = syncer.getLocalState();
        String key = addLsp.instanceKey();
        log.info("Adding LSP with instance key {}", key);

        // It should not exist in local state yet!
        NsoStateWrapper<NsoLSP> existingLsp = syncer.findLocalEntryByName(key);
        log.info("Does the LSP already exist? {}", existingLsp != null);
        assert existingLsp == null;

        // Ok, add it. Default state is "NOOP", let evaluation() call set it to "ADD".
        state
            .put(
                key.hashCode(),
                new NsoStateWrapper<>(NsoStateSyncer.State.NOOP, addLsp)
            );
        syncer.setLocalState(state);
        syncer.setDirty(true);

        log.info("LSP list, was {}, expect {}, now {}", originalSize, originalSize + 1, syncer.getLocalState().size());
        assert syncer.getLocalState().size() == originalSize + 1;
        assert syncer.findLocalEntryByName(addLsp.instanceKey()) != null;
    }

    @Given("I had marked LSP instance with name {string} and device {string} as {string}")
    public void iHadMarkedLSPInstanceWithNameAndDeviceAs(String lspName, String lspDevice, String stateName) {
        NsoStateSyncer.State state = NsoStateSyncer
            .State
            .valueOf(
                stateName
                    .replace("-", "")
                    .toUpperCase()
            );

        int id = (lspName + "," + lspDevice).hashCode();
        NsoStateWrapper<NsoLSP> lsp = syncer.findLocalEntryById(id);

        assert lsp != null;

        switch (state) {
            case ADD:
                syncer.add(id);
                break;
            case DELETE:
                syncer.delete(id);
                break;
            case REDEPLOY:
                syncer.redeploy(id);
                break;
            case NOOP:
                syncer.noop(id);
                break;
        }

    }

    @When("I perform an LSP synchronization")
    public void iPerformAnLSPSynchronization() throws NsoStateSyncerException, Exception {
        try {
            syncer.sync(syncer.getNsoProxy().getNsoServiceConfigRestPath(NsoService.LSP));
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            world.add(e);
        }
    }

    @When("I evaluate LSP with name {string} and device {string}")
    public void iEvaluateLSPWithNameAndDevice(String lspName, String lspDevice) throws Exception {
        int id = (lspName + "," + lspDevice).hashCode();
        syncer.evaluate(id);
    }

    @Then("The NSO LSP service is synchronized")
    public void theNSOLSPServiceIsSynchronized() {
        assert syncer.isSynchronized();
    }

    @Then("The NSO LSP service is not synchronized")
    public void theNSOLSPServiceIsNotSynchronized() {
        assert !syncer.isSynchronized();
    }


    @Then("The list of LSP service instances marked {string} has a count of {int}")
    public void theListOfLSPServiceInstancesMarkedHasACountOf(String stateName, int expectedCount) {
        NsoStateSyncer.State state = NsoStateSyncer
            .State
            .valueOf(
                stateName
                    .replace("-", "")
                    .toUpperCase()
            );
        assert syncer.countByLocalState(state) == expectedCount;
    }

    @Given("I had changed LSP instance with name {string} and device {string} to name {string} and device {string} from {string}")
    public void iHadChangedLSPInstanceWithNameAndDeviceToNameAndDeviceFrom(
        String lspNameA, String lspDeviceA,
        String lspNameB, String lspDeviceB,
        String lspJsonFile
    ) throws Exception {
        assert Files.exists(new ClassPathResource(lspJsonFile).getFile().toPath());
        NsoLSP[] lsps = loadJson(lspJsonFile);
        NsoLSP newLsp = null;
        Dictionary<Integer, NsoStateWrapper<NsoLSP>> state = syncer.getLocalState();
        int id = (lspNameA + "," + lspDeviceA).hashCode();

        for (NsoLSP lsp : lsps) {
            if (lsp.getName().equals(lspNameB) && lsp.getDevice().equals(lspDeviceB)) {
                newLsp = lsp;
                break;
            }
        }

        assert newLsp != null;

        state.remove(id);
        state.put(newLsp.instanceKey().hashCode(), new NsoStateWrapper<>(NsoStateSyncer.State.NOOP, newLsp));

        syncer.setLocalState(state);
        syncer.setDirty(true);
    }
}
