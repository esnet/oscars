package net.es.oscars.simpleresv.rest;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.dto.resv.BasicCircuitSpecification;
import net.es.oscars.dto.resv.CircuitSpecification;
import net.es.oscars.dto.resv.Connection;
import net.es.oscars.dto.resv.ReservationDetails;
import net.es.oscars.oscarsapi.RequestController;
import net.es.oscars.simpleresv.svc.ConnectionGenerationService;
import net.es.oscars.simpleresv.svc.ConnectionSimplificationService;
import net.es.oscars.st.resv.ResvState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Provides a simplified endpoint for submitting and committing circuit reservations.
 */
@Slf4j
@Controller
public class SimpleResvController {

    @Autowired
    private ConnectionSimplificationService connectionSimplificationService;

    @Autowired
    private ConnectionGenerationService connectionGenerationService;

    @Autowired
    private RequestController requestController;

    @RequestMapping(value = "/whatif/resv_simple/get/{connectionId}", method = RequestMethod.GET)
    @ResponseBody
    public ReservationDetails getDetails(@PathVariable("connectionId") String connectionId){
        log.info("Retrieving reservation information...");

        Connection conn = requestController.query(connectionId);
        return simplifyResponse(conn, connectionId);
    }

    @RequestMapping(value = "/whatif/resv_simple/connection/add", method = RequestMethod.POST)
    @ResponseBody
    public ReservationDetails submitSpec(@RequestBody CircuitSpecification spec){

        log.info("Received Specification from Client. Submitting (must commit later).");
        log.info("Specification Params: " + spec);

        Connection conn = connectionGenerationService.generateConnection(spec);
        // Submit, do not commit
        conn = requestController.submit(conn);
        return simplifyResponse(conn, spec.getConnectionId());
    }

    @RequestMapping(value = "/whatif/resv_simple/commit/{connectionId}", method = RequestMethod.GET)
    @ResponseBody
    public ReservationDetails commit(@PathVariable("connectionId") String connectionId) {
        log.info("Committing Reservation " + connectionId);

        // Commit
        Connection conn = requestController.commit(connectionId);
        return simplifyResponse(conn, connectionId);
    }

    @RequestMapping(value = "/whatif/resv_simple/connection/add_commit", method = RequestMethod.POST)
    @ResponseBody
    public ReservationDetails submitCommitSpec(@RequestBody CircuitSpecification spec){

        log.info("Received Specification from Client. Submitting and committing (on success).");
        log.info("Specification Params: " + spec);

        Connection conn = connectionGenerationService.generateConnection(spec);
        // Submit
        conn = requestController.submit(conn);
        // Commit if submit was successful
        if(conn.getStates().getResv().equals(ResvState.HELD)) {
            conn = requestController.commit(conn.getConnectionId());
        }
        return simplifyResponse(conn, spec.getConnectionId());
    }

    @RequestMapping(value = "/whatif/resv_simple/connection/add_commit_basic/", method = RequestMethod.POST)
    @ResponseBody
    public ReservationDetails submitCommitBasicSpec(@RequestBody BasicCircuitSpecification spec){

        log.info("Received Basic Specification from Client. Submitting and committing (on success).");
        log.info("Specification Params: " + spec);

        Connection conn = connectionGenerationService.generateConnection(spec);
        // Submit
        conn = requestController.submit(conn);
        // Commit if submit was successful
        if(conn.getStates().getResv().equals(ResvState.HELD)) {
            conn = requestController.commit(conn.getConnectionId());
        }
        return simplifyResponse(conn, spec.getConnectionId());
    }

    @RequestMapping(value = "/whatif/resv_simple/abort/{connectionId}", method = RequestMethod.GET)
    @ResponseBody
    public ReservationDetails abort(@PathVariable("connectionId") String connectionId) {
        log.info("Aborting Reservation " + connectionId);

        // Abort
        Connection conn = requestController.abort(connectionId);
        return simplifyResponse(conn, connectionId);

    }

    private ReservationDetails simplifyResponse(Connection conn, String connectionId){
        ReservationDetails resDetails = connectionSimplificationService.simplifyConnection(conn);
        resDetails.setConnectionId(connectionId);
        log.info("Response Details: " + resDetails.toString());
        return resDetails;
    }


}
