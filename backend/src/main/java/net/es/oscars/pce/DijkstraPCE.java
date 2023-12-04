package net.es.oscars.pce;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.resv.ent.EroHop;
import net.es.oscars.topo.beans.TopoAdjcy;
import net.es.oscars.topo.beans.TopoUrn;
import net.es.oscars.web.beans.PcePath;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import java.util.*;

@Slf4j
public class DijkstraPCE {

    public static PcePath shortestPath(DirectedWeightedMultigraph<TopoUrn, TopoAdjcy> graph , TopoUrn src, TopoUrn dst) {

        DijkstraShortestPath<TopoUrn, TopoAdjcy> alg = new DijkstraShortestPath<>(graph);
        GraphPath<TopoUrn, TopoAdjcy> path = alg.getPath(src, dst);
        double w = alg.getPathWeight(src,dst);
        log.info("shortest path cost: "+w);
        List<EroHop> azEro = PceLibrary.toEro(path);
        List<EroHop> zaEro = new ArrayList<>();
        for (EroHop hop : azEro) {
            zaEro.add(EroHop.builder().urn(hop.getUrn()).build());
        }

        Collections.reverse(zaEro);

        return PcePath.builder()
                .azEro(azEro)
                .zaEro(zaEro)
                .cost(w)
                .build();

    }



}
