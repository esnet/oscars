package net.es.oscars.web.rest;


import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.PCEException;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.pce.PceService;

import net.es.oscars.web.beans.PceRequest;
import net.es.oscars.web.beans.PceResponse;
import net.es.oscars.web.beans.v2.AllPathsPceRequest;
import net.es.oscars.web.beans.v2.AllPathsPceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Slf4j
public class PCEController {
    @Autowired
    private Startup startup;


    @Autowired
    private PceService pceService;

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public void handleResourceNotFoundException(NoSuchElementException ex) {
        log.warn("requested an item which did not exist", ex);
    }
    @ExceptionHandler(StartupException.class)
    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    public void handleStartup(StartupException ex) {
        log.warn("Still in startup");
    }



    @RequestMapping(value = "/api/pce/paths", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public PceResponse paths(@RequestBody PceRequest request) throws PCEException, StartupException {
        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }

        return pceService.calculatePaths(request);

    }


    @RequestMapping(value = "/api/pce/all-paths", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public AllPathsPceResponse allPaths(@RequestBody AllPathsPceRequest request) throws PCEException, StartupException {
        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }

        return pceService.calculateAllPaths(request);

    }




}