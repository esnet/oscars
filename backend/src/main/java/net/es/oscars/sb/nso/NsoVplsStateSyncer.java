package net.es.oscars.sb.nso;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.sb.nso.ent.NsoService;
import net.es.topo.common.dto.nso.YangPatch;
import org.springframework.stereotype.Component;
import net.es.oscars.sb.nso.dto.NsoStateWrapper;
import net.es.oscars.sb.nso.dto.NsoVplsResponse;
import net.es.oscars.sb.nso.exc.NsoStateSyncerException;
import net.es.topo.common.dto.nso.FromNsoServiceConfig;
import net.es.topo.common.dto.nso.NsoVPLS;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * NSO VPLS State Synchronizer.
 * We create a List of Dictionary objects where
 *  - The dictionary key is the NsoVPLS vc-id value
 *  - The value is an NsoVPLS object wrapped in NsoStateWrapper that lets us know what State enum operation is expected
 *    with the NsoVPLS object.
 *
 * Usage examples:
 *
 * <pre>
 * NsoVplsStateSyncer vplsStateSyncer = new NsoVplsStateSyncer();
 *
 * // Assume we already have an NsoVPLS object...
 * // that has vc-id, qos-mode, name, routing-domain, etc...
 * // NsoVPLS vpls = new NsoVPLS(...);
 *
 *
 * // Add a new NsoVPLS
 * vplsStateSyncer.getLocalState().put(
 *   vpls.getVcId(), // The vc-id value
 *   vpls // The NsoVPLS object
 * )
 *
 * // Remove an existing NsoVPLS
 * vplsStateSyncer.getLocalState().remove(
 *   vpls.getVcId()
 * );
 *
 * // Redeploy an existing NsoVPLS
 * // Assume we have an oldNsoVPLS object and a newNsoVPLS object.
 * // ... remove the old one first
 * vplsStateSyncer.getLocalState().remove(
 *   oldNsoVPLS.getVcId()
 * )
 * // ... Add the new one
 * vplsStateSyncer.getLocalState().put(
 *   newNsoVPLS.getVcId(), // The vc-id value
 *   newNsoVPLS // The NsoVPLS object
 * )
 *
 * </pre>
 * @author aalbino
 * @since 1.2.23
 */
@Slf4j
@Component
public class NsoVplsStateSyncer extends NsoStateSyncer<NsoStateWrapper<NsoVPLS>> {

