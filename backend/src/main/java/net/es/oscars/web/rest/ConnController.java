package net.es.oscars.web.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.NsiException;
import net.es.oscars.app.exc.NsiMappingException;
import net.es.oscars.app.exc.PCEException;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.app.util.UsernameGetter;
import net.es.oscars.nsi.ent.NsiMapping;
import net.es.oscars.nsi.svc.NsiMappingService;
import net.es.oscars.nsi.svc.NsiService;
import net.es.oscars.sb.nso.resv.NsoResvException;
import net.es.oscars.sb.ent.RouterCommandHistory;
import net.es.oscars.resv.db.CommandHistoryRepository;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.enums.BuildMode;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.enums.State;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.resv.svc.ConnUtils;
import net.es.oscars.web.beans.*;
import net.es.oscars.web.simple.SimpleConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@RestController
@Slf4j
public class ConnController {
    private final Startup startup;

    private final ConnectionRepository connRepo;

    private final CommandHistoryRepository historyRepo;

    private final ConnService connSvc;

    private final ConnUtils connUtils;

    private final NsiService nsiSvc;

    private final NsiMappingService nsiMappingService;

    private final UsernameGetter usernameGetter;

    public ConnController(Startup startup, ConnectionRepository connRepo, CommandHistoryRepository historyRepo,
                          ConnService connSvc, ConnUtils connUtils, NsiService nsiSvc,
                          NsiMappingService nsiMappingService, UsernameGetter usernameGetter) {
        this.startup = startup;
        this.connRepo = connRepo;
        this.historyRepo = historyRepo;
        this.connSvc = connSvc;
        this.connUtils = connUtils;
        this.nsiSvc = nsiSvc;
        this.nsiMappingService = nsiMappingService;
        this.usernameGetter = usernameGetter;
    }

    @ExceptionHandler(StartupException.class)
    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    public void handleStartup() {
        log.warn("Still in startup");
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


    @RequestMapping(value = "/protected/conn/generateId", method = RequestMethod.GET)
    public String generateConnectionId() throws StartupException {
        this.checkStartup();
        return connUtils.genUniqueConnectionId();
    }


    @RequestMapping(value = "/protected/conn/commit", method = RequestMethod.POST)
    @ResponseBody
    public ConnChangeResult commit(Authentication authentication, @RequestBody String connectionId)
            throws StartupException, NsoResvException, PCEException, ConnException {
        this.checkStartup();


        Connection c = connSvc.findConnection(connectionId).orElseThrow();
        c.setUsername(usernameGetter.username(authentication));

        log.debug("committing : \n" + connectionId);

        return connSvc.commit(c);
    }


    @RequestMapping(value = "/protected/conn/release", method = RequestMethod.POST)
    @ResponseBody
    public ConnChangeResult release(@RequestBody String connectionId) throws StartupException, ConnException {
        this.checkStartup();

        Connection c = connSvc.findConnection(connectionId).orElseThrow();

        if (c.getPhase().equals(Phase.ARCHIVED)) {
            throw new ConnException("Cannot cancel ARCHIVED connection");
        } else {
            Optional<NsiMapping> om = nsiMappingService.getMappingForOscarsId(c.getConnectionId());
            om.ifPresent(nsiMapping -> nsiSvc.forcedEnd(nsiMapping));
            return connSvc.release(c);
        }
    }

    @RequestMapping(value = "/protected/conn/mode/{connectionId:.+}", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public Connection setMode(@PathVariable String connectionId, @RequestBody String mode)
            throws StartupException, ConnException {
        this.checkStartup();
        Connection c = connSvc.findConnection(connectionId).orElseThrow();
        if (!c.getPhase().equals(Phase.RESERVED)) {
            throw new ConnException("invalid phase: " + c.getPhase() + " for connection " + connectionId);
        }
        log.info(c.getConnectionId() + " setting build mode to " + mode);
        c.setMode(BuildMode.valueOf(mode));
        connRepo.save(c);
        return c;
    }


    @RequestMapping(value = "/protected/conn/state/{connectionId:.+}", method = RequestMethod.POST)
    @Transactional
    public void setState(@PathVariable String connectionId, @RequestBody String state)
            throws StartupException {
        this.checkStartup();

        Connection c = connSvc.findConnection(connectionId).orElseThrow();
        log.info(c.getConnectionId() + " overriding state to " + state);
        c.setState(State.valueOf(state));
        connRepo.save(c);

    }

    @RequestMapping(value = "/api/conn/info/{connectionId:.+}", method = RequestMethod.GET)
    @ResponseBody
    public Connection info(@PathVariable String connectionId) throws StartupException, NoSuchElementException {
        this.checkStartup();
        return connSvc.findConnection(connectionId).orElseThrow(NoSuchElementException::new);
    }

    @RequestMapping(value = "/api/conn/history/{connectionId:.+}", method = RequestMethod.GET)
    @ResponseBody
    public List<RouterCommandHistory> history(@PathVariable String connectionId) throws StartupException, ConnException {
        this.checkStartup();
        if (connectionId == null || connectionId.isEmpty()) {
            log.info("no connectionId!");
            throw new ConnException("no connectionId");
        }
//        log.info("looking for connectionId "+ connectionId);
        return historyRepo.findByConnectionId(connectionId);
    }

    @RequestMapping(value = "/api/conn/list", method = RequestMethod.POST)
    @ResponseBody
    public ConnectionList list(@RequestBody ConnectionFilter filter) throws StartupException {

        return connSvc.filter(filter);
    }


    private void checkStartup() throws StartupException {
        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }
    }


}