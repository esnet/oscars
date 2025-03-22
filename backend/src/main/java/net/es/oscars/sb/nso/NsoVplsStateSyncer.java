package net.es.oscars.sb.nso;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.sb.nso.dto.NsoStateWrapper;
import net.es.oscars.sb.nso.dto.NsoVplsResponse;
import net.es.oscars.sb.nso.exc.NsoStateSyncerException;
import net.es.topo.common.dto.nso.FromNsoServiceConfig;
import net.es.topo.common.dto.nso.NsoVPLS;
import net.es.topo.common.dto.nso.enums.NsoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * NSO VPLS State Synchronizer.
 * We create a List of Dictionary objects where
 *  - The dictionary key is the NsoVPLS name
 *  - The value is an NsoVPLS object wrapped in NsoStateWrapper that lets us know what State enum operation is expected
 *    with the NsoVPLS object.
 *
 * @author aalbino
 * @since 1.2.23
 */
@Slf4j
@Component
public class NsoVplsStateSyncer extends NsoStateSyncer<NsoStateWrapper<NsoVPLS>> {

    private final NsoProxy nsoProxy;

    public NsoVplsStateSyncer(NsoProxy proxy) {
        super();
        nsoProxy = proxy;
        // Local state, composed of the NSO VPLS object, and the state we are marking it as.
        // Default mark for each state should be NsoStateSyncer.State.NOOP
        Dictionary<String, NsoStateWrapper<NsoVPLS>> localState = new Hashtable<>();
        setLocalState(localState);

        Dictionary<String, NsoStateWrapper<NsoVPLS>> remoteState = new Hashtable<>();
        setRemoteState(remoteState);
    }
    /**
     * Loads the NSO service state data from the specified path.
     *
     * @param path The URI path to the API endpoint to load.
     * @return True if successful, False otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean load(String path) throws NsoStateSyncerException {
        try {

            // Only load if local state is not dirty.
            if (!this.isDirty()) {

                // Load NSO service state from path, with each NsoVPLS object is assigned a NOOP state as default.

                FromNsoServiceConfig serviceConfig = nsoProxy.getNsoServiceConfig(NsoService.VPLS);
                if (serviceConfig.getSuccessful()) {

                    // Get the VPLS, wrap each VPLS in NsoStateWrapper, and populate our
                    // copy of local and remote state.
                    NsoVplsResponse response = nsoProxy.getVpls();
                    for (NsoVPLS vpls : response.getNsoVpls()) {
                        // As the local VPLS matches the Remote VPLS state, state should be NOOP
                        getLocalState().put(vpls.getName(), new NsoStateWrapper<>(State.NOOP, vpls));
                        getRemoteState().put(vpls.getName(), new NsoStateWrapper<>(State.NOOP, vpls));
                    }

                    // Mark local state as loaded = true
                    // Mark local state as dirty = false
                    this.setLoaded(true);
                    this.setDirty(false);
                }
            } else {
                this.setLoaded(false);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this.isLoaded();
    }

    /**
     * Synchronize current service state to the specified API endpoint.
     *
     * @param path The URI path to the API endpoint.
     * @return True if successful, False otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean sync(String path) throws NsoStateSyncerException {
        try {
            // Only synchronize if NSO service state was loaded, and the local service state is dirty = true.
            if (this.isLoaded() && this.isDirty()) {

                // @TODO Sync local state with NSO service state at path

                this.setSynchronized(true);
            }
        } catch (Exception e) {
            throw (NsoStateSyncerException) e;
        }
        return this.isSynchronized();
    }

    /**
     * Evaluate the current state of the specified ID against the loaded NSO state data.
     * Should automatically mark the ID as one of "add", "delete", "redeploy", or "no-op".
     *
     * How do we handle interim changes from remote?
     *  - VPLS in remote state added between now and last sync? Mark as "delete", local state takes precedence.
     *  - VPLS in remote state removed between now and last sync? Mark as "add", local state takes precedence.
     *  - VPLS in remote state redeployed (changed) between now and last sync? Mark as "redeploy", local state takes precedence.
     *
     *
     * @param id The ID to evaluate against the loaded NSO service state.
     * @return NsoStateSyncer.State Return the NsoStateSyncer.State enum result.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public NsoStateSyncer.State evaluate(String id) throws NsoStateSyncerException {
        NsoStateSyncer.State state = State.NOOP;

        try {
            // Only evaluate if we actually have an NSO service state to compare against.
            if (this.isLoaded()) {
                // @TODO Evaluate the local state for entry with the requested ID against the loaded NSO service state
                // @TODO set state = [the new state enum]

                if (getLocalState().get(id) != null && getRemoteState().get(id) != null) {

                    // Exists in local and remote. Default state is NOOP unless changed
                    // Is it changed?
                    if (!getLocalState().get(id).equals(getRemoteState().get(id))) {
                        // Mark for REDEPLOY
                        String description = "Local and remote state differ for VPLS " + id + ", mark for redeploy.";
                        redeploy(id, description);
                    }

                } else {
                    // Doesn't exist in local
                    // Does it exist in remote? (Remote state may have changed between now and last load time)
                    if (getRemoteState().get(id) != null) {
                        String description = "No state found locally for VPLS " + id + ", mark for delete.";
                        // Exists in remote, but not locally. Mark as "delete".
                        delete(id, description);
                        log.info(description);

                    } else if (getLocalState().get(id) != null) {

                        // Exists locally, but not in remote. Mark local as "add".
                        String description = "No state found remotely for VPLS " + id + ", mark for add.";
                        add(id, description);
                        log.info(description);

                    } else {
                        // Doesn't exist in local OR remote. Throw exception
                        throw new NsoStateSyncerException("No state found for VPLS " + id + " in local or remote.");
                    }
                }
            } else {
                throw new NsoStateSyncerException("No state loaded yet.");
            }
        } catch (Exception e) {
            throw (NsoStateSyncerException) e;
        }

        return state;
    }

    /**
     * Mark the specified ID as "add".
     *
     * @param id The ID to mark as "add".
     * @return True if successful, False if add was effectively a no-op.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean add(String id) throws NsoStateSyncerException {
        return marked(id, State.ADD);
    }
    @Override
    public boolean add(String id, String description) throws NsoStateSyncerException {
        return marked(id, State.ADD, description);
    }

    /**
     * Mark the specified ID as "delete".
     *
     * @param id The ID to mark as "delete".
     * @return True if successful, False if delete was effectively a no-op.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean delete(String id) throws NsoStateSyncerException {
        return marked(id, State.DELETE);
    }
    @Override
    public boolean delete(String id, String description) throws NsoStateSyncerException {
        return marked(id, State.DELETE, description);
    }

    /**
     * Mark the specified ID as "redeploy".
     *
     * @param id The ID to mark as "redeploy".
     * @return True if successful, False if redeploy was effectively a no-op.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean redeploy(String id) throws NsoStateSyncerException {
        return marked(id, State.REDEPLOY);
    }
    @Override
    public boolean redeploy(String id, String description) throws NsoStateSyncerException {
        return marked(id, State.REDEPLOY, description);
    }

    /**
     * Mark the specified ID as "no-op".
     *
     * @param id The ID to mark as "no-op"
     * @return True if successful, False otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean noop(String id) throws NsoStateSyncerException {
        return marked(id, State.NOOP);
    }

    /**
     * @param id
     * @param description Optional description for this operation.
     * @return True if successful, false otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean noop(String id, String description) throws NsoStateSyncerException {
        return marked(id, State.NOOP, description);
    }


    /**
     * Return the count of instances in the local state
     *
     * @return The count of instances in the local state as an integer.
     */
    @Override
    public int getLocalInstanceCount() {
        return getLocalState().size();
    }

