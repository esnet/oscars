package net.es.oscars.sb.nso;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.sb.nso.dto.NsoLspResponse;
import net.es.oscars.sb.nso.dto.NsoStateWrapper;
import net.es.oscars.sb.nso.exc.NsoCommitException;
import net.es.oscars.sb.nso.exc.NsoStateSyncerException;
import net.es.oscars.sb.nso.rest.NsoServicesWrapper;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.YangPatchWrapper;
import net.es.topo.common.dto.nso.enums.NsoService;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.es.oscars.sb.nso.NsoAdapter.WORK_LSP_NAME_PIECE;
import static net.es.oscars.sb.nso.NsoAdapter.PROTECT_LSP_NAME_PIECE;
import static net.es.oscars.sb.nso.NsoAdapter.LSP_NAME_DELIMITER;

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
    // this regex should look like this: "(.*)-(WRK|PRT)-(.*)"
    private final String regexPatternOscarsManagedLsp = "(.*)" + LSP_NAME_DELIMITER + "(" + WORK_LSP_NAME_PIECE + "|" + PROTECT_LSP_NAME_PIECE + ")" + LSP_NAME_DELIMITER + "(.*)";
    private Hashtable<Integer, Integer> mapLspToVcId = new Hashtable<>();

    @Autowired
    private NsoAdapter adapter;

    public NsoLspStateSyncer(NsoProxy proxy) {
        super();
        this.nsoProxy = proxy;

        // Local state, composed of the NSO LSP object, and the state we are marking it as.
        // Default mark for each state should be NsoStateSyncer.State.NOOP
        Dictionary<Integer, NsoStateWrapper<NsoLSP>> localState = new Hashtable<>();
        setLocalState(localState);

        Dictionary<Integer, NsoStateWrapper<NsoLSP>> remoteState = new Hashtable<>();
        setRemoteState(remoteState);
    }

    /**
     * Loads the NSO (LSP) service state data from the specified path.
     * A note about LSP name strings. They have the following two (2) patterns:
     * - `(\w+)-(PRT|WRK)-(.*)` OSCARS managed LSP. Example: `C2KR-PRT-losa-cr6`. The first group is the connection ID, followed by either PRT or WRK, and finally, the device string. PRT is "protected", WRK is "work". See NsoAdapter.lspName()
     * - `(\d+)---(.*)` NOT OSCARS managed. A vcId integer (VPLS ID), and the device name. Example: `6999---star-cr6`.
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
     * Loads the NSO (LSP) service state data from the specified path.
     * A note about LSP name strings. They have the following two (2) patterns:
     * - `(.*)-(PRT|WRK)-(.*)` OSCARS managed LSP. Example: `C2KR-PRT-losa-cr6`. The first group is the connection ID, followed by either PRT or WRK, and finally, the device string. PRT is "protected", WRK is "work". See NsoAdapter.lspName()
     * - `(\d+)---(.*)` NOT OSCARS managed. A vcId integer (VPLS ID), and the device name. Example: `6999---star-cr6`.
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
                mapLspToVcId = new Hashtable<>();
                NsoLspResponse lspResponse;
                // Load NSO service state from path, with each NsoVPLS object is assigned a NOOP state as default.
                if (!path.isEmpty()) {
                    lspResponse = nsoProxy.getLsps(path);
                } else {
                    lspResponse = nsoProxy.getLsps();
                }

                if (lspResponse != null) {

                    // Get the LSP, wrap each LSP in NsoStateWrapper, and populate our
                    // copy of local and remote state.
                    for (NsoLSP lsp : lspResponse.getNsoLSPs()) {
                        // Generate an integer using name and device strings
                        Integer key = hashKey(lsp);
                        if (this.isOscarsManaged(lsp)) {
                            log.debug("NsoLspStateSyncer.load() - Managed by OSCARS. Collect LSP " + lsp.getName() + "/" + lsp.getDevice() + " (" + key + ")");
                            getLocalState()
                                    .put(
                                            key,
                                            new NsoStateWrapper<>(State.NOOP, lsp)
                                    );
                            getRemoteState()
                                    .put(
                                            key,
                                            new NsoStateWrapper<>(State.NOOP, lsp)
                                    );
                        } else {
                            log.debug("NsoLspStateSyncer.load() - Not managed by OSCARS. Skip LSP " + lsp.getName() + "/" + lsp.getDevice() + " (" + key + ")");
                        }
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
            log.error(nse.getLocalizedMessage(), nse);
            throw nse;
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new NsoStateSyncerException(e.getLocalizedMessage());
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
        return sync(path, false);
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
        try {
            this.setSynchronized(false);
            if (!this.isLoaded()) {
                throw new NsoStateSyncerException("No state loaded yet.");
            }

            // First, evaluate all local VPLS states
            Enumeration<NsoStateWrapper<NsoLSP>> enumeration = getLocalState().elements();
            while (enumeration.hasMoreElements()) {
                NsoStateWrapper<NsoLSP> wrappedNsoLSP = enumeration.nextElement();
                // This should automatically mark this VPLS as "noop", "add", "delete", or "redeploy"
                evaluate(
                        hashKey(
                                wrappedNsoLSP.getInstance()
                        )
                );
            }

            // Only synchronize if NSO service state was loaded, and the local service state is dirty = true.
            if (this.isDirty()) {
                boolean gotCommitError = false;
                this.setSynchronized(false);
                this.syncResults = new Hashtable<>();

                // Sync local state with NSO service state at path
                // Then, generate the RestTemplate / YangPatches and send using NsoProxy
                List<NsoStateWrapper<NsoLSP>> toDelete = filterLocalState(State.DELETE); // One Yang Patch (1 HTTP call)
                List<NsoStateWrapper<NsoLSP>> toAdd = filterLocalState(State.ADD); // One Yang Patch (1 HTTP call)
                List<NsoStateWrapper<NsoLSP>> toRedeploy = filterLocalState(State.REDEPLOY); // One Yang Patch (1 HTTP call)

                // ...Delete BEGIN
                // @TODO This for loop could be parallelized.
                //  See NsoProxy.makeDismantleYangPatch()
                for (NsoStateWrapper<NsoLSP> wrapper : toDelete) {
                    NsoLSP lsp = wrapper.getInstance();
                    if (!this.isOscarsManaged(lsp)) {
                        // do not dismantle unmanaged LSPs
                        continue;
                    }
                    String connectionId = findConnectionId(lsp);
                    String lspInstanceKey = lsp.instanceKey();

                    try {
                        YangPatchWrapper yangPatchWrapper = NsoProxy.makeDismantleLspYangPatch(lspInstanceKey);
                        nsoProxy.deleteLsp(yangPatchWrapper, lspInstanceKey);
                        this.syncResults.put(hashKey(wrapper.getInstance()), Triple.of(connectionId, State.DELETE, true));

                    } catch (NsoCommitException nsoCommitException) {
                        gotCommitError = true;
                        log.info("Error! NsoCommitException: " + nsoCommitException.getMessage(), nsoCommitException);
                        this.syncResults.put(hashKey(wrapper.getInstance()), Triple.of(connectionId, State.DELETE, false));
                    }
                }
                // ...Delete END

                // ...Redeploy BEGIN
                // @TODO This for loop could be parallelized.
                // @TODO The redeploys MIGHT need to be ordered in a certain way if there are resource dependencies
                for (NsoStateWrapper<NsoLSP> wrapper : toRedeploy) {
                    NsoLSP lsp = wrapper.getInstance();
                    if (!this.isOscarsManaged(lsp)) {
                        log.warn("attempted to redeploy unmanaged LSP {}/{}", lsp.getName(), lsp.getDevice());
                        continue;
                    }

                    String connectionId = findConnectionId(lsp);

                    try {
                        String lspInstanceKey = lsp.instanceKey();
                        YangPatchWrapper yangPatchWrapper = NsoProxy.makeRedeployLspYangPatch(lsp);

                        nsoProxy.redeployLsp(yangPatchWrapper, lspInstanceKey);
                        this.syncResults.put(hashKey(wrapper.getInstance()), Triple.of(connectionId, State.REDEPLOY, true));

                    } catch (NsoCommitException nsoCommitException) {
                        gotCommitError = true;
                        log.info("Error! NsoCommitException: " + nsoCommitException.getMessage(), nsoCommitException);
                        this.syncResults.put(hashKey(wrapper.getInstance()), Triple.of(connectionId, State.REDEPLOY, false));
                    }

                }
                // ...Redeploy END

                // ...Add BEGIN
                // @TODO This for loop could also be parallelized (but must run after the delete)
                for (NsoStateWrapper<NsoLSP> wrapper : toAdd) {
                    NsoServicesWrapper.NsoServicesWrapperBuilder addBuilder = NsoServicesWrapper.builder();
                    List<NsoLSP> addList = new ArrayList<>();
                    NsoLSP lsp = wrapper.getInstance();
                    if (!this.isOscarsManaged(lsp)) {
                        log.warn("attempted to add an unmanaged LSP {}/{}", lsp.getName(), lsp.getDevice());
                        continue;
                    }

                    String connectionId = findConnectionId(lsp);

                    addList.add(lsp);

                    NsoServicesWrapper addThese = addBuilder
                            .lspInstances(addList)
                            .build();
                    try {
                        nsoProxy.buildServices(addThese, connectionId);
                        this.syncResults.put(hashKey(wrapper.getInstance()), Triple.of(connectionId, State.ADD, true));

                    } catch (NsoCommitException nsoCommitException) {
                        gotCommitError = true;
                        this.syncResults.put(hashKey(wrapper.getInstance()), Triple.of(connectionId, State.ADD, false));
                        log.warn(nsoCommitException.getMessage(), nsoCommitException);
                    }
                }
                // ...Add END


                // Set state to "clean" state.
                this.setDirty(false);

                // Mark as synchronized.
                this.setSynchronized(!gotCommitError);

            } else {
                log.info("LSP State is not dirty. Nothing to synchronize.");
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
     * @param id The ID to evaluate against the loaded NSO service state.
     * @return NsoStateSyncer.State Return the NsoStateSyncer.State enum result.
     * @throws NsoStateSyncerException Will throw an exception if an error occurs.
     */
    @Override
    public State evaluate(Integer id) throws NsoStateSyncerException {
        NsoStateSyncer.State state = State.NOOP;

        // Only evaluate if we actually have an NSO service state to compare against.
        if (!this.isLoaded()) {
            throw new NsoStateSyncerException("No state loaded yet.");
        }
        NsoStateWrapper<NsoLSP> local = findLocalEntryById(id);
        NsoStateWrapper<NsoLSP> remote = findRemoteEntryById(id);

        // Evaluate the local state for entry with the requested ID against the loaded NSO service state
        if (local != null && remote != null) {

            // Exists in local and remote. Default state is NOOP unless local and remote entries differ.
            // Is the local entry different from the remote entry?
            if (!local.getInstance().equals(remote.getInstance())) {
                // Mark for REDEPLOY
                String description = "Local and remote state differ for LSP '" + local.getInstance().instanceKey() + "' (" + id + "), mark for redeploy.";
                state = State.REDEPLOY;
                redeploy(id, description);
            }

        } else {
            // Doesn't exist in local
            // Does it exist in remote? (Remote state may have changed between now and last load time)
            if (remote != null) {
                String description = "No state found locally for LSP ' " + remote.getInstance().instanceKey() + "' (" + id + "), mark for delete.";
                // Exists in remote, but not locally. Copy to local, then mark as "delete".
                remote.setState(State.NOOP);
                localState.put(id, remote);

                delete(id, description);
                state = State.DELETE;
                log.info("description: " + description);

            } else if (local != null) {

                // Exists locally, but not in remote. Mark local as "add".
                String description = "No state found remotely for LSP '" + local.getInstance().instanceKey() + "' (" + id + "), mark for add.";
                add(id, description);
                state = State.ADD;
                log.info("description: " + description);

            } else {
                // Doesn't exist in local OR remote. Throw exception
                throw new NsoStateSyncerException("No state found for LSP hash code '" + id + "' in local or remote.");
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
        return marked(id, State.ADD, description);
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
        return marked(id, State.DELETE);
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
        return marked(id, State.DELETE, description);
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
        return marked(id, State.REDEPLOY);
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
     * @param id          The ID to mark as "no-op"
     * @param description Optional. The description for this action.
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
        return localState.size();
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
     * @param instanceKeyName The entry key (as "name,device") to look for.
     * @return The entry found within the local NSO state list.
     */
    @Override
    public NsoStateWrapper<NsoLSP> findLocalEntryByName(String instanceKeyName) {
        return _findByName(instanceKeyName, getLocalState());
    }

    /**
     * NOT SUPPORTED. LSPs do not have any "ID" property provided.
     *
     * @param id The entry ID to look for.
     * @return The entry found within the local NSO state list.
     * @throws UnsupportedOperationException This method is not supported.
     */
    @Override
    public NsoStateWrapper<NsoLSP> findLocalEntryById(int id) throws UnsupportedOperationException {
        return _findById(id, getLocalState());
    }

    /**
     * Find a remote state entry by name.
     *
     * @param name The entry name to look for.
     * @return The entry found within the remote NSO state list.
     */
    @Override
    public NsoStateWrapper<NsoLSP> findRemoteEntryByName(String name) {
        return _findByName(name, getRemoteState());
    }

    /**
     * NOT IMPLEMENTED. Find a remote state entry by ID.
     *
     * @param id The entry ID to look for.
     * @return The entry found within the remote NSO state list.
     * @throws UnsupportedOperationException This method is not supported.
     */
    @Override
    public NsoStateWrapper<NsoLSP> findRemoteEntryById(int id) throws UnsupportedOperationException {
        return _findById(id, getRemoteState());
    }

    /**
     * LSPs don't have a unique ID in their JSON payload. We must go by name and device string, and generate the hashCode integer.
     * WARNING: hashCodes may collide!
     * This is what compositeKey() does:
     * - `int id = (lsp.getName() + '/' + lsp.getDevice()).hashCode();`
     *
     * @param lsp The NsoLSP object
     * @return The composite ID integer
     */
    public static int hashKey(NsoLSP lsp) {
        int id;

        id = (
                lsp.instanceKey()
        ).hashCode();

        return id;
    }

    public String findConnectionId(NsoLSP lsp) {
        String connectionId = "";
        Pattern pattern = Pattern.compile(regexPatternOscarsManagedLsp);
        Matcher matcher = pattern.matcher(lsp.getName());

        if (matcher.find()) {
            connectionId = matcher.group(1);
        }

        return connectionId;
    }

    public boolean isOscarsManaged(NsoLSP lsp) {
        Pattern pattern = Pattern.compile(regexPatternOscarsManagedLsp);
        Matcher matcher = pattern.matcher(lsp.getName());
        return matcher.find();
    }


    NsoStateWrapper<NsoLSP> _findByName(String instanceKeyName, Dictionary<Integer, NsoStateWrapper<NsoLSP>> state) {
        NsoStateWrapper<NsoLSP> result = null;
        Enumeration<NsoStateWrapper<NsoLSP>> enumeration = state.elements();

        while (enumeration.hasMoreElements()) {
            NsoStateWrapper<NsoLSP> wrappedLsp = enumeration.nextElement();
            if (wrappedLsp.getInstance().instanceKey().equals(instanceKeyName)) {
                result = wrappedLsp;
                break;
            }
        }

        return result;
    }

    NsoStateWrapper<NsoLSP> _findById(int id, Dictionary<Integer, NsoStateWrapper<NsoLSP>> state) {
        NsoStateWrapper<NsoLSP> found = null;
        Enumeration<NsoStateWrapper<NsoLSP>> entries = state.elements();
        while (entries.hasMoreElements()) {
            NsoStateWrapper<NsoLSP> entry = entries.nextElement();
            if (entry.getInstance().instanceKey().hashCode() == id) {
                found = entry;
                break;
            }
        }
        return found;
    }

    /**
     * Mark a VPLS with state.
     *
     * @param id    The VPLS ID to mark.
     * @param state From NsoStateSyncer states: State.ADD, State.DELETE, State.REDEPLOY, State.NOOP
     * @return True if successful, false otherwise.
     * @throws NsoStateSyncerException May throw an exception.
     */
    private boolean marked(Integer id, State state) throws NsoStateSyncerException {
        return marked(id, state, "");
    }

    /**
     * Mark a VPLS with state.
     *
     * @param id          The VPLS ID to mark.
     * @param state       From NsoStateSyncer states: State.ADD, State.DELETE, State.REDEPLOY, State.NOOP
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
            NsoStateWrapper<NsoLSP> lspWrapped = getLocalState().get(id);
            if (lspWrapped == null) {
                throw new NsoStateSyncerException("NsoLspStateSyncer.java::marked() - No entry found for LSP hash code " + id + " in local state when marking as " + state.toString() + ".");
            }

            lspWrapped.setState(state);
            lspWrapped.setDescription(description);
            getLocalState().remove(id);
            getLocalState().put(id, lspWrapped);

            if (!isDirty()) {
                setDirty(true);
            }
            marked = true;
        } catch (NsoStateSyncerException nse) {
            log.error(nse.getMessage(), nse);
            // Continue execution, return false.
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new NsoStateSyncerException(e.getMessage());
        }

        return marked;
    }
}
