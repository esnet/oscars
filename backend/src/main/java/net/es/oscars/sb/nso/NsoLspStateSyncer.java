package net.es.oscars.sb.nso;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.sb.nso.dto.NsoLspResponse;
import net.es.oscars.sb.nso.dto.NsoStateWrapper;
import net.es.oscars.sb.nso.dto.NsoVplsResponse;
import net.es.oscars.sb.nso.exc.NsoStateSyncerException;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.NsoVPLS;
import net.es.topo.common.dto.nso.enums.NsoService;
import org.springframework.stereotype.Component;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * NSO LSP State synchronizer.
 *
 * @author aalbino
 * @since 1.2.24
 */
@Slf4j
@Component
@Getter
@Setter
public class NsoLspStateSyncer extends NsoStateSyncer<NsoStateWrapper<NsoLSP>> {

    private final NsoProxy nsoProxy;

    public Dictionary<String, NsoStateWrapper<NsoLSP>> localLSPState;
    private Dictionary<String, NsoStateWrapper<NsoLSP>> remoteLSPState;

    public NsoLspStateSyncer(NsoProxy proxy) {
        super();
        this.nsoProxy = proxy;

        // Local state, composed of the NSO LSP object, and the state we are marking it as.
        // Default mark for each state should be NsoStateSyncer.State.NOOP
        Dictionary<String, NsoStateWrapper<NsoLSP>> localState = new Hashtable<>();
        setLocalLSPState(localState);

        Dictionary<String, NsoStateWrapper<NsoLSP>> remoteState = new Hashtable<>();
        setRemoteLSPState(remoteState);
    }

