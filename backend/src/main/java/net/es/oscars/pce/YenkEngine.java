package net.es.oscars.pce;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PCEException;
import net.es.oscars.pce.beans.PathConstraint;
import net.es.oscars.pce.beans.YenkEdge;
import net.es.oscars.pce.beans.YenkVertex;
import net.es.oscars.resv.ent.EroHop;
import net.es.oscars.resv.ent.VlanPipe;
import net.es.oscars.topo.beans.TopoUrn;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.enums.Layer;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.oscars.web.beans.PcePath;
import net.es.oscars.web.beans.PceResponse;
import net.es.topo.common.devel.DevelUtils;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.PathValidator;
import org.jgrapht.alg.shortestpath.YenKShortestPath;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(name="pce.engine", havingValue="yenk")
public class YenkEngine implements Engine {

    @Value("${pce.timeout:5}")
    private int timeout;
    final TopologyStore topologyStore;

    public YenkEngine(TopologyStore topologyStore) {
        this.topologyStore = topologyStore;
    }

    @Override
    public PceResponse calculatePaths(VlanPipe requestPipe,
                                      Map<String, Integer> availIngressBw,
                                      Map<String, Integer> availEgressBw,
                                      PathConstraint constraint) throws PCEException {

        Instant es = Instant.now();

        Integer pathForwardMbps = requestPipe.getAzBandwidth();
        Integer pathReverseMbps = requestPipe.getZaBandwidth();

        Topology topology = topologyStore.getTopology();
        Map<String, TopoUrn> baseline = topologyStore.getTopoUrnMap();


        SimpleWeightedGraph<YenkVertex, YenkEdge> yenkGraphByMplsMetric = YenkGraphFactory.yenkGraph(topology, availIngressBw, availEgressBw, Layer.MPLS);
        Map<String, YenkVertex> vertexMap = new HashMap<>();
        for (String hop:  constraint.getEro()) {
            TopoUrn topoUrn = baseline.get(hop);
            switch (topoUrn.getUrnType()) {
                case DEVICE -> vertexMap.put(hop, YenkVertex.builder().urn(hop).type(YenkVertex.Type.DEVICE).build());
                case PORT -> vertexMap.put(hop, YenkVertex.builder().urn(hop).type(YenkVertex.Type.PORT).build());
            }
        }
        Set<String> excludedSoFar = new HashSet<>();
        if (constraint.getExclude() != null) {
            excludedSoFar.addAll(constraint.getExclude());
        }
        DevelUtils.dumpDebug("constraint ", constraint);

        /* how we handle pathfinding:
        - iterate the hops in the requested ERO up to the next-to-last one.
        - find a path between that hop, and the next one in the sequence.

        i.e: ERO is     alpha-cr6           // first element guaranteed to be a device
                        gamma-cr6
                        gamma-cr6:2/1/2
                        ...
                        delta-cr6
                        omega-cr6           // last element guaranteed to be a device

         First pair: 'alpha-cr6' to 'gamma-cr6':  we fire up our algorithm with those vertices as A and Z.
         We retrieve the partial path found, if any, and mark all of the path's vertices for exclusion from
         further pathfinding.

         Second pair: 'gamma-cr6' to 'gamma-cr6:2/1/2':  are immediately adjacent, and we don't need (or want)
         to fire up a slow algo to give us that. We add `gamma-cr6` and 'gamma-cr6:2/1/2' to the exclusion set.

         We continue adding to our partial path and the exclusion set, until the last ERO hop-pair,
         where we do our final pathfinding between 'delta-cr6' and 'omega-cr6'.

         */
        List<YenkVertex> path = new ArrayList<>();
        int invoked = 0;
        double cost = 0;
        boolean failed = false;

        // we know we have at least two elements in our ERO so this should not overrun
        for (int i = 0; i <= constraint.getEro().size() -2; i++) {
            YenkVertex a = vertexMap.get(constraint.getEro().get(i));
            YenkVertex z = vertexMap.get(constraint.getEro().get(i+1));

            // immediately adjacent, no need to fire up an algorithm for this
            if (yenkGraphByMplsMetric.containsEdge(a, z)) {
                log.info("direct edge: "+a.getUrn()+" -- "+z.getUrn());
                DevelUtils.dumpDebug("excludedSoFar ", excludedSoFar);

                YenkEdge e = yenkGraphByMplsMetric.getEdge(a, z);
                if (!edgeAllowed(e, a, pathForwardMbps, pathReverseMbps, excludedSoFar)) {
                    failed = true;
                    break;
                } else {
                    if (!path.contains(a)) {
                        path.add(a);
                    }
                    if (!path.contains(z)) {
                        path.add(z);
                    }
                    excludedSoFar.add(a.getUrn());
                    excludedSoFar.add(z.getUrn());
                    cost += 10;
                }
            } else {
                log.info("pathfinding indirect edge: "+a.getUrn()+" -- "+z.getUrn());
                DevelUtils.dumpDebug("excludedSoFar ", excludedSoFar);

                ExecutorService executor = Executors.newSingleThreadExecutor();
                FutureTask<GraphPath<YenkVertex, YenkEdge>> pathComputation = new FutureTask<>(() ->
                        getYenkPath(yenkGraphByMplsMetric, a, z, pathForwardMbps, pathReverseMbps, excludedSoFar));

                GraphPath<YenkVertex, YenkEdge> partialPath = null;
                try {
                    executor.execute(pathComputation);
                    partialPath = pathComputation.get(timeout, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    failed = true;
                    log.info("timed out calculating path");
                    pathComputation.cancel(true);
                } catch (Exception e) {
                    failed = true;
                    log.error(e.getMessage());
                    // handle other exceptions
                } finally {
                    executor.shutdownNow();
                }

                invoked++;
                if (partialPath == null) {
                    failed = true;
                    break;

                } else {
                    partialPath.getVertexList().forEach(v -> {
                        if (!path.contains(v)) {
                            path.add(v);
                        }
                        excludedSoFar.add(v.getUrn());
                    });
                    cost += partialPath.getWeight();

                }
            }
            log.info("pathfinding complete for: "+a.getUrn()+" -- "+z.getUrn());
            DevelUtils.dumpDebug("partial path: ", path);
        }

        PcePath yenkPath = null;

        if (!failed) {
            List<EroHop> azEro = path.stream()
                    .map(vertex -> EroHop.builder().urn(vertex.getUrn()).build())
                    .collect(Collectors.toList());
            List<EroHop> zaEro = new ArrayList<>(azEro);
            Collections.reverse(zaEro);

            yenkPath = PcePath.builder()
                    .azEro(azEro)
                    .zaEro(zaEro)
                    .cost(cost)
                    .build();

            PceLibrary.pathBandwidths(yenkPath, baseline, availIngressBw, availEgressBw);
            DevelUtils.dumpDebug("yenkPath", yenkPath);
        }


        Instant ee = Instant.now();
        log.info("YenK paths found (failed: " + failed + ") in time " + Duration.between(es, ee));

        return PceResponse.builder()
                .evaluated(invoked)
                .widestSum(yenkPath)
                .widestAZ(yenkPath)
                .widestZA(yenkPath)
                .shortest(yenkPath)
                .leastHops(yenkPath)
                .fits(yenkPath)
                .build();
    }




    private GraphPath<YenkVertex, YenkEdge> getYenkPath(SimpleWeightedGraph<YenkVertex, YenkEdge> graph,
                                                        YenkVertex a, YenkVertex z,
                                                        Integer pathForwardMbps, Integer pathReverseMbps,
                                                        Set<String> exclude) {
        log.info("getting yenk path for "+a.getUrn()+" -> "+z.getUrn());
        log.info("forward mbps  "+pathForwardMbps+" reverse "+pathReverseMbps);
        PathValidator<YenkVertex, YenkEdge> pathValidator = BandwidthPathValidator.builder()
                .pathForwardMbps(pathForwardMbps)
                .pathReverseMbps(pathReverseMbps)
                .exclude(exclude)
                .build();

        YenKShortestPath<YenkVertex, YenkEdge> yspByMpls = new YenKShortestPath<>(graph, pathValidator);

        List<GraphPath<YenkVertex, YenkEdge>> pathList = yspByMpls.getPaths(a, z, 1);
        // we just return the shortest path
        if (pathList.isEmpty()) {
            log.info("no path for "+a.getUrn()+" --- "+z.getUrn());
            return null;
        } else {
            return pathList.get(0);
        }

    }

    public static boolean edgeAllowed(YenkEdge edge, YenkVertex thisVertex, Integer pathForwardMbps, Integer pathReverseMbps, Set<String> exclude) {
        /*
        This is a critical function that validates an edge so that it is allowed to be traversed
        by the YenKShortestPaths algo.


        Note that graph is structured like this:
        device --edge-- portVertex --edge-- portVertex --edge-- deviceVertex

        We look at the end of the path so far and figure out what the candidate next vertex is.

        we accept a candidate edge if...
        - both A and Z vertices associated with the edge are not in the exclude set,
        - it has sufficient bandwidth in both directions,
            PARENTHESIS:
                This part is slightly tricky.
                - The pathValidator instance has the pathForwardMbps attribute. It describes
                how much bandwidth we need the edge to have in the forward direction
                as the algo traverses it. We use pathReverseMbps to also simultaneously
                check the reverse direction.
                - The YenkEdge has azBandwidth and zaBandwidth. Those  "A-Z" / "Z-A"
                directional terms are fixed and don't map to the direction the edge
                is currently being traversed by the algorithm. We compare to them or
                their flipped depending on whether we are traversing the edge from its
                A to Z or its Z to A.
            END PARENTHESIS

         */
//        DevelUtils.dumpDebug("edge ", edge);
//        DevelUtils.dumpDebug("exclude ", exclude);

        long edgeForwardCapacity, edgeReverseCapacity;
        YenkVertex nextVertex;
        if (edge.getA().equals(thisVertex)) {
            edgeForwardCapacity = edge.getAzCapacity();
            edgeReverseCapacity = edge.getZaCapacity();
            nextVertex = edge.getZ();
        } else if (edge.getZ().equals(thisVertex)) {
            edgeForwardCapacity = edge.getZaCapacity();
            edgeReverseCapacity = edge.getAzCapacity();
            nextVertex = edge.getA();
        } else {
            // this should never happen
            log.error("bad edge");
            return false;
        }

        if (exclude.contains(nextVertex.getUrn())) {
            return false;
        }

        return edgeForwardCapacity >= pathForwardMbps && edgeReverseCapacity >= pathReverseMbps;

    }


    @Data
    @Builder
    public static class BandwidthPathValidator implements PathValidator<YenkVertex, YenkEdge> {
        int pathForwardMbps;
        int pathReverseMbps;
        Set<String> exclude;

        @Override
        public boolean isValidPath(GraphPath<YenkVertex, YenkEdge> partialPath, YenkEdge e) {


            YenkVertex thisVertex = partialPath.getEndVertex();
            return edgeAllowed(e, thisVertex, pathForwardMbps, pathReverseMbps, exclude);

        }
    }
}
