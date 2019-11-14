package net.es.oscars.web.rest;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.resv.db.*;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.ConnectionMode;
import net.es.oscars.resv.enums.EventType;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.resv.svc.LogService;
import net.es.oscars.web.beans.ConnException;
import net.es.oscars.web.beans.CurrentlyHeldEntry;
import net.es.oscars.web.beans.HoldException;
import net.es.oscars.web.beans.Interval;
import net.es.oscars.web.simple.SimpleConnection;
import net.es.oscars.web.simple.Validity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;


@RestController
@Slf4j
public class HoldController {
    @Autowired
    private LogService logService;
    @Autowired
    private Startup startup;

    @Autowired
    private ConnectionRepository connRepo;


    @Autowired
    private ConnService connSvc;

    @Value("${resv.timeout}")
    private Integer resvTimeout;

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

    @RequestMapping(value = "/protected/extend_hold/{connectionId:.+}", method = RequestMethod.GET)
    @Transactional
    public Instant extendHold(@PathVariable String connectionId)
            throws StartupException {

        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }
        Optional<Connection> maybeConnection = connRepo.findByConnectionId(connectionId);

        Instant exp = Instant.now().plus(resvTimeout, ChronoUnit.SECONDS);

        if (maybeConnection.isPresent()) {
            Connection conn = maybeConnection.get();
            if (conn.getPhase().equals(Phase.HELD)) {

                conn.getHeld().setExpiration(exp);
                connRepo.save(conn);

                return exp;
            } else {
                return Instant.MIN;

            }

        } else {
            return Instant.MIN;
        }

    }


    @RequestMapping(value = "/protected/held/current", method = RequestMethod.GET)
    @ResponseBody
    @Transactional
    public List<CurrentlyHeldEntry> currentlyHeld()  throws StartupException {
        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }
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
        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }
        Optional<Connection> c = connRepo.findByConnectionId(connectionId);
        if (!c.isPresent()){
            throw new IllegalArgumentException("connection not found for "+connectionId);
        } else if (!c.get().getPhase().equals(Phase.HELD)) {
            throw new IllegalArgumentException("connection not in HELD phase for "+connectionId);
        } else {
            connRepo.delete(c.get());
        }

    }



    @RequestMapping(value = "/protected/cloneable", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public SimpleConnection cloneable(Authentication authentication,
                                      @RequestBody SimpleConnection connection)
            throws ConnException {

        int duration = connection.getEnd() - connection.getBegin();

        // try to get starting now() with same duration
        String username = authentication.getName();
        connection.setUsername(username);

        Instant now = Instant.now();

        connection.setBegin(new Long(now.getEpochSecond()).intValue());
        connection.setEnd(connection.getBegin()+duration);
        Validity v = connSvc.validate(connection, ConnectionMode.CLONE);
        connection.setValidity(v);

        return connection;

    }

    @RequestMapping(value = "/protected/hold", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public SimpleConnection hold(Authentication authentication,
                                 @RequestBody SimpleConnection in)
            throws StartupException, ConnException {

        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }

        // TODO: Don't throw exception; populate all the Validity entries instead
        Validity v = connSvc.validate(in, ConnectionMode.NEW);
        if (!v.isValid()) {
            in.setValidity(v);
            log.info("did not update invalid connection "+in.getConnectionId());
            log.info("reason: "+v.getMessage());
            return in;
        }

        String username = authentication.getName();
        in.setUsername(username);

        Instant exp = Instant.now().plus(resvTimeout, ChronoUnit.SECONDS);
        Long secs = exp.toEpochMilli() / 1000L;
        in.setHeldUntil(secs.intValue());

        String connectionId = in.getConnectionId();

        // String prettyNew = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(in);
        // log.debug("incoming conn: \n" + prettyNew);

        Optional<Connection> maybeConnection = connRepo.findByConnectionId(connectionId);
        if (maybeConnection.isPresent()) {
            Connection prev = maybeConnection.get();
            if (!prev.getPhase().equals(Phase.HELD)) {
                throw new IllegalArgumentException("connection not in HELD phase");
            }

            log.info("overwriting previous connection for " + connectionId);
            // String prettyPrv = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(prev);
            // log.debug("prev conn: "+prev.getId()+"\n" + prettyPrv);

            connSvc.updateConnection(in, prev);

            // String prettyUpd = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(prev);
            // log.debug("updated conn: "+prev.getId()+"\n" + prettyUpd);

            connRepo.save(prev);
        } else {
            log.info("saving new connection " + connectionId);
            Event ev = Event.builder()
                    .connectionId(connectionId)
                    .description("created")
                    .type(EventType.CREATED)
                    .at(Instant.now())
                    .username("")
                    .build();
            logService.logEvent(in.getConnectionId(), ev);
            Connection c = connSvc.toNewConnection(in);

            // String pretty = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(c);
            // log.debug("new conn:\n" + pretty);
            connRepo.save(c);
        }

        return in;
    }

    // TODO: at 1.1 implement this
    @RequestMapping(value = "/protected/pcehold", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public SimpleConnection pceHold(Authentication authentication,
                                 @RequestBody SimpleConnection in)
            throws StartupException, ConnException {

        return this.hold(authentication, in);
    }


}