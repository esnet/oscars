package net.es.oscars.sb.nso;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.sb.nso.dto.NsoVplsResponse;
import net.es.oscars.sb.nso.exc.NsoStateSyncerException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

@Getter
@Setter
@Slf4j
@Component
public abstract class NsoStateSyncer<T> {
    public enum State {
        NOOP,
        ADD,
        DELETE,
        REDEPLOY
    }

    private boolean isLoaded = false;
    private boolean isDirty = false;
    private boolean isSynchronized = false;

    public Dictionary<Integer, T> localState;
    private Dictionary<Integer, T> remoteState;

    /**
     * Loads the NSO service state data from the specified path.
     * @param path The URI path string to the API endpoint to load.
     * @return True if successful, False otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    public abstract boolean load(String path) throws NsoStateSyncerException;

    /**
     * Synchronize current service state to the specified API endpoint.
     * @param path The URI path string to the API endpoint.
     * @return True if successful, False otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    public abstract boolean sync(String path) throws NsoStateSyncerException;

    /**
     * Evaluate the current state of the specified ID against the loaded NSO state data.
     * Should automatically mark the ID as one of "add", "delete", "redeploy", or "no-op".
     * @param id The ID to evaluate against the loaded NSO service state.
     * @return NsoStateSyncer.State Return the NsoStateSyncer.State enum result.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    public abstract NsoStateSyncer.State evaluate(Integer id) throws NsoStateSyncerException;

    /**
     * Mark the specified ID as "add".
     * @param id The ID to mark as "add".
     * @return True if successful, False if add was effectively a no-op.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    public abstract boolean add(Integer id) throws NsoStateSyncerException;
    /**
     * Mark the specified ID as "add".
     * @param id The ID to mark as "add".
     * @param description Optional. The description for this action.
     * @return True if successful, False if add was effectively a no-op.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    public abstract boolean add(Integer id, String description) throws NsoStateSyncerException;

    /**
     * Mark the specified ID as "delete".
     * @param id The ID to mark as "delete".
     * @return True if successful, false otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    public abstract boolean delete(Integer id) throws NsoStateSyncerException;
    /**
     * Mark the specified ID as "delete".
     * @param id The ID to mark as "delete".
     * @param description Optional. The description for this action.
     * @return True if successful, false otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    public abstract boolean delete(Integer id, String description) throws NsoStateSyncerException;

    /**
     * Mark the specified ID as "redeploy".
     * @param id The ID to mark as "redeploy".
     * @return True if successful, false otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    public abstract boolean redeploy(Integer id) throws NsoStateSyncerException;

    /**
     * Mark the specified ID as "redeploy".
     * @param id The ID to mark as "redeploy".
     * @param description Optional. The description for this action.
     * @return True if successful, false otherwise
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    public abstract boolean redeploy(Integer id, String description) throws NsoStateSyncerException;

    /**
     * Mark the specified ID as "no-op".
     * @param id The ID to mark as "no-op"
     * @return True if successful, false otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    public abstract boolean noop(Integer id) throws NsoStateSyncerException;
    /**
     * Mark the specified ID as "no-op".
     * @param id The ID to mark as "no-op"
     * @param description Optional. The description for this action.
     * @return True if successful, false otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    public abstract boolean noop(Integer id, String description) throws NsoStateSyncerException;

    /**
     * Return the count of instances in the local state
     * @return The count of instances in the local state as an integer.
     */
    public abstract int getLocalInstanceCount();

    /**
     * Return the count of instances in the remote state
     * @return The count of instances in the remote state as an integer.
     */
    public abstract int getRemoteInstanceCount();
}
