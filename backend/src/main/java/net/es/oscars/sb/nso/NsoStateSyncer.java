package net.es.oscars.sb.nso;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.sb.nso.dto.NsoStateWrapper;
import net.es.oscars.sb.nso.exc.NsoStateSyncerException;
import net.es.topo.common.dto.nso.NsoVPLS;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Component;

import java.util.*;

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
    public Dictionary<Integer, Triple<String, State, Boolean>> syncResults = new Hashtable<>();

    public Dictionary<Integer, T> localState = new Hashtable<>();
    private Dictionary<Integer, T> remoteState = new Hashtable<>();

    public void clear() {
        isLoaded = false;
        isDirty = false;
        isSynchronized = false;

        localState = new Hashtable<>();
        remoteState = new Hashtable<>();
    }

    /**
     * Loads the NSO service state data from the specified path.
     * @return True if successful, False otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    public abstract boolean load() throws NsoStateSyncerException;

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
     * Synchronize current service state to the specified API endpoint.
     * @param path The URI path string to the API endpoint.
     * @param dryRun If true, this will perform a dry run. If false, this will attempt an actual synchronization.
     * @return True if successful, False otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    public abstract boolean sync(String path, boolean dryRun) throws NsoStateSyncerException;

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

    /**
     * Find a local state entry by name.
     * @param name The entry name to look for.
     * @return The entry found within the local NSO state list.
     */
    public abstract T findLocalEntryByName(String name);

    /**
     * Find a local state entry by ID.
     * @param id The entry ID to look for.
     * @return The entry found within the local NSO state list.
     */
    public abstract T findLocalEntryById(int id);

    /**
     * Find a remote state entry by name.
     * @param name The entry name to look for.
     * @return The entry found within the remote NSO state list.
     */
    public abstract T findRemoteEntryByName(String name);
    /**
     * Find a remote state entry by ID.
     * @param id The entry ID to look for.
     * @return The entry found within the remote NSO state list.
     */
    public abstract T findRemoteEntryById(int id);

    /**
     * Return the amount of VPLS entries with the requested State enumeration.
     * @param state The NsoStateWrapper.State enumeration to look for
     * @return Returns 0 if none found, or the count of entries with the requested State enumeration set.
     */
    public Integer countByLocalState(State state) {
        Integer count = 0;
        Enumeration<T> enumeration = localState.elements();
        while (enumeration.hasMoreElements()) {
            T wrappedNsoVPLS = enumeration.nextElement();
            if (((NsoStateWrapper<?>) wrappedNsoVPLS).state.equals(state)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Return the VPLS entries with the requested State enumeration.
     * @param state The NsoStateWrapper.State enumeration to look for
     * @return Returns a list of the filtered local state entries.
     */
    public List<T> filterLocalState(State state) {
        List<T> found = new ArrayList<>();
        Enumeration<T> enumeration = localState.elements();
        while (enumeration.hasMoreElements()) {
            T wrappedNsoVPLS = enumeration.nextElement();
            if (((NsoStateWrapper<?>) wrappedNsoVPLS).state.equals(state)) {
                found.add(wrappedNsoVPLS);
            }
        }

        return found;
    }
}
