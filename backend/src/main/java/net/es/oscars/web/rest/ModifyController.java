package net.es.oscars.web.rest;


import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.model.Interval;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.ConnectionSouthbound;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.resv.svc.LogService;
import net.es.oscars.web.beans.*;
import net.es.oscars.web.simple.Validity;
import net.es.topo.common.devel.DevelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;


@RestController
@Slf4j
public class ModifyController {
    @Autowired
    private LogService logService;
    @Autowired
    private Startup startup;

    @Autowired
    private ConnectionRepository connRepo;


    @Autowired
    private ConnService connSvc;


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

    @RequestMapping(value = "/protected/modify/description", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public ModifyResponse modifyDescription(@RequestBody DescriptionModifyRequest request)
            throws StartupException, NoSuchElementException {
        this.checkStartup();

        boolean success = false;
        String explanation = "";
        Connection c = connSvc.findConnection(request.getConnectionId()).orElseThrow(NoSuchElementException::new);

        DevelUtils.dumpDebug("modify description", request);
        if (request.getDescription().isEmpty()) {
            explanation = "Description null or empty";
        } else {
            c.setDescription(request.getDescription());
            if (c.getPhase().equals(Phase.RESERVED)) {
                connRepo.save(c);
            }
            success = true;
        }

        return ModifyResponse.builder()
                .success(success)
                .explanation(explanation)
                .connection(c)
                .build();
    }

    @RequestMapping(value = "/api/valid/schedule", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public ScheduleRangeResponse validSchedule(@RequestBody ScheduleRangeRequest request)
            throws StartupException {
        this.checkStartup();

        boolean allowed = false;
        String explanation = "";

        Instant floor = Instant.now();
        Instant ceiling = Instant.now();
        try {
            Connection c = connSvc.findConnection(request.getConnectionId()).orElseThrow(NoSuchElementException::new);
            if (c.getPhase() == Phase.RESERVED) {
                switch (request.getType()) {
                    case END -> {
                        // TODO: make those hardcoded time amounts configurable

                        // we can only modify the ending time to past the current time
                        // therefore we leave response.beginning to now().

                        // note that we can't use anything larger than ChronoUnit.DAYS in Instant.plus()
                        ceiling = ceiling.plus(365 * 5, ChronoUnit.DAYS);

                        c.getReserved().getSchedule().setEnding(ceiling);

                        Validity v = connSvc.verifyModification(c);
                        allowed = v.isValid();
                        if (!allowed) {
                            explanation = v.getMessage();
                        } else {
                            floor = c.getReserved().getSchedule().getBeginning().plus(300, ChronoUnit.SECONDS);
                        }

                    }
                    case BEGIN -> {
                        explanation = "changing start time currently unsupported";
                    }
                }
            } else {
                explanation = "connection " + request.getConnectionId() + " not in RESERVED phase";
            }

        } catch (NoSuchElementException ex) {
            explanation = "connection " + request.getConnectionId() + " not found";
        }

        return ScheduleRangeResponse.builder()
                .allowed(allowed)
                .explanation(explanation)
                .floor(floor)
                .ceiling(ceiling)
                .connectionId(request.getConnectionId())
                .type(request.getType())
                .build();
    }

    @RequestMapping(value = "/protected/modify/schedule", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public ModifyResponse modifySchedule(@RequestBody ScheduleModifyRequest request)
            throws StartupException, ConnException {
        this.checkStartup();

        DevelUtils.dumpDebug("sch modify", request);

        boolean success = false;
        String explanation = "";
        Connection c = null;


        if (request.getType().equals(ScheduleModifyType.BEGIN)) {
            explanation = "modifying start time is not allowed at this point";
        } else {

            // TODO: migrate away from a unix timestamp
            Instant inOneMinute = Instant.now().plus(60, ChronoUnit.SECONDS);
            Instant requestedEnding = Instant.ofEpochMilli(request.getTimestamp() * 1000);

            try {
                c = connSvc.findConnection(request.getConnectionId()).orElseThrow(NoSuchElementException::new);
                if (!c.getSouthbound().equals(ConnectionSouthbound.NSO)) {
                    throw new ConnException("Connection southbound must be NSO");
                }

                // silently adjust requested end timestamp to now + 1 minute if requested to make it shorter
                if (requestedEnding.isBefore(inOneMinute)) {
                    requestedEnding = inOneMinute;
                }

                try {
                    connSvc.modifySchedule(c, c.getReserved().getSchedule().getBeginning(), requestedEnding);
                    success = true;
                    explanation = "Modification successful";
                } catch (ModifyException ex) {
                    explanation = "Schedule modification failed: " + ex.getMessage();
                }

            } catch (NoSuchElementException ex) {
                explanation = "connection " + request.getConnectionId() + " not found";
            }
        }

        return ModifyResponse.builder()
                .success(success)
                .explanation(explanation)
                .connection(c)
                .build();

    }

    @RequestMapping(value = "/protected/modify/bandwidth", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public ModifyResponse modifyBandwidth(@RequestBody BandwidthModifyRequest request)
            throws StartupException {
        this.checkStartup();

        DevelUtils.dumpDebug("modify request", request);

        boolean success = false;
        String explanation = "";
        Connection c = null;

        try {
            c = connSvc.findConnection(request.getConnectionId()).orElseThrow(NoSuchElementException::new);
            if (c.getPhase() == Phase.RESERVED) {
                try {
                    connSvc.modifyBandwidth(c, request.getBandwidth());
                    success = true;
                    explanation = "Modification successful";
                } catch (ModifyException ex) {
                    explanation = "Modification failed" + ex.getMessage();
                }
            } else {
                explanation = "connection " + request.getConnectionId() + " not in RESERVED phase";
            }

        } catch (NoSuchElementException ex) {
            explanation = "connection " + request.getConnectionId() + " not found";
        }

        return ModifyResponse.builder()
                .success(success)
                .explanation(explanation)
                .connection(c)
                .build();

    }


    @RequestMapping(value = "/api/valid/bandwidth", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public BandwidthRangeResponse validBandwidth(@RequestBody BandwidthRangeRequest request)
            throws StartupException, ModifyException {
        this.checkStartup();

        boolean allowed = false;
        String explanation = "";
        int floor = 0;
        int ceiling = 0;


        try {
            Connection c = connSvc.findConnection(request.getConnectionId()).orElseThrow(NoSuchElementException::new);
            if (c.getPhase() == Phase.RESERVED) {
                allowed = true;
                Interval interval = Interval.builder()
                        .beginning(c.getReserved().getSchedule().getBeginning())
                        .ending(c.getReserved().getSchedule().getEnding())
                        .build();
                ceiling = connSvc.findAvailableMaxBandwidth(c, c.getReserved().getCmp(), interval);

            } else {
                explanation = "connection " + request.getConnectionId() + " not in RESERVED phase";
            }

        } catch (NoSuchElementException ex) {
            explanation = "connection " + request.getConnectionId() + " not found";
        }

        BandwidthRangeResponse rr = BandwidthRangeResponse.builder()
                .allowed(allowed)
                .explanation(explanation)
                .floor(floor)
                .ceiling(ceiling)
                .connectionId(request.getConnectionId())
                .build();
        DevelUtils.dumpDebug("bw range response", rr);
        return rr;

    }

    private void checkStartup() throws StartupException {

        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }


    }


}