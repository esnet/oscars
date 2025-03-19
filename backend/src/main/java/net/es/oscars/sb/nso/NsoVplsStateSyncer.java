package net.es.oscars.sb.nso;

import net.es.oscars.sb.nso.exc.NsoStateSyncerException;
import net.es.topo.common.dto.nso.NsoVPLS;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

public class NsoVplsStateSyncer extends NsoStateSyncer<Dictionary<NsoVPLS, NsoStateSyncer.State>> {

    public NsoVplsStateSyncer() {
        super();
        // Local state, composed of the NSO VPLS object, and the state we are marking it as.
        // Default mark for each state should be NsoStateSyncer.State.NOOP
        List<Dictionary<NsoVPLS, NsoStateSyncer.State>> localState = new ArrayList<>();
        setLocalState(localState);

        List<Dictionary<NsoVPLS, NsoStateSyncer.State>> remoteState = new ArrayList<>();
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

                // @TODO Load NSO service state from path, with each NsoVPLS object is assigned a NOOP state as default.

                // Mark local state as loaded = true
                // Mark local state as dirty = false
                this.setLoaded(true);
                this.setDirty(false);
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
        boolean added = false;
        try {
            // @TODO Mark the requested ID within the local state data as NsoSyncerState.State.ADD
            added = true;
        } catch (Exception e) {
            throw (NsoStateSyncerException) e;
        }
        return added;
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
        boolean deleted = false;
        try {
            // @TODO Mark the requested ID within the local state data as NsoSyncerState.State.DELETE
            deleted = true;
        } catch (Exception e) {
            throw (NsoStateSyncerException) e;
        }
        return deleted;
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
        boolean redeployed = false;
        try {
            // @TODO Mark the requested ID within the local state data as NsoSyncerState.State.REDEPLOY
            redeployed = true;
        } catch (Exception e) {
            throw (NsoStateSyncerException) e;
        }
        return redeployed;
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
        boolean nooped = false;
        try {
            // @TODO mark the requested ID within the local state data as NsoSyncerState.State.NOOP
            nooped = true;
        } catch (Exception e) {
            throw (NsoStateSyncerException) e;
        }
        return nooped;
    }
}
