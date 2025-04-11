package net.es.oscars.web.rest;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.sb.nso.NsoVplsStateSyncer;
import net.es.oscars.sb.nso.dto.NsoStateWrapper;
import net.es.oscars.web.beans.ConnException;
import net.es.oscars.web.beans.NsoStateResponse;
import net.es.topo.common.dto.nso.NsoVPLS;
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

    @RequestMapping(value = "/api/nso-sync", method = RequestMethod.GET)
    @ResponseBody
    public NsoStateResponse getNsoStates() throws Exception {
        List<NsoVPLS> listVpls = getListVpls("");

        return NsoStateResponse
                .builder()
                .vpls(listVpls)
                .build();
    }
    @RequestMapping(value = "/api/nso-sync/{vcId:.+}", method = RequestMethod.GET)
    @ResponseBody
    public NsoStateResponse getNsoStateById(@PathVariable String vcId) throws Exception {
        List<NsoVPLS> listVpls = getListVpls(vcId);

        return NsoStateResponse
            .builder()
            .vpls(listVpls)
            .build();
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


        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return listVpls;
    }
}
