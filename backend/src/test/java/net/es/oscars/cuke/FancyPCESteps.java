package net.es.oscars.cuke;

import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.pce.PceLibrary;
import net.es.oscars.topo.beans.TopoAdjcy;
import net.es.oscars.topo.beans.TopoUrn;
import net.es.oscars.topo.svc.TopoService;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;


import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Category({UnitTests.class})
public class FancyPCESteps extends CucumberSteps {
    @Autowired
    private TopoService topoService;

    @When("^I ask for all paths from \"([^\"]*)\" to \"([^\"]*)\" with a relaxation radius of (\\d+)$")
    public void i_ask_for_all_paths_from_to_with_maximum_length_of(String a, String z, int radius) throws Throwable {
        Map<String, TopoUrn> urnMap = topoService.getTopoUrnMap();
        assert urnMap.containsKey(a);
        assert urnMap.containsKey(z);

        List<TopoAdjcy> topoAdjcies = topoService.getTopoAdjcies();
        TopoUrn src = topoService.getTopoUrnMap().get(a);
        TopoUrn dst = topoService.getTopoUrnMap().get(z);

        Map<TopoAdjcy, Double> costs = new HashMap<>();
        for (TopoAdjcy adjcy : topoAdjcies) {
            //log.info("adjcy:" + adjcy.asLogString());
            costs.put(adjcy, 1D);
        }
        log.info("adjcies size: "+topoAdjcies.size());

        DirectedWeightedMultigraph<TopoUrn, TopoAdjcy> graph = PceLibrary.makeGraph(topoAdjcies, costs);
        assert graph.containsVertex(src);
        assert graph.containsVertex(dst);

        DijkstraShortestPath<TopoUrn, TopoAdjcy> sp = new DijkstraShortestPath<>(graph);
        GraphPath<TopoUrn, TopoAdjcy> path = sp.getPath(src, dst);

        assert path != null;
        int length = radius + path.getEdgeList().size();

        AllDirectedPaths<TopoUrn, TopoAdjcy> ap = new AllDirectedPaths<>(graph);

        Instant start = Instant.now();
        List<GraphPath<TopoUrn, TopoAdjcy>> paths = ap.getAllPaths(src, dst, true, length);
        Instant end = Instant.now();


        log.info(paths.size()+ " distinct paths found between "+a + " and "+ z+ " found in "+ Duration.between(start, end));

        int i = 1;

    }


}