    /**
     * Loads the NSO service state data from the default NsoProxy path.
     *
     * @return True if successful, False otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean load() throws NsoStateSyncerException {
        boolean success = false;
        try {
            String path = nsoProxy.getNsoServiceConfigRestPath(NsoService.LSP);
            success = load(path);
        } catch (Exception e) {
            log.error("NsoLspStateSyncer.load() - error while loading nso services", e);
            throw new NsoStateSyncerException(e.getLocalizedMessage());
        }

        return success;
    }

    /**
     * Loads the NSO service state data from the specified path.
     *
     * @param path The URI path string to the API endpoint to load.
     * @return True if successful, False otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean load(String path) throws NsoStateSyncerException {
        try {

            // Only load if local state is not dirty.
            if (!this.isDirty()) {

                NsoLspResponse lspResponse;
                // Load NSO service state from path, with each NsoVPLS object is assigned a NOOP state as default.
                if (!path.isEmpty()) {
                    lspResponse = nsoProxy.getLsps(path);
                } else {
                    lspResponse = nsoProxy.getLsps();
                }

                if (lspResponse != null) {

                    // Get the VPLS, wrap each VPLS in NsoStateWrapper, and populate our
                    // copy of local and remote state.
                    for (NsoLSP lsp : lspResponse.getNsoLSPs()) {
                        // As the local VPLS matches the Remote VPLS state, state should be NOOP
                        getLocalLSPState().put(lsp.getName(), new NsoStateWrapper<>(State.NOOP, lsp));
                        getRemoteLSPState().put(lsp.getName(), new NsoStateWrapper<>(State.NOOP, lsp));
                    }

                    // Mark local state as loaded = true
                    // Mark local state as dirty = false
                    this.setLoaded(true);
                    this.setDirty(false);
                }
            } else {
                this.setLoaded(false);
            }
        } catch (NsoStateSyncerException nse) {
            log.error(nse.getMessage(), nse);
            throw nse;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new NsoStateSyncerException(e.getMessage());
        }
        return this.isLoaded();
    }

    /**
     * Synchronize current service state to the specified API endpoint.
     *
     * @param path The URI path string to the API endpoint.
     * @return True if successful, False otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean sync(String path) throws NsoStateSyncerException {
        return false;
    }

    /**
     * Synchronize current service state to the specified API endpoint.
     *
     * @param path   The URI path string to the API endpoint.
     * @param dryRun If true, this will perform a dry run. If false, this will attempt an actual synchronization.
     * @return True if successful, False otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean sync(String path, boolean dryRun) throws NsoStateSyncerException {
        return false;
    }

    /**
     * Evaluate the current state of the specified ID against the loaded NSO state data.
     * Should automatically mark the ID as one of "add", "delete", "redeploy", or "no-op".
     *
     * @param id The ID to evaluate against the loaded NSO service state.
     * @return NsoStateSyncer.State Return the NsoStateSyncer.State enum result.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public State evaluate(Integer id) throws NsoStateSyncerException {
        return null;
    }

    /**
     * Mark the specified ID as "add".
     *
     * @param id The ID to mark as "add".
     * @return True if successful, False if add was effectively a no-op.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean add(Integer id) throws NsoStateSyncerException {
        return false;
    }

    /**
     * Mark the specified ID as "add".
     *
     * @param id          The ID to mark as "add".
     * @param description Optional. The description for this action.
     * @return True if successful, False if add was effectively a no-op.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean add(Integer id, String description) throws NsoStateSyncerException {
        return false;
    }

    /**
     * Mark the specified ID as "delete".
     *
     * @param id The ID to mark as "delete".
     * @return True if successful, false otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean delete(Integer id) throws NsoStateSyncerException {
        return false;
    }

    /**
     * Mark the specified ID as "delete".
     *
     * @param id          The ID to mark as "delete".
     * @param description Optional. The description for this action.
     * @return True if successful, false otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean delete(Integer id, String description) throws NsoStateSyncerException {
        return false;
    }

    /**
     * Mark the specified ID as "redeploy".
     *
     * @param id The ID to mark as "redeploy".
     * @return True if successful, false otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean redeploy(Integer id) throws NsoStateSyncerException {
        return false;
    }

    /**
     * Mark the specified ID as "redeploy".
     *
     * @param id          The ID to mark as "redeploy".
     * @param description Optional. The description for this action.
     * @return True if successful, false otherwise
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean redeploy(Integer id, String description) throws NsoStateSyncerException {
        return false;
    }

    /**
     * Mark the specified ID as "no-op".
     *
     * @param id The ID to mark as "no-op"
     * @return True if successful, false otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean noop(Integer id) throws NsoStateSyncerException {
        return false;
    }

    /**
     * Mark the specified ID as "no-op".
     *
     * @param id          The ID to mark as "no-op"
     * @param description Optional. The description for this action.
     * @return True if successful, false otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean noop(Integer id, String description) throws NsoStateSyncerException {
        return false;
    }

    /**
     * Return the count of instances in the local state
     *
     * @return The count of instances in the local state as an integer.
     */
    @Override
    public int getLocalInstanceCount() {
        return 0;
    }

    /**
     * Return the count of instances in the remote state
     *
     * @return The count of instances in the remote state as an integer.
     */
    @Override
    public int getRemoteInstanceCount() {
        return 0;
    }

    /**
     * Find a local state entry by name.
     *
     * @param name The entry name to look for.
     * @return The entry found within the local NSO state list.
     */
    @Override
    public NsoStateWrapper<NsoLSP> findLocalEntryByName(String name) {
        return null;
    }

    /**
     * NOT IMPLEMENTED. Find a local state entry by ID.
     *
     * @param id The entry ID to look for.
     * @return The entry found within the local NSO state list.
     */
    @Override
    public NsoStateWrapper<NsoLSP> findLocalEntryById(int id) {
        return null;
    }

    /**
     * Find a remote state entry by name.
     *
     * @param name The entry name to look for.
     * @return The entry found within the remote NSO state list.
     */
    @Override
    public NsoStateWrapper<NsoLSP> findRemoteEntryByName(String name) {
        return null;
    }

    /**
     * NOT IMPLEMENTED. Find a remote state entry by ID.
     *
     * @param id The entry ID to look for.
     * @return The entry found within the remote NSO state list.
     */
    @Override
    public NsoStateWrapper<NsoLSP> findRemoteEntryById(int id) {
        return null;
    }
}
