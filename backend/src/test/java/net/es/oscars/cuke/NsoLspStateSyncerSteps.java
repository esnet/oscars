package net.es.oscars.cuke;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.nso.NsoHttpServer;
import net.es.oscars.sb.nso.NsoLspStateSyncer;
import net.es.oscars.sb.nso.NsoProxy;
import net.es.oscars.sb.nso.NsoStateSyncer;
import net.es.oscars.sb.nso.dto.NsoStateWrapper;
import net.es.oscars.sb.nso.exc.NsoStateSyncerException;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.YangPatchWrapper;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    @Given("I had added LSP instance name {string} with device {string} from {string} to VPLS {string} as endpoint {string}")
    public void iHadAddedLSPInstanceNameWithDeviceFromToVPLS(String lspName, String lspDevice, String lspJsonFile, String vplsName, String endpointAorZ) throws Exception {
        assert Files.exists(new ClassPathResource(lspJsonFile).getFile().toPath());
        assert !lspDevice.isEmpty();
        assert !lspName.isEmpty();

        // OSCARS managed VPLS only!
        assert lspName.matches("(\\w+)-(WRK|PRT)-(.*)");

        // A or Z endpoint?
        assert endpointAorZ.matches("^(A|Z)$");

        int originalSize = syncer.getLocalState().size();

        // Add the LSP
        NsoLSP[] lsps = new ObjectMapper()
            .readValue(
                new ClassPathResource(lspJsonFile).getFile(),
                NsoLSP[].class
            );

        boolean found = false;
        NsoLSP addLsp = null;
        for (NsoLSP lsp : lsps) {
            if (lsp.getName().equals(lspName) && lsp.getDevice().equals(lspDevice)) {
                found = true;
                addLsp = lsp;
                break;
            }
        }
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

        // Ok, add it.
        state
            .put(
                key.hashCode(),
                new NsoStateWrapper<>(NsoStateSyncer.State.ADD, addLsp)
            );
        syncer.setLocalState(state);

        log.info("LSP list, was {}, expect {}, now {}", originalSize, originalSize + 1, syncer.getLocalState().size());
        assert syncer.getLocalState().size() == originalSize + 1;
        assert syncer.findLocalEntryByName(addLsp.instanceKey()) != null;
    }
}
