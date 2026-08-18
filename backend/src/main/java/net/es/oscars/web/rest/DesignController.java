package net.es.oscars.web.rest;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.app.util.HashidMaker;
import net.es.oscars.resv.beans.DesignResponse;
import net.es.oscars.resv.db.DesignRepository;
import net.es.oscars.resv.ent.Design;
import net.es.oscars.resv.svc.DesignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@RestController
@Slf4j
public class DesignController {
    @Autowired
    private Startup startup;

    @Autowired
    private DesignRepository designRepo;

    @Autowired
    private DesignService designService;

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

    @RequestMapping(value = "/protected/design/verify", method = RequestMethod.POST)
    @ResponseBody
    public DesignResponse design_verify(@RequestBody Design newDesign) throws StartupException {
        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }



        return designService.verifyDesign(newDesign);
    }

    @RequestMapping(value = "/protected/designs/{designId}", method = RequestMethod.POST)
    @ResponseBody
    public DesignResponse design_update_if_valid(@RequestBody Design newDesign, @PathVariable String designId)
    throws StartupException {
        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }


        DesignResponse dr = designService.verifyDesign(newDesign);
        if (dr.isValid()) {
            Optional<Design> maybeDesign = designRepo.findByDesignId(designId);
            if (maybeDesign.isPresent()) {

                log.info("overwriting previous design " + designId);
                maybeDesign.ifPresent(design -> designRepo.delete(design));
            } else {
                log.info("saving new design " + designId);

            }
            designRepo.save(newDesign);
        }
        return dr;
    }


    @RequestMapping(value = "/protected/designs/", method = RequestMethod.GET)
    @ResponseBody
    public List<Design> designs_all() throws StartupException {
        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }



        return designRepo.findAll();
    }


    @RequestMapping(value = "/protected/designs/{designId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void designs_delete(@PathVariable String designId) throws StartupException {
        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }

        Design design = designRepo.findByDesignId(designId).orElseThrow(NoSuchElementException::new);
        designRepo.delete(design);
    }


    @RequestMapping(value = "/protected/designs/{designId}", method = RequestMethod.GET)
    @ResponseBody
    public Design designs_get(@PathVariable String designId) throws StartupException {
        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }

        return designRepo.findByDesignId(designId).orElseThrow(NoSuchElementException::new);
    }


    @RequestMapping(value = "/protected/designs/generateId", method = RequestMethod.GET)
    @ResponseBody
    public String generateDesignId() throws StartupException {
        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }


        boolean found = false;
        String result = "";
        while (!found) {
            String candidate = HashidMaker.randomHashid();
            Optional<Design> d = designRepo.findByDesignId(candidate);
            if (!d.isPresent()) {
                found = true;
                result = candidate;
            }
        }
        return result;


    }


}