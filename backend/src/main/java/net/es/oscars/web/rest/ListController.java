package net.es.oscars.web.rest;


import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.dto.pss.cmd.CommandType;
import net.es.oscars.model.Interval;
import net.es.oscars.sb.db.RouterCommandsRepository;
import net.es.oscars.sb.ent.RouterCommandHistory;
import net.es.oscars.resv.db.CommandHistoryRepository;
import net.es.oscars.resv.db.LogRepository;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.EventType;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.sb.ent.RouterCommands;
import net.es.topo.common.model.oscars1.IntRange;
import net.es.oscars.topo.enums.CommandParamType;
import net.es.oscars.web.beans.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

import static net.es.oscars.sb.nso.NsoAdapter.NSO_TEMPLATE_VERSION;

@RestController
@Slf4j
public class ListController {
    @Autowired
    private Startup startup;

    @Autowired
    private LogRepository logRepo;

    @Autowired
    private CommandHistoryRepository historyRepo;

    @Autowired
    private RouterCommandsRepository rcRepo;

    @Autowired
    private ConnService connService;

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


    @RequestMapping(value = "/api/list/overlapping", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public Map<String, MinimalConnEntry> overlappingList(@RequestBody IntRange range) throws StartupException {
        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }
        Map<String, MinimalConnEntry> result = new HashMap<>();
        String errMsg = "";
        boolean hasError = false;

        if (range.getFloor() == null) {
            range.setFloor(0);
        } else if (range.getFloor() < 0) {
            hasError = true;
            errMsg += "floor must be >= 0\n";
        }

        if (range.getCeiling() == null) {
            range.setCeiling(0);
        } else if (range.getCeiling() < 0) {
            hasError = true;
            errMsg += "ceiling must be >= 0\n";
        }
        if (range.getCeiling() < range.getFloor()) {
            hasError = true;
            errMsg += "ceiling must be >= 0\n";
        }
        if (hasError) {
            throw new IllegalArgumentException(errMsg);
        }

        Instant beginning = Instant.ofEpochSecond(range.getFloor());
        Instant ending = Instant.ofEpochSecond(range.getCeiling());
        Interval interval = Interval.builder()
                .beginning(beginning)
                .ending(ending)
                .build();

        ConnectionFilter filter = ConnectionFilter.builder()
                .interval(interval)
                .sizePerPage(-1)
                .build();

        ConnectionList list = connService.filter(filter);
        for (Connection c : list.getConnections()) {
            List<RouterCommandHistory> rchList = historyRepo.findByConnectionId(c.getConnectionId());
            Optional<EventLog> maybeLog = logRepo.findByConnectionId(c.getConnectionId());
            List<RouterCommands> routerCommandsList = rcRepo.findByConnectionId(c.getConnectionId());
            String southbound = "RANCID";
            for (RouterCommands rc : routerCommandsList) {
                if (rc.getTemplateVersion().startsWith(NSO_TEMPLATE_VERSION) ||
                        rc.getTemplateVersion().startsWith("NSO")) {
                    southbound = "NSO";
                    break;
                }
            }

            List<MinimalConnEndpoint> endpoints = new ArrayList<>();
            Map<String, List<Integer>> sdps = new HashMap<>();
            Set<List<String>> eros = new HashSet<>();
            Schedule s;
            Components cmp;
            if (c.getPhase().equals(Phase.RESERVED)) {
                s = c.getReserved().getSchedule();
                cmp = c.getReserved().getCmp();
            } else if (c.getPhase().equals(Phase.ARCHIVED)) {
                s = c.getArchived().getSchedule();
                cmp = c.getArchived().getCmp();
            } else {
                log.error("invalid phase for " + c.getConnectionId());
                continue;
            }
            for (VlanJunction j: cmp.getJunctions()) {
                List<Integer> sdpIds = new ArrayList<>();
                for (CommandParam p : j.getCommandParams()) {
                    if (p.getParamType().equals(CommandParamType.ALU_SDP_ID)) {
                        sdpIds.add(p.getResource());
                    }
                }
                if (!sdpIds.isEmpty()) {
                    sdps.put(j.getDeviceUrn(), sdpIds);
                }
            }
            for (VlanFixture f : cmp.getFixtures()) {
                MinimalConnEndpoint ep = MinimalConnEndpoint.builder()
                        .vlan(f.getVlan().getVlanId())
                        .router(f.getJunction().getDeviceUrn())
                        .port(f.getPortUrn().split(":")[1])
                        .build();
                endpoints.add(ep);
            }
            for (VlanPipe p : cmp.getPipes()) {
                List<String> ero = new ArrayList<>();
                for (EroHop h : p.getAzERO()) {
                    ero.add(h.getUrn());
                }
                eros.add(ero);
            }


            MinimalConnEntry e = MinimalConnEntry.builder()
                    .description(c.getDescription())
                    .sdps(sdps)
                    .eros(eros)
                    .southbound(southbound)
                    .endpoints(endpoints)
                    .build();

            Instant firstBuilt = Instant.MAX;
            Instant lastDismantled = Instant.MIN;
            for (RouterCommandHistory rch : rchList) {
                if (rch.getType().equals(CommandType.BUILD)) {
                    if (rch.getDate().isBefore(firstBuilt)) {
                        firstBuilt = rch.getDate();
                    }
                }
                if (rch.getType().equals(CommandType.DISMANTLE)) {
                    if (rch.getDate().isAfter(lastDismantled)) {
                        lastDismantled = rch.getDate();
                    }
                }
            }
            if (firstBuilt.equals(Instant.MAX)) {
                // either it was never built, or, it is migrated
                // in that case use the schedule as a best guess
                Long start = s.getBeginning().getEpochSecond();
                Long end = s.getEnding().getEpochSecond();

                e.setStart(start.intValue());
                e.setEnd(end.intValue());
            } else {
                Long start = firstBuilt.getEpochSecond();
                e.setStart(start.intValue());
                Long end;
                if (lastDismantled.equals(Instant.MIN)) {
                    // maybe it was never dismantled?
                    if (s.getEnding().isBefore(Instant.now())) {
                        // set our end to the schedule end...

                        end = s.getEnding().getEpochSecond();
                        // but see if we have an event about an early cancel
                        if (maybeLog.isPresent()) {
                            EventLog log = maybeLog.get();
                            for (Event ev: log.getEvents()) {
                                if (ev.getType().equals(EventType.CANCELLED)) {
                                    end = ev.getOccurrence().getEpochSecond();
                                }
                            }
                        }
                    } else {
                        end = Instant.now().getEpochSecond();
                    }
                } else {
                    end = lastDismantled.getEpochSecond();
                }
                e.setEnd(end.intValue());

            }




            result.put(c.getConnectionId(), e);
        }

        return result;
    }


}