    private final NsoProxy nsoProxy;
    public NsoProxy getNsoProxy() {
        return nsoProxy;
    }
    public NsoVplsStateSyncer(NsoProxy proxy) {
        super();
        nsoProxy = proxy;
        // Local state, composed of the NSO VPLS object, and the state we are marking it as.
        // Default mark for each state should be NsoStateSyncer.State.NOOP
        Dictionary<Integer, NsoStateWrapper<NsoVPLS>> localState = new Hashtable<>();
        setLocalState(localState);

        Dictionary<Integer, NsoStateWrapper<NsoVPLS>> remoteState = new Hashtable<>();
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

                NsoVplsResponse vplsResponse = nsoProxy.getVpls();
                if (vplsResponse != null) {

                    // Get the VPLS, wrap each VPLS in NsoStateWrapper, and populate our
                    // copy of local and remote state.
                    for (NsoVPLS vpls : vplsResponse.getNsoVpls()) {
                        // As the local VPLS matches the Remote VPLS state, state should be NOOP
                        getLocalState().put(vpls.getVcId(), new NsoStateWrapper<>(State.NOOP, vpls));
                        getRemoteState().put(vpls.getVcId(), new NsoStateWrapper<>(State.NOOP, vpls));
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
     * @param path The URI path to the API endpoint.
     * @return True if successful, False otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean sync(String path) throws NsoStateSyncerException {
        try {
            if (!this.isLoaded()) {
                throw new NsoStateSyncerException("No state loaded yet.");
            }
            // Only synchronize if NSO service state was loaded, and the local service state is dirty = true.
            if (this.isDirty()) {
                // Sync local state with NSO service state at path

                // First, evaluate all local VPLS states
                Enumeration<NsoStateWrapper<NsoVPLS>> enumeration = getLocalState().elements();
                while (enumeration.hasMoreElements()) {
                    NsoStateWrapper<NsoVPLS> wrappedNsoVPLS = enumeration.nextElement();
                    // This should automatically mark this VPLS as "noop", "add", "delete", or "redeploy"
                    evaluate(wrappedNsoVPLS.getInstance().getVcId());
                }

                // @TODO Generate the RestTemplate / YangPatches and send using NsoProxy
                List<NsoStateWrapper<NsoVPLS>> toDelete = filterLocalState(State.DELETE);
                List<NsoStateWrapper<NsoVPLS>> toAdd = filterLocalState(State.ADD);
                List<NsoStateWrapper<NsoVPLS>> toRedeploy = filterLocalState(State.REDEPLOY);

                for (NsoStateWrapper<NsoVPLS> wrapper : toDelete) {
                    NsoVPLS delete = wrapper.getInstance();
                    // Mark this VPLS vc-id to DELETE
                }

                for (NsoStateWrapper<NsoVPLS> wrapper : toAdd) {
                    NsoVPLS add = wrapper.getInstance();
                    // Mark this VPLS vc-id to ADD
                }

                for (NsoStateWrapper<NsoVPLS> wrapper : toRedeploy) {
                    NsoVPLS redeploy = wrapper.getInstance();
                    // Mark this VPLS vc-id to REDEPLOY
                }
//                FromNsoServiceConfig serviceConfig = new FromNsoServiceConfig();
//                NsoAdapter.NsoOscarsDismantle dismantle = new NsoAdapter.NsoOscarsDismantle(dismantleConnectionId, dismantleVcId, listKeys);
//                nsoProxy.deleteServices(dismantle);


                this.setSynchronized(true);
                this.setDirty(false);
            }
        } catch (NsoStateSyncerException nse) {
            log.error(nse.getMessage(), nse);
            throw nse;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new NsoStateSyncerException(e.getMessage());
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
    public NsoStateSyncer.State evaluate(Integer id) throws NsoStateSyncerException {
        NsoStateSyncer.State state = State.NOOP;

        // Only evaluate if we actually have an NSO service state to compare against.
        if (!this.isLoaded()) {
            throw new NsoStateSyncerException("No state loaded yet.");
        }
        NsoStateWrapper<NsoVPLS> local = findLocalEntryById(id);
        NsoStateWrapper<NsoVPLS> remote = findRemoteEntryById(id);

        // Evaluate the local state for entry with the requested ID against the loaded NSO service state
        if (local != null && remote != null) {

            // Exists in local and remote. Default state is NOOP unless local and remote entries differ.
            // Is the local entry different from the remote entry?
            if (!local.getInstance().equals(remote.getInstance())) {
                // Mark for REDEPLOY
                String description = "Local and remote state differ for VPLS " + id + ", mark for redeploy.";
                redeploy(id, description);
            }

        } else {
            // Doesn't exist in local
            // Does it exist in remote? (Remote state may have changed between now and last load time)
            if (remote != null) {
                String description = "No state found locally for VPLS " + id + ", mark for delete.";
                // Exists in remote, but not locally. Copy to local, then mark as "delete".
                remote.setState(State.NOOP);
                localState.put(id, remote);

                delete(id, description);
                log.info(description);

            } else if (local != null) {

                // Exists locally, but not in remote. Mark local as "add".
                String description = "No state found remotely for VPLS " + id + ", mark for add.";
                add(id, description);
                log.info(description);

            } else {
                // Doesn't exist in local OR remote. Throw exception
                throw new NsoStateSyncerException("No state found for VPLS " + id + " in local or remote.");
            }
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
    public boolean add(Integer id) throws NsoStateSyncerException {
        return marked(id, State.ADD);
    }

    public boolean add(String name) throws NsoStateSyncerException {

        Integer id = getLocalVcIdByName(name);
        if (id == 0) return false;

        return add(id);
    }
    /**
     * Mark the specified ID as "add".
     *
     * @param id The ID to mark as "add".
     * @param description Optional. The description for this action.
     * @return True if successful, False if add was effectively a no-op.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean add(Integer id, String description) throws NsoStateSyncerException {
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
    public boolean delete(Integer id) throws NsoStateSyncerException {
        return marked(id, State.DELETE);
    }
    /**
     * Mark the specified ID as "delete".
     *
     * @param id The ID to mark as "delete".
     * @param description Optional. The description for this action.
     * @return True if successful, False if delete was effectively a no-op.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean delete(Integer id, String description) throws NsoStateSyncerException {
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
    public boolean redeploy(Integer id) throws NsoStateSyncerException {
        return marked(id, State.REDEPLOY);
    }
    /**
     * Mark the specified ID as "redeploy".
     *
     * @param id The ID to mark as "redeploy".
     * @param description Optional. The description for this action.
     * @return True if successful, false otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean redeploy(Integer id, String description) throws NsoStateSyncerException {
        return marked(id, State.REDEPLOY, description);
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
        return marked(id, State.NOOP);
    }

    /**
     * Mark the specified ID as "no-op".
     *
     * @param id The ID to mark as "redeploy".
     * @param description Optional description for this operation.
     * @return True if successful, false otherwise.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public boolean noop(Integer id, String description) throws NsoStateSyncerException {
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
     * Find a local state entry by name.
     *
     * @param name The entry name to look for.
     * @return The entry found within the local NSO state list.
     */
    @Override
    public NsoStateWrapper<NsoVPLS> findLocalEntryByName(String name) {
        NsoStateWrapper<NsoVPLS> found = null;
        Enumeration<NsoStateWrapper<NsoVPLS>> enumeration = getLocalState().elements();
        while (enumeration.hasMoreElements()) {
            NsoStateWrapper<NsoVPLS> wrappedNsoVPLS = enumeration.nextElement();
            if (wrappedNsoVPLS.getInstance().getName().equals(name)) {
                found = wrappedNsoVPLS;
                break;
            }
        }
        return found;
    }

    /**
     * Find a local state entry by ID.
     * @param id The entry ID to look for.
     * @return The entry found within the local NSO state list.
     */
    @Override
    public NsoStateWrapper<NsoVPLS> findLocalEntryById(int id) {
        NsoStateWrapper<NsoVPLS> found = null;
        Enumeration<NsoStateWrapper<NsoVPLS>> enumeration = getLocalState().elements();
        while (enumeration.hasMoreElements()) {
            NsoStateWrapper<NsoVPLS> wrappedNsoVPLS = enumeration.nextElement();
            if (wrappedNsoVPLS.getInstance().getVcId().equals(id)) {
                found = wrappedNsoVPLS;
                break;
            }
        }
        return found;
    }

    /**
     * Find a remote state entry by name.
     *
     * @param name The entry name to look for.
     * @return The entry found within the remote NSO state list.
     */
    @Override
    public NsoStateWrapper<NsoVPLS> findRemoteEntryByName(String name) {
        NsoStateWrapper<NsoVPLS> found = null;
        Enumeration<NsoStateWrapper<NsoVPLS>> enumeration = getRemoteState().elements();
        while (enumeration.hasMoreElements()) {
            NsoStateWrapper<NsoVPLS> wrappedNsoVPLS = enumeration.nextElement();
            if (wrappedNsoVPLS.getInstance().getName().equals(name)) {
                found = wrappedNsoVPLS;
                break;
            }
        }
        return found;
    }

    /**
     * Find a remote state entry by ID.
     * @param id The entry ID to look for.
     * @return The entry found within the remote NSO state list.
     */
    @Override
    public NsoStateWrapper<NsoVPLS> findRemoteEntryById(int id) {
        NsoStateWrapper<NsoVPLS> found = null;
        Enumeration<NsoStateWrapper<NsoVPLS>> enumeration = getRemoteState().elements();
        while (enumeration.hasMoreElements()) {
            NsoStateWrapper<NsoVPLS> wrappedNsoVPLS = enumeration.nextElement();
            if (wrappedNsoVPLS.getInstance().getVcId().equals(id)) {
                found = wrappedNsoVPLS;
                break;
            }
        }
        return found;
    }

    /**
     * Mark a VPLS with state.
     * @param id The VPLS ID to mark.
     * @param state From NsoStateSyncer states: State.ADD, State.DELETE, State.REDEPLOY, State.NOOP
     * @return True if successful, false otherwise.
     * @throws NsoStateSyncerException May throw an exception.
     */
    private boolean marked(Integer id, State state) throws NsoStateSyncerException {
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
    private boolean marked(Integer id, State state, String description) throws NsoStateSyncerException {
        boolean marked = false;
        try {
            if (!this.isLoaded()) {
                throw new NsoStateSyncerException("No state loaded yet.");
            }
            NsoStateWrapper<NsoVPLS> vplsWrapped = getLocalState().get(id);
            if (vplsWrapped == null) {
                throw new NsoStateSyncerException("NsoVplsStateSyncer.java::marked() - No entry found for VPLS " + id + " in local state when marking as " + state.toString() + ".");
            }

            vplsWrapped.setState(state);
            vplsWrapped.setDescription(description);
            getLocalState().remove(id);
            getLocalState().put(id, vplsWrapped);

            log.info(description);


            if (!isDirty() && !State.NOOP.equals(state)) {
                setDirty(true);
            }
        } catch (NsoStateSyncerException nse) {
            log.error(nse.getMessage(), nse);
            // Continue execution, return false.
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new NsoStateSyncerException(e.getMessage());
        }

        return marked;
    }

    /**
     * Return the local VPLS ID according to its name.
     * @param name The VPLS name string.
     * @return Returns 0 if not found.
     */
    public Integer getLocalVcIdByName(String name) {
        return _getVcIdByName(name, getLocalState());
    }

    /**
     * Return the remote VPLS ID according to its name.
     * @param name The VPLS name string.
     * @return Returns 0 if not found.
     */
    public Integer getRemoteVcIdByName(String name) {
        return _getVcIdByName(name, getRemoteState());
    }

    private Integer _getVcIdByName(String name, Dictionary<Integer, NsoStateWrapper<NsoVPLS>> state) {
        Integer id = 0;

        Enumeration<NsoStateWrapper<NsoVPLS>> enumeration = state.elements();
        while (enumeration.hasMoreElements()) {
            NsoStateWrapper<NsoVPLS> wrappedNsoVPLS = enumeration.nextElement();
            if (wrappedNsoVPLS.getInstance().getName().equals(name)) {
                id = wrappedNsoVPLS.getInstance().getVcId();
                break;
            }
        }

        return id;
    }
}
