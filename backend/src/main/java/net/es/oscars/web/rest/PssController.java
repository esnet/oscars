package net.es.oscars.web.rest;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.PSSException;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.dto.pss.cmd.CommandType;
import net.es.oscars.dto.pss.cmd.GeneratedCommands;
import net.es.oscars.sb.SouthboundTask;
import net.es.oscars.sb.beans.QueueName;
import net.es.oscars.sb.db.RouterCommandsRepository;
import net.es.oscars.sb.ent.RouterCommands;
import net.es.oscars.sb.SouthboundQueuer;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.ent.Tag;
import net.es.oscars.resv.enums.BuildMode;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.enums.State;
import net.es.oscars.web.beans.PssWorkStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;


@RestController
@Slf4j
public class PssController {
    @Autowired
    private Startup startup;
    @Autowired
    private SouthboundQueuer southboundQueuer;

    @Autowired
    private RouterCommandsRepository rcRepo;

    @Autowired
    private ConnectionRepository connRepo;

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

    // TODO: deprecate this
    @RequestMapping(value = "/protected/pss/commands/{connectionId:.+}/{deviceUrn}", method = RequestMethod.GET)
    @ResponseBody
    public List<RouterCommands> commands(@PathVariable String connectionId, @PathVariable String deviceUrn) throws StartupException {
        this.checkStartup();
        return rcRepo.findByConnectionIdAndDeviceUrn(connectionId, deviceUrn);
    }

    @RequestMapping(value = "/api/pss/generated/{connectionId:.+}/{deviceUrn}", method = RequestMethod.GET)
    @ResponseBody
    public GeneratedCommands generated(@PathVariable String connectionId, @PathVariable String deviceUrn)
            throws StartupException {
        this.checkStartup();

        GeneratedCommands gc = GeneratedCommands.builder()
                .device(deviceUrn)
                .generated(new HashMap<>())
                .build();

        for (RouterCommands rc : rcRepo.findByConnectionIdAndDeviceUrn(connectionId, deviceUrn)) {
            gc.getGenerated().put(rc.getType(), rc.getContents());
        }
        return gc;
    }


    @RequestMapping(value = "/protected/pss/commands/{connectionId:.+}", method = RequestMethod.GET)
    @ResponseBody
    public List<RouterCommands> commands(@PathVariable String connectionId) throws StartupException {
        this.checkStartup();
        return rcRepo.findByConnectionId(connectionId);

    }



    @RequestMapping(value = "/protected/pss/work_status/{connectionId:.+}", method = RequestMethod.GET)
    @ResponseBody
    public PssWorkStatus working(@PathVariable String connectionId) throws StartupException, PSSException {
        this.checkStartup();

        Optional<Connection> maybeC = connRepo.findByConnectionId(connectionId);
        if (!maybeC.isPresent()) {
            throw new NoSuchElementException("connection not found");

        } else {
            PssWorkStatus pwt = PssWorkStatus.builder()
                    .connectionId(connectionId)
                    .build();

            // first check if there are running tasks for this connection id; if so, return that
            for (SouthboundTask t : southboundQueuer.entries(QueueName.RUNNING)) {
                if (t.getConnectionId().equals(connectionId)) {
                    pwt.setNext(t.getIntent());
                    pwt.setWork(QueueName.RUNNING);
                    pwt.setExplanation("Currently working to "+t.getCommandType().toString());
                    return pwt;
                }
            }

            // otherwise check if waiting, then return that
            for (SouthboundTask t : southboundQueuer.entries(QueueName.WAITING)) {
                if (t.getConnectionId().equals(connectionId)) {
                    pwt.setNext(t.getIntent());
                    pwt.setWork(QueueName.WAITING);
                    pwt.setExplanation("Waiting in line; next action will be to "+t.getCommandType().toString());
                    return pwt;
                }
            }
            // otherwise return idle

            pwt.setExplanation("PSS is idle");
            pwt.setNext(null);
            pwt.setWork(null);
            return pwt;

        }

    }


