package net.es.oscars.cuke;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import kotlin.jvm.Throws;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.sb.nso.NsoProxy;
import net.es.oscars.sb.nso.NsoStateSyncer;
import net.es.oscars.sb.nso.NsoVplsStateSyncer;
import net.es.oscars.sb.nso.dto.NsoStateWrapper;
import net.es.oscars.sb.nso.exc.NsoStateSyncerException;
import net.es.topo.common.dto.nso.NsoVPLS;
import net.es.topo.common.dto.nso.enums.NsoService;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Category({UnitTests.class})
public class NsoVplsStateSyncerSteps extends CucumberSteps {
    @Autowired
    private CucumberWorld world;

    NsoVplsStateSyncer syncer;

    @Autowired
    NsoProxy proxy;

    @Given("The list of active OSCARS connections are loaded from {string}")
    public void theListOfActiveOSCARSConnectionsAreLoadedFrom(String arg0) throws Throwable {
        // Load the (mock) NSO response payload
        syncer = new NsoVplsStateSyncer(proxy);
        syncer.load(arg0);
    }

    @Given("The NSO VPLS service state is loaded")
    public void theNSOVPLSServiceStateIsLoaded() throws Throwable {
        // The NSO response payload should be marked as loaded.
        assert syncer != null;
        assert syncer.isLoaded();
    }

    @Given("The NSO VPLS service state has {int} instances")
    public void theNSOVPLSServiceStateHasInstances(int arg0) throws Throwable {
        int count = syncer.getRemoteInstanceCount();
        assert count == arg0;
    }



    @Given("The VPLS instance {string} is present in the NSO VPLS service state")
    public void theVPLSInstanceIsPresentInTheNSOVPLSServiceState(String arg0) throws Throwable {
        Integer id = syncer.getLocalVcIdByName(arg0);
        assert id != 0;
    }

    @Given("The VPLS instance {string} is not present in the NSO VPLS service state")
    public void theVPLSInstanceIsNotPresentInTheNSOVPLSServiceState(String arg0) throws Throwable {
        // STUB
        Integer id = syncer.getLocalVcIdByName(arg0);
        assert id == 0;
    }

    @Given("I had added VPLS instance {string} from {string}")
    public void iHadAddedVPLSInstance(String arg0, String arg1) throws Throwable {
        List<NsoVPLS> vplsList = loadNsoVplsListFromJson(arg1);
        NsoVPLS vpls = null;


        for (NsoVPLS nsoVpls : vplsList) {
            if (nsoVpls.getName().equals(arg0)) {
                vpls = nsoVpls;
            }
        }

        if (vpls == null) {
            throw new AssertionError("No VPLS found with name " + arg0 + " in " + arg1);
        }
        NsoStateWrapper<NsoVPLS> wrappedVpls = new NsoStateWrapper<>(
                NsoStateSyncer.State.NOOP,
                vpls
        );
        syncer
            .getLocalState()
            .put(
                vpls.getVcId(),
                wrappedVpls
            );

        assert syncer.getLocalVcIdByName(arg0) != 0;
    }

    @Given("I had removed VPLS instance {string}")
    public void iHadRemovedVPLSInstance(String arg0) {
        NsoStateWrapper<NsoVPLS> entry = syncer.findLocalEntryByName(arg0);
        if (entry != null) {
            syncer.localState.remove(entry.getInstance().getVcId());
        }
    }

    @Given("I had changed VPLS instance {string} to {string} from {string}")
    public void iHadChangedVPLSInstanceToFrom(String arg0, String arg1, String arg2) throws Throwable {
        // Load the expected list of NsoVPLS from file
        List<NsoVPLS> loaded = loadNsoVplsListFromJson(arg2);
        NsoVPLS vpls = null;
        int id = 0;

        for (NsoVPLS nsoVpls : loaded) {
            if (nsoVpls.getName().equals(arg1)) {
                id = nsoVpls.getVcId();
                vpls = nsoVpls;
                break;
            }
        }
        assert id != 0;

        NsoStateWrapper<NsoVPLS> entry = syncer.findLocalEntryByName(arg0);

        assert entry != null;

        syncer.localState.remove(entry.getInstance().getVcId());

        NsoStateWrapper<NsoVPLS> newWrappedVpls = new NsoStateWrapper<>(NsoStateSyncer.State.NOOP, vpls);
        syncer.localState.put(id, newWrappedVpls);

    }

    /**
     * Evaluate one or more VPLS IDs
     * @param arg0 One or more comma-delimited VPLS IDs
     */
    @When("I evaluate VPLS {string}")
    public void iEvaluateVPLS(String arg0) {
        try {
            // Attempt to get the VC ID from either a local or remote entry.
            int vcId = syncer.getLocalVcIdByName(arg0);
            if (vcId == 0) {
                vcId = syncer.getRemoteVcIdByName(arg0);
            }
            syncer.evaluate(vcId);
        } catch (NsoStateSyncerException e) {
            world.add(e);
        }

    }

