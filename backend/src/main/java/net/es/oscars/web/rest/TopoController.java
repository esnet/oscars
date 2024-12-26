package net.es.oscars.web.rest;


import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.resv.svc.ResvService;
import net.es.oscars.topo.beans.*;
import net.es.oscars.topo.beans.Device;
import net.es.oscars.topo.beans.Port;
import net.es.oscars.topo.beans.Version;
import net.es.oscars.topo.enums.Layer;
import net.es.oscars.topo.enums.UrnType;
import net.es.oscars.topo.pop.ConsistencyException;
import net.es.oscars.topo.svc.ConsistencyService;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.oscars.model.Interval;
import net.es.oscars.web.beans.SimpleAdjcy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Slf4j
public class TopoController {

    @Autowired
    private TopologyStore topologyStore;

    @Autowired
    private ConsistencyService consistencySvc;

    @Autowired
    private ResvService resvService;

    @Autowired
    private Startup startup;

    // cache these in memory

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

    @RequestMapping(value = "/api/topo/ethernetPortsByDevice", method = RequestMethod.GET)
    @ResponseBody
    @Transactional
    public Map<String, List<Port>> ethernetPortsByDevice()
            throws ConsistencyException, StartupException {
        startup.startupCheck();
        Map<String, List<Port>> eppd = new HashMap<>();


        Topology topology = topologyStore.getTopology();
        if (topology.getVersion() == null) {
            throw new ConsistencyException("null current topology");
        } else {
            log.info("topo id: " + topology.getVersion().getUpdated());
        }

        for (Device d : topology.getDevices().values()) {
            List<Port> ports = new ArrayList<>();
            for (Port p : d.getPorts()) {
                if (p.getCapabilities().contains(Layer.ETHERNET)) {
                    ports.add(p);
                }
            }
            eppd.put(d.getUrn(), ports);
        }
        return eppd;
    }

    @RequestMapping(value = "/api/topo/adjacencies", method = RequestMethod.GET)
    @ResponseBody
    public Set<SimpleAdjcy> adjacencies()
            throws StartupException {
        startup.startupCheck();

        List<TopoAdjcy> topoAdjcies = topologyStore.getTopoAdjcies();


        Set<SimpleAdjcy> simpleAdjcies = new HashSet<>();
        for (TopoAdjcy adjcy : topoAdjcies) {
            if (adjcy.getA().getUrnType().equals(UrnType.PORT) &&
                    adjcy.getZ().getUrnType().equals(UrnType.PORT)) {
                SimpleAdjcy simpleAdjcy = SimpleAdjcy.builder()
                        .a(adjcy.getA().getDevice().getUrn())
                        .b(adjcy.getA().getPort().getUrn())
                        .y(adjcy.getZ().getPort().getUrn())
                        .z(adjcy.getZ().getDevice().getUrn())
                        .build();
                simpleAdjcies.add(simpleAdjcy);
            }
        }

        return simpleAdjcies;
    }


    @RequestMapping(value = "/api/topo/baseline", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, PortBwVlan> baseline() throws StartupException {
        startup.startupCheck();
        return topologyStore.getBaseline();

    }


    @RequestMapping(value = "/api/topo/available", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, PortBwVlan> available(@RequestBody Interval interval) throws StartupException {
        startup.startupCheck();
        return resvService.available(interval, null);

    }

    @RequestMapping(value = "/api/topo/version", method = RequestMethod.GET)
    @ResponseBody
    public Version version() throws StartupException, ConsistencyException {
        startup.startupCheck();
        return topologyStore.getVersion();
    }

    @RequestMapping(value = "/api/topo/report", method = RequestMethod.GET)
    @ResponseBody
    public ConsistencyReport report() throws StartupException {
        startup.startupCheck();
        return consistencySvc.getLatestReport();
    }

    @RequestMapping(value = "/api/topo/locations", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Location> locations() throws ConsistencyException, StartupException {
        startup.startupCheck();

        Topology topology = topologyStore.getTopology();
        Map<String, Location> loc = new HashMap<>();

        for (Device d : topology.getDevices().values()) {
            Location l = Location.builder()
                    .latitude(d.getLatitude())
                    .longitude(d.getLongitude())
                    .location(d.getLocation())
                    .locationId(d.getLocationId())
                    .build();

            loc.put(d.getUrn(), l);
        }

        return loc;
    }


}