    @RequestMapping(value = "/protected/pss/regenerate/{connectionId:.+}", method = RequestMethod.GET)
    @ResponseBody
    @Transactional
    public void regenerate(@PathVariable String connectionId) throws StartupException, PSSException {
        this.checkStartup();
        Optional<Connection> maybeC = connRepo.findByConnectionId(connectionId);
        if (!maybeC.isPresent()) {
            throw new NoSuchElementException("connection not found");

        } else {
            Connection c = maybeC.get();
            if (!c.getPhase().equals(Phase.RESERVED)) {
                throw new PSSException("can only regenerate for connections in RESERVED phase");
            }
            for (Tag t : c.getTags()) {
                if (t.getCategory().equals("migrated")) {
                    throw new PSSException("Regeneration not allowed for migrated connections");
                }
            }
        }

        List<RouterCommands> rc = rcRepo.findByConnectionId(connectionId);
        rcRepo.deleteAll(rc);

    }


    @RequestMapping(value = "/protected/pss/build/{connectionId:.+}", method = RequestMethod.GET)
    @ResponseBody
    public void build(@PathVariable String connectionId) throws StartupException, PSSException {
        this.checkStartup();
        Optional<Connection> cOpt = connRepo.findByConnectionId(connectionId);
        if (!cOpt.isPresent()) {
            throw new NoSuchElementException();
        } else {
            Connection c = cOpt.get();
            if (!c.getPhase().equals(Phase.RESERVED)) {
                throw new PSSException("invalid connection phase");
            } else if (!c.getMode().equals(BuildMode.MANUAL)) {
                throw new PSSException("build mode not manual");
            } else if (c.getState().equals(State.FAILED)) {
                throw new PSSException("state is FAILED");
            } else if (c.getState().equals(State.FINISHED)) {
                throw new PSSException("state is FINISHED");
            } else if (c.getState().equals(State.ACTIVE)) {
                return;
            } else if (c.getReserved().getSchedule().getBeginning().isAfter(Instant.now())) {
                throw new PSSException("cannot build before begin time");

            } else if  (c.getReserved().getSchedule().getEnding().isBefore(Instant.now())) {
                throw new PSSException("cannot build after end time");
            }
            southboundQueuer.add(CommandType.BUILD, c.getConnectionId(), State.ACTIVE);

        }
    }
    @RequestMapping(value = "/protected/pss/dismantle/{connectionId:.+}", method = RequestMethod.GET)
    @ResponseBody
    @Transactional
    public void dismantle(@PathVariable String connectionId) throws StartupException, PSSException {
        this.checkStartup();
        Optional<Connection> cOpt = connRepo.findByConnectionId(connectionId);
        if (!cOpt.isPresent()) {
            throw new NoSuchElementException();
        } else {
            Connection c = cOpt.get();
            if (!c.getPhase().equals(Phase.RESERVED)) {
                throw new PSSException("invalid connection phase");
            } else if (!c.getMode().equals(BuildMode.MANUAL)) {
                throw new PSSException("build mode not manual");
            } else if (!c.getState().equals(State.ACTIVE)) {
                return;
            } else if (c.getReserved().getSchedule().getBeginning().isAfter(Instant.now())) {
                throw new PSSException("cannot dismantle before begin time");

            } else if  (c.getReserved().getSchedule().getEnding().isBefore(Instant.now())) {
                throw new PSSException("cannot dismantle after end time");
            }
            southboundQueuer.add(CommandType.DISMANTLE, c.getConnectionId(), State.ACTIVE);


        }

    }



    @RequestMapping(value = "/api/pss/opStatusCommands/{connectionId:.+}", method = RequestMethod.GET)
    public List<RouterCommands> operationalStatusCommands(@PathVariable String connectionId) throws StartupException {
        this.checkStartup();
        List<RouterCommands> allCommands = rcRepo.findByConnectionId(connectionId);

        List<RouterCommands> result = new ArrayList<>();
        allCommands.forEach(c -> {
            if (c.getType().equals(CommandType.OPERATIONAL_STATUS)) {
                result.add(c);
            }
        });

        return result;

    }

    private void checkStartup() throws StartupException {

        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }


    }


}