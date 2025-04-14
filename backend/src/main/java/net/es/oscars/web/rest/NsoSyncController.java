package net.es.oscars.web.rest;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.sb.nso.NsoStateSyncer;
import net.es.oscars.sb.nso.NsoVplsStateSyncer;
import net.es.oscars.sb.nso.dto.NsoStateWrapper;
import net.es.oscars.sb.nso.exc.NsoStateSyncerException;
import net.es.oscars.web.beans.*;
import net.es.topo.common.dto.nso.NsoVPLS;
import net.es.topo.common.dto.nso.enums.NsoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * NsoSyncController REST API endpoint.
 * HTTP GET /api/nso-sync/{vcId:.+}
 * List of the NSO VPLS objects from NSO.
 * If provided, the vcId parameter can be one or more comma-separated
 * VC-ID integers to retrieve.
 * If no vcId is provided, all VPLS objects are retrieved from NSO.
 *
 * @author aalbino
 * @version 1.2.23
 */
@Slf4j
@RestController
public class NsoSyncController {
    @Autowired
    private Startup startup;

    @Autowired
    private NsoVplsStateSyncer nsoVplsStateSyncer;

    /**
     * Constructor
     * @param nsoVplsStateSyncer Autowired, adds an NsoVplsStateSyncer class object to coordinate NSO Sync.
     */
    public NsoSyncController(NsoVplsStateSyncer nsoVplsStateSyncer) {
        this.nsoVplsStateSyncer = nsoVplsStateSyncer;
    }

