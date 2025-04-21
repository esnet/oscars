package net.es.oscars.sb.nso;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.sb.nso.dto.NsoStateWrapper;
import net.es.oscars.sb.nso.exc.NsoStateSyncerException;
import net.es.topo.common.dto.nso.NsoLSP;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NsoLspStateSyncer extends NsoStateSyncer<NsoStateWrapper<NsoLSP>> {
    /**
     * Loads the NSO service state data from the specified path.
     *
     * @param path The URI path string to the API endpoint to load.
     * @return True if successful, False otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean load(String path) throws NsoStateSyncerException {
        return false;
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
     * Find a local state entry by ID.
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
     * Find a remote state entry by ID.
     *
     * @param id The entry ID to look for.
     * @return The entry found within the remote NSO state list.
     */
    @Override
    public NsoStateWrapper<NsoLSP> findRemoteEntryById(int id) {
        return null;
    }
}