    /**
     * Return the count of instances in the remote state
     *
     * @return The count of instances in the remote state as an integer.
     */
    @Override
    public int getRemoteInstanceCount() {
        return getRemoteState().size();
    }

    /**
     * Mark a VPLS with state.
     * @param id The VPLS ID to mark.
     * @param state From NsoStateSyncer states: State.ADD, State.DELETE, State.REDEPLOY, State.NOOP
     * @return True if successful, false otherwise.
     * @throws NsoStateSyncerException May throw an exception.
     */
    private boolean marked(String id, State state) throws NsoStateSyncerException {
        return marked(id, state, "");
    }
    /**
     * Mark a VPLS with state.
     * @param id The VPLS ID to mark.
     * @param state From NsoStateSyncer states: State.ADD, State.DELETE, State.REDEPLOY, State.NOOP
     * @param description Optional. Description for this action.
     * @return True if successful, false otherwise.
     * @throws NsoStateSyncerException May throw an exception.
     */
    private boolean marked(String id, State state, String description) throws NsoStateSyncerException {
        boolean marked = false;
        try {
            NsoStateWrapper<NsoVPLS> vplsWrapped = getLocalState().get(id);
            if (vplsWrapped == null) {
                throw new NsoStateSyncerException("NsoVplsStateSyncer.java::marked() - No entry found for VPLS " + id + " in local state when marking as " + state.toString() + ".");
            }

            vplsWrapped.setState(state);
            vplsWrapped.setDescription(description);
            getLocalState().remove(id);
            getLocalState().put(id, vplsWrapped);

            log.info(description);

            marked = true;
        } catch (Exception e) {
            throw (NsoStateSyncerException) e;
        }

        return marked;
    }
}