    /**
     * Mark one or more VPLS IDs for add
     * @param arg0 One or more comma-delimited VPLS IDs
     * @param arg1 the state to mark with ("add", "delete", "redeploy", "no-op")
     */
    @When("I mark VPLS instance {string} with {string}")
    public void iMarkVPLSInstanceWith(String arg0, String arg1) throws Throwable {
        int vcId = syncer.getLocalVcIdByName(arg0);
        NsoStateSyncer.State requestedState = NsoStateSyncer
            .State
                .valueOf(
                    arg1.replaceAll("-", "")
                    .toUpperCase()
                );

        switch (requestedState) {
            case ADD:
                syncer.add(vcId);
                break;
            case DELETE:
                syncer.delete(vcId);
                break;
            case REDEPLOY:
                syncer.redeploy(vcId);
                break;
            case NOOP:
                syncer.noop(vcId);
                break;
            default:
                throw new AssertionError("Unknown requested state " + requestedState);
                // Do nothing.
        }
    }


    @When("I apply VPLS service patch from {string}")
    public void iApplyVPLSServicePatchFrom(String arg0) {
        // STUB
    }

    @Then("VPLS {string} is marked as {string}")
    public void vplsIsMarkedAs(String arg0, String arg1) {
        int vcId = syncer.getLocalVcIdByName(arg0);
        NsoStateSyncer.State markedAs = syncer.getLocalState().get(vcId).state;
        NsoStateSyncer.State expectedState = NsoStateSyncer.State.valueOf(arg1.replaceAll("-", "").toUpperCase());

        assert expectedState == markedAs;
    }

    @Then("The list of VPLS service instances marked {string} has a count of {int}")
    public void theListOfVPLSServiceInstancesMarkedHasACountOf(String arg0, int arg1) {
        syncer.getLocalVcIdByName(arg0);
        int count = syncer.countByLocalState(
            NsoStateSyncer.State.valueOf(arg0.replaceAll("-", "").toUpperCase())
        );
        log.info(arg0 + " count: " + count + ", expected: " + arg1);
        assert count == arg1;
    }

    @Then("The list of VPLS service instances marked {string} equals {string}")
    public void theListOfVPLSServiceInstancesMarkedEquals(String arg0, String arg1) throws Throwable {
        // Unwrap and add the found NsoVPLS into a clean array list. Sorted.
        List<NsoStateWrapper<NsoVPLS>> foundWrapped = syncer.filterLocalState(
                NsoStateSyncer.State.valueOf(arg0.replaceAll("-", "").toUpperCase())
        );

        List<NsoVPLS> found = new ArrayList<>();
        for (NsoStateWrapper<NsoVPLS> wrappedVpls : foundWrapped) {
            found.add(wrappedVpls.getInstance());
        }
        found.sort((o1, o2) -> o1.getVcId().compareTo(o2.getVcId()));

        // Load the expected list of NsoVPLS from file. Sorted.
        List<NsoVPLS> loaded = loadNsoVplsListFromJson(arg1);
        loaded.sort((o1, o2) -> o1.getVcId().compareTo(o2.getVcId()));

        // Compare found to loaded. If both lists contain each other, they are "equal" enough.
        assert loaded.size() == found.size();
        assert loaded.containsAll(found);
        assert found.containsAll(loaded);

        // Assert each entry in found is exact equal to their
        // counterpart in loaded. We expect them in the same order
        // for test assertions.
        for (int i = 0; i < found.size(); i++) {
            NsoVPLS foundVpls = found.get(i);
            NsoVPLS loadedVpls = loaded.get(i);

            assert loadedVpls.getVcId().equals(foundVpls.getVcId());
            assert loadedVpls.equals(foundVpls);
        }

    }

    @Then("The NSO VPLS service state now has {int} instances")
    public void theNSOVPLSServiceStateNowHasInstances(int arg0) {
        log.info("expect " + arg0 + " instances. local state has " + syncer.getLocalState().size() + " instances, remote state has " + syncer.getRemoteState().size());
        assert syncer.getLocalState().size() == arg0;
        assert syncer.getRemoteState().size() == arg0;
    }

    @When("I perform a synchronization")
    public void iPerformASynchronization() {
        try {
            boolean success = syncer.sync(
                syncer
                    .getNsoProxy()
                    .getNsoServiceConfigRestPath(NsoService.VPLS)
            );

            assert success;
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            world.add(e);
        }
    }
    @Then("The NSO VPLS service is synchronized")
    public void theNSOVPLSServiceIsSynchronized() {
        assert syncer.isSynchronized();
    }

    @Then("The VPLS instance {string} matches {string}")
    public void theVPLSInstanceMatches(String arg0, String arg1) {
        // STUB
    }

    @Then("The list of VPLS service instances equals {string}")
    public void theListOfVPLSServiceInstancesEquals(String arg0) {
        // STUB

    }


    private List<NsoVPLS> loadNsoVplsListFromJson(String path) throws Throwable {
        ObjectMapper mapper = new ObjectMapper();
        NsoVPLS[] vplsList = null;
        List<NsoVPLS> loadedVplsList = new ArrayList<>();

        try {
            Resource resource = new ClassPathResource(path);

            String json = new String(Files.readAllBytes(Paths.get(resource.getFile().getAbsolutePath())));
            vplsList = mapper.readValue(json, NsoVPLS[].class);

            loadedVplsList = new ArrayList<>(Arrays.asList(vplsList));

            log.info("Loaded " + loadedVplsList.size() + " VPLS instances");

        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }

        return loadedVplsList;
    }

}
