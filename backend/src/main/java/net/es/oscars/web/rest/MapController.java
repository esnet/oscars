package net.es.oscars.web.rest;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.topo.beans.*;
import net.es.oscars.topo.db.DeviceRepository;
import net.es.oscars.topo.db.AdjcyRepository;
import net.es.oscars.topo.ent.Device;
import net.es.oscars.topo.ent.Adjcy;
import net.es.oscars.topo.pop.ConsistencyException;
import net.es.oscars.topo.pop.UIPopulator;
import net.es.oscars.topo.svc.TopoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Slf4j
public class MapController {

    @Autowired
    private DeviceRepository deviceRepo;
    @Autowired
    private AdjcyRepository adjcyRepo;
    @Autowired
    private TopoService topoService;

    @Autowired
    private Startup startup;
    @Autowired
    private UIPopulator uiPopulator;

    @ExceptionHandler(StartupException.class)
    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    public void handleStartup(StartupException ex) {
        log.warn("Still in startup");
    }


    @RequestMapping(value = "/api/map", method = RequestMethod.GET)
    @ResponseBody
    public MapGraph getMap() throws ConsistencyException, StartupException {
        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }


        MapGraph g = MapGraph.builder().edges(new ArrayList<>()).nodes(new ArrayList<>()).build();
        Map<String, Position> positionMap = uiPopulator.getPositions().getPositions();

        Topology topology = topoService.currentTopology();

        for (Device d : topology.getDevices().values()) {
            MapNode n = MapNode.builder()
                    .id(d.getUrn())
                    .label(d.getUrn())
                    .title(d.getUrn())
                    .value(1)
                    .type(d.getType().toString())
                    .build();

            if (positionMap.keySet().contains(d.getUrn())) {
                n.setX(positionMap.get(d.getUrn()).getX());
                n.setY(positionMap.get(d.getUrn()).getY());
                n.setFixed(new HashMap<>());
                n.getFixed().put("x", true);
                n.getFixed().put("y", true);

            }
            g.getNodes().add(n);
        }

        Set<String> added = new HashSet<>();
        for (Adjcy pa : topology.getAdjcies()) {

            String edgeId = pa.getA().getUrn() + " -- " + pa.getZ().getUrn();
            String reverseEdgeId = pa.getZ().getUrn() + " -- " + pa.getA().getUrn();
            if (!(added.contains(edgeId) || added.contains(reverseEdgeId))) {
                //
                added.add(edgeId);
                added.add(reverseEdgeId);
                String aNodeId = pa.getA().getDevice();
                String zNodeId = pa.getZ().getDevice();

                Integer bandwidthFloor = topoService.minimalReservableBandwidth(pa);
                String capString = bandwidthFloor + "Mbps";
                if (bandwidthFloor >= 1000) {
                    double minCapDub = (double) bandwidthFloor / 1000;
                    capString = minCapDub + " Gbps";
                }
                String linkTitle = edgeId + ", Capacity: " + System.lineSeparator() + capString;
                MapEdge ve = MapEdge.builder()
                        .from(aNodeId).to(zNodeId)
                        .id(edgeId)
                        .title(linkTitle)
                        .label("")
                        .value(4)
                        .arrows(null).arrowStrikethrough(false).color(null)
                        .build();

                g.getEdges().add(ve);
            }


        }
        return g;
    }

    @RequestMapping(value = "/api/positions", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Position> getPositions() throws StartupException {
        if (startup.isInStartup()) {
            throw new StartupException("OSCARS starting up");
        } else if (startup.isInShutdown()) {
            throw new StartupException("OSCARS shutting down");
        }
        return uiPopulator.getPositions().getPositions();

    }

}