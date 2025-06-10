package net.es.oscars.web.rest;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.app.util.UsernameGetter;
import net.es.oscars.resv.db.*;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.ConnectionMode;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.web.beans.BandwidthAvailabilityResponse;
import net.es.oscars.web.beans.ConnException;
import net.es.oscars.web.beans.CurrentlyHeldEntry;
import net.es.oscars.web.simple.SimpleConnection;
import net.es.oscars.web.simple.Validity;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;



@RestController
@Slf4j
public class HoldController {
    private final Startup startup;

    private final ConnectionRepository connRepo;

    private final ConnService connSvc;

    private final UsernameGetter usernameGetter;

    public HoldController(Startup startup, ConnectionRepository connRepo, ConnService connSvc,
                          UsernameGetter usernameGetter) {
        this.startup = startup;
        this.connRepo = connRepo;
        this.connSvc = connSvc;
        this.usernameGetter = usernameGetter;
    }


    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public void handleResourceNotFoundException(NoSuchElementException ex) {
        log.warn("requested an item which did not exist", ex);
    }

    @ExceptionHandler(StartupException.class)
    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    public void handleStartup() {
        log.warn("Still in startup");
    }

    @RequestMapping(value = "/protected/extend_hold/{connectionId:.+}", method = RequestMethod.GET)
    @Transactional
    public Instant extendHold(@PathVariable String connectionId)
            throws StartupException, NoSuchElementException {
        this.checkStartup();


        return connSvc.extendHold(connectionId);
    }


    @RequestMapping(value = "/protected/held/current", method = RequestMethod.GET)
    @ResponseBody
    @Transactional
    public List<CurrentlyHeldEntry> currentlyHeld()  throws StartupException {
        this.checkStartup();

        List<Connection> connections = connRepo.findByPhase(Phase.HELD);
        List<CurrentlyHeldEntry> result = new ArrayList<>();
        for (Connection c: connections) {
            CurrentlyHeldEntry e = CurrentlyHeldEntry.builder()
                    .connectionId(c.getConnectionId())
                    .username(c.getUsername())
                    .build();
            result.add(e);
        }
        return result;
    }

    @RequestMapping(value = "/protected/held/clear/{connectionId:.+}", method = RequestMethod.GET)
    @ResponseBody
    @Transactional
    public void clearHeld(@PathVariable String connectionId)  throws StartupException {
        this.checkStartup();

        connSvc.releaseHold(connectionId);
    }



    @RequestMapping(value = "/protected/cloneable", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public SimpleConnection cloneable(Authentication authentication,
                                      @RequestBody SimpleConnection connection)
            throws ConnException, StartupException {

        this.checkStartup();

        int duration = connection.getEnd() - connection.getBegin();

        connection.setUsername(usernameGetter.username(authentication));
        // try to get starting now() with same duration

        Instant now = Instant.now();

        connection.setBegin(Long.valueOf(now.getEpochSecond()).intValue());
        connection.setEnd(connection.getBegin()+duration);
        Validity v = connSvc.validate(connection, ConnectionMode.CLONE);
        connection.setValidity(v);

        return connection;

    }

    @RequestMapping(value = "/protected/hold", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public SimpleConnection hold(Authentication authentication, @RequestBody SimpleConnection in)
            throws StartupException, ConnException {
        this.checkStartup();

        in.setUsername(usernameGetter.username(authentication));
        Pair<SimpleConnection, Connection> holdResult = connSvc.holdConnection(in);
        return holdResult.getLeft();
    }

    // TODO: at 1.1 implement this
    @RequestMapping(value = "/protected/pcehold", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public SimpleConnection pceHold(Authentication authentication, @RequestBody SimpleConnection in)
            throws StartupException, ConnException {
        return this.hold(authentication, in);
    }

    private void checkStartup() throws StartupException {
        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }
    }

}