package net.es.oscars.web.rest;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.svc.ConnUtils;
import net.es.oscars.web.beans.ConnectionFilter;
import net.es.oscars.web.beans.Interval;
import net.es.oscars.web.simple.*;
import net.es.oscars.web.beans.PceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;


@RestController
@Slf4j
public class SimpleApiController {

    @Autowired
    private ConnController connController;


    @Autowired
    private Startup startup;

    @ExceptionHandler(StartupException.class)
    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    public void handleStartup(StartupException ex) {
        log.warn("Still in startup");
    }


    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public void handleResourceNotFoundException(NoSuchElementException ex) {
        log.warn("requested an item which did not exist", ex);
    }

    // informational: generate a connection id
    @RequestMapping(value = "/protected/simple/generateId", method = RequestMethod.GET)
    @ResponseBody
    public String generateConnectionId() throws StartupException {
        return connController.generateConnectionId();
    }

    // informational: make a PCE request for a -- z
    @RequestMapping(value = "/protected/simple/pce", method = RequestMethod.POST)
    @ResponseBody
    public void pce(@RequestBody PceRequest request) {

    }

    // combo: hold and immediately commit, perform pathfinding as needed
    @RequestMapping(value = "/protected/simple/combo", method = RequestMethod.POST)
    @ResponseBody
    public void combo(Authentication authentication, @RequestBody SimpleConnection conn) {

    }

    // hold: try to hold some resources for a while
    @RequestMapping(value = "/protected/simple/hold", method = RequestMethod.POST)
    @ResponseBody
    public void hold(Authentication authentication, @RequestBody SimpleConnection conn) {

    }

    // commit: use after successful hold
    @RequestMapping(value = "/protected/simple/commit", method = RequestMethod.POST)
    @ResponseBody
    public void commit(Authentication authentication, @RequestBody SimpleConnection conn) {
    }


    @RequestMapping(value = "/protected/simple/uncommit", method = RequestMethod.POST)
    @ResponseBody
    public void uncommit(@RequestBody String connectionId) throws StartupException {
    }


    @RequestMapping(value = "/protected/simple/cancel", method = RequestMethod.POST)
    @ResponseBody
    public void cancel(@RequestBody String connectionId) throws StartupException {
    }


    @RequestMapping(value = "/protected/simple/unhold", method = RequestMethod.POST)
    @ResponseBody
    public void unhold(@RequestBody String connectionId) {

    }

    @RequestMapping(value = "/api/simple/info", method = RequestMethod.POST)
    @ResponseBody
    public SimpleConnection info(@RequestBody String connectionId) throws StartupException, NoSuchElementException {
        Connection c = connController.info(connectionId);
        if (c == null) {
            return  null;
        } else {
            return ConnUtils.fromConnection(c, false);
        }

    }

    @RequestMapping(value = "/api/conn/simplelist", method = RequestMethod.GET)
    @ResponseBody
    public List<SimpleConnection> simpleList(@RequestParam(defaultValue = "0", required = false) Integer include_svc_id) throws StartupException {
        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }

        boolean return_svc_ids = false;
        if (include_svc_id != null) {
            if (include_svc_id > 0) {
                return_svc_ids = true;
            }
        }
        Interval interval = Interval.builder()
                .beginning(Instant.now())
                .ending(Instant.now())
                .build();
        ConnectionFilter f = ConnectionFilter.builder()
                .phase(Phase.RESERVED.toString())
                .sizePerPage(Integer.MAX_VALUE)
                .interval(interval)
                .page(1)
                .build();
        List<Connection> connections = connController.list(f).getConnections();
        List<SimpleConnection> result = new ArrayList<>();
        for (Connection c : connections) {
            result.add(ConnUtils.fromConnection(c, return_svc_ids));
        }
        return result;
    }

    @RequestMapping(value = "/api/conn/pmcList", method = RequestMethod.GET)
    @ResponseBody
    public List<SimpleConnection> pmcList() throws StartupException {
        ConnectionFilter f = ConnectionFilter.builder()
                .phase(Phase.RESERVED.toString())
                .sizePerPage(Integer.MAX_VALUE)
                .page(1)
                .build();
        List<Connection> connections = connController.list(f).getConnections();
        List<SimpleConnection> result = new ArrayList<>();
        for (Connection c : connections) {
            result.add(ConnUtils.fromConnection(c, false));
        }
        return result;
    }


}