    @ExceptionHandler(ConnException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public void handleMiscException(ConnException ex) {
        log.warn("conn request error", ex);
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public void handleResourceNotFoundException() {
        log.warn("requested an item which did not exist");
    }

    /**
     * HTTP GET /api/nso-sync
     * Request for a list of NsoVPLS objects from NSO state.
     *
     * @return Returns a list of NsoVPLS objects
     * @throws Exception Throws an exception on failure.
     */
    @RequestMapping(value = "/api/nso-sync", method = RequestMethod.GET)
    @ResponseBody
    public NsoStateResponse getNsoStates() throws Exception {
        try {
            // Get all of the NsoVPLS objects
            List<NsoVPLS> listVpls = getListVpls("");

            return NsoStateResponse
                    .builder()
                    .vpls(listVpls)
                    .build();
        } catch (Exception e) {
            log.error("Exception while calling getNsoStates HTTP GET path '/api/nso-sync'", e);
            throw e;
        }
    }

    /**
     * HTTP GET /api/nso-sync/{vcId:.+}
     * Request one or more (comma-delimited) NsoVPLS object(s) by vcId
     * @param vcId One or more comma-delimited vcId integers.
     * @return Returns a list of NsoVPLS objects.
     * @throws Exception Throws an exception on failure.
     */
    @RequestMapping(value = "/api/nso-sync/{vcId:.+}", method = RequestMethod.GET)
    @ResponseBody
    public NsoStateResponse getNsoStateById(@PathVariable String vcId) throws Exception {
        try {
            List<NsoVPLS> listVpls = getListVpls(vcId);

            return NsoStateResponse
                    .builder()
                    .vpls(listVpls)
                    .build();
        } catch (Exception e) {
            log.error("HTTP GET /api/nso-sync/{vcId:.+} - Exception while calling getNsoStateById", e);
            throw new Exception(e);
        }
    }

    /**
     * HTTP POST /api/nso-sync
     * Send an HTTP BODY payload containing a list of NsoVPLS objects.
     * The list of NsoVPLS objects are treated as the "local" state, which is evaluated against the
     * "remote" state from NSO. We then synchronize our local state to the remote NSO state.
     *
     * @param nsoStateRequest The NsoStateRequestPayload object containing a list of NsoVPLS objects.
     * @return Returns an NsoStateResponse, which has a boolean flag indicating if a sync was
     * successful (synchronized = true), or if it failed (synchronized = false).
     * @throws NsoStateSyncerException NSO Synchronization exception.
     */
    @RequestMapping(value = "/api/nso-sync", method = RequestMethod.POST)
    @ResponseBody
    public NsoStateResponse postNsoState(@RequestBody NsoStateRequestPayload nsoStateRequest) throws Exception {
        try {
            boolean synced = false;
            if (nsoStateRequest.getVpls() != null) {
                Hashtable<Integer, NsoStateWrapper<NsoVPLS>> localState = new Hashtable<>();
                for (NsoVPLS nsoVpls : nsoStateRequest.getVpls()) {

                    NsoStateWrapper<NsoVPLS> wrappedVpls = new NsoStateWrapper<>(
                            NsoStateSyncer.State.NOOP,
                            nsoVpls
                    );

                    localState.put(nsoVpls.getVcId(), wrappedVpls);
                }

                // Load the NSO state, evaluate what we got against it, and sync!
                nsoVplsStateSyncer.load();
                nsoVplsStateSyncer.setLocalState(localState);

                // Did we synchronize?
                synced = nsoVplsStateSyncer.sync(
                        nsoVplsStateSyncer
                            .getNsoProxy()
                            .getNsoServiceConfigRestPath(NsoService.VPLS)
                );
            }

            return NsoStateResponse
                .builder()
                .isSynchronized(true)
                .build();
        } catch (Exception e) {
            log.error("HTTP POST /api/nso-sync - Exception while syncing NSO VPLS state from OSCARS", e);
            throw e;
        }


    }

    /**
     * HTTP DELETE /api/nso-sync/{vcId:.+}
     * Mark one or more vcID for 'delete' and synchronize to NSO state.
     * @param vcId One or more comma-delimited NSO VPLS objects by vcId.
     * @return If a 'delete' synchronization was a success (true), or not synchronized (false).
     * @throws NsoStateSyncerException Throws an exception if NSO synchronization failed.
     */
    @RequestMapping(value = "/api/nso-sync/{vcId:.+}", method = RequestMethod.DELETE)
    @ResponseBody
    public NsoStateResponse deleteNsoState(@PathVariable String vcId) throws Exception {
        try {
            NsoStateResponse response = new NsoStateResponse();
            boolean synced;
            synced = deleteListVpls(vcId);
            response.setSynchronized( synced );

            return response;
        } catch (Exception e) {
            log.error("HTTP DELETE /api/nso-sync - Exception while syncing NSO VPLS state from OSCARS", e);
            throw e;
        }
    }

    private boolean deleteListVpls(String vcId) throws Exception {
        try {
            boolean deleted = false;
            nsoVplsStateSyncer.load();

            List<Integer> vcIds = new ArrayList<>();

            if (!vcId.isEmpty()) {
                String[] strVcIds = vcId.split(",");
                for (String strVcId : strVcIds) {
                    vcIds.add(Integer.parseInt(strVcId));
                }
            }

            Dictionary<Integer, NsoStateWrapper<NsoVPLS>> wrapped = nsoVplsStateSyncer.getLocalState();
            Enumeration<NsoStateWrapper<NsoVPLS>> e = wrapped.elements();

            // Did the request come with a list of
            // VPLS IDs to retrieve?
            if (!vcIds.isEmpty()) {
                while (e.hasMoreElements()) {
                    NsoStateWrapper<NsoVPLS> wrapper = e.nextElement();
                    if (
                            vcIds.contains(
                                    wrapper.getInstance().getVcId()
                            )
                    ) {
                        // Only the requested vcIds.
                        nsoVplsStateSyncer.delete(wrapper.getInstance().getVcId());
                    }
                }
            } else {
                while (e.hasMoreElements()) {
                    NsoStateWrapper<NsoVPLS> wrapper = e.nextElement();
                    // Get them all
                    nsoVplsStateSyncer.delete(wrapper.getInstance().getVcId());
                }
            }

            deleted = nsoVplsStateSyncer.sync(
                    nsoVplsStateSyncer
                        .getNsoProxy()
                        .getNsoServiceConfigRestPath(NsoService.VPLS)
            );
            return deleted;
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw e;
        }
    }

    private List<NsoVPLS> getListVpls(String vcId) throws Exception {
        List<NsoVPLS> listVpls = new ArrayList<>();
        try {
            // Attempt to get the VPLS states from NSO,
            // and "unwrap" the NsoVPLS objects.
            nsoVplsStateSyncer.load();
            Dictionary<Integer, NsoStateWrapper<NsoVPLS>> wrapped = nsoVplsStateSyncer.getLocalState();

            // ... If we are provided a comma separated list of vcIds, let's get them
            List<Integer> vcIds = new ArrayList<>();
            if (!vcId.isEmpty()) {
                String[] strVcIds = vcId.split(",");
                for (String strVcId : strVcIds) {
                    vcIds.add(Integer.parseInt(strVcId));
                }
            }

            Enumeration<NsoStateWrapper<NsoVPLS>> e = wrapped.elements();

            // Did the request come with a list of
            // VPLS IDs to retrieve?
            if (!vcIds.isEmpty()) {
                while (e.hasMoreElements()) {
                    NsoStateWrapper<NsoVPLS> wrapper = e.nextElement();
                    if (
                            vcIds.contains(
                                    wrapper.getInstance().getVcId()
                            )
                    ) {
                        // Only the requested vcIds.
                        listVpls.add(wrapper.getInstance());
                    }
                }
            } else {
                while (e.hasMoreElements()) {
                    NsoStateWrapper<NsoVPLS> wrapper = e.nextElement();
                    // Get them all
                    listVpls.add(wrapper.getInstance());
                }
            }

            return listVpls;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }
}
