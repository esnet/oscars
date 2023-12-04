package net.es.oscars.web.rest;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.app.util.DbAccess;
import net.es.oscars.app.util.UsernameGetter;
import net.es.oscars.resv.db.*;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.ConnectionMode;
import net.es.oscars.resv.enums.EventType;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.resv.svc.LogService;
import net.es.oscars.web.beans.ConnException;
import net.es.oscars.web.beans.CurrentlyHeldEntry;
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
import java.util.concurrent.locks.ReentrantLock;

import static net.es.oscars.resv.svc.ConnUtils.updateConnection;


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


    @Autowired
    private UsernameGetter usernameGetter;

    @Autowired
    private DbAccess dbAccess;

    @Value("${resv.timeout}")
    private Integer resvTimeout;

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

        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }

        Instant expiration = Instant.now().plus(resvTimeout, ChronoUnit.SECONDS);
        ReentrantLock connLock = dbAccess.getConnLock();
        if (connLock.isLocked()) {
            log.debug("connection already locked; extend hold returning");
            return expiration;
        }

        connLock.lock();
        try {
            Optional<Connection> maybeConnection = connRepo.findByConnectionId(connectionId);
            if (maybeConnection.isPresent()) {
                Connection conn = maybeConnection.get();
                if (conn.getPhase().equals(Phase.HELD) && conn.getHeld() != null) {
                    conn.getHeld().setExpiration(expiration);
                    connRepo.save(conn);
                } else {
                    log.debug("pretending to extend hold for a non-HELD connection");
                }
            } else {
                throw new NoSuchElementException("connection not found");
            }

        } finally {
            // log.debug("unlocked connections");
            connLock.unlock();
        }
        return expiration;
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
        if (c.isEmpty()){
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

        in.setUsername(usernameGetter.username(authentication));

        Instant exp = Instant.now().plus(resvTimeout, ChronoUnit.SECONDS);
        long secs = exp.toEpochMilli() / 1000L;
        in.setHeldUntil((int) secs);

        String connectionId = in.getConnectionId();

        // String prettyNew = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(in);
        // log.debug("incoming conn: \n" + prettyNew);

        Optional<Connection> maybeConnection = connRepo.findByConnectionId(connectionId);
        if (maybeConnection.isPresent()) {
            Connection prev = maybeConnection.get();
            // don't throw an error, just return the input. makes sure
            if (!prev.getPhase().equals(Phase.HELD)) {
                return in;
            }

            log.info("overwriting previous connection for " + connectionId);
            // String prettyPrv = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(prev);
            // log.debug("prev conn: "+prev.getId()+"\n" + prettyPrv);

            updateConnection(in, prev);

            // String prettyUpd = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(prev);
            // log.debug("updated conn: "+prev.getId()+"\n" + prettyUpd);

            connRepo.save(prev);
        } else {
            log.info("saving new connection " + connectionId);
            Event ev = Event.builder()
                    .connectionId(connectionId)
                    .description("created")
                    .type(EventType.CREATED)
                    .occurrence(Instant.now())
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