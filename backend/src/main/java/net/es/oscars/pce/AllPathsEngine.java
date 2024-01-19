package net.es.oscars.pce;

import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PCEException;
import net.es.oscars.pce.beans.PathConstraint;
import net.es.oscars.resv.ent.EroHop;
import net.es.oscars.resv.ent.VlanPipe;
import net.es.oscars.topo.beans.TopoAdjcy;
import net.es.oscars.topo.beans.TopoUrn;
import net.es.oscars.topo.beans.Version;
import net.es.oscars.topo.enums.UrnType;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.oscars.web.beans.PcePath;
import net.es.oscars.web.beans.PceResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@ConditionalOnProperty(name="pce.engine", havingValue="allpaths")

public class AllPathsEngine implements Engine {
    private final TopologyStore topologyStore;

    @Value("#{new Double('${pce.long-path-ratio}')}")
    private Double longPathRatio;

    @Value("${pce.long-path-detour:9}")
    private Integer longPathDetour;

    @Value("${pce.short-path-detour:15}")
    private Integer shortPathDetour;

    private final Map<Pair<TopoUrn, TopoUrn>, List<GraphPath<TopoUrn, TopoAdjcy>>> pathsCache = new HashMap<>();
    private Version cacheTopoVersion = null;

    public AllPathsEngine(TopologyStore topologyStore) {
        this.topologyStore = topologyStore;
    }


    private List<GraphPath<TopoUrn, TopoAdjcy>> cachedPaths
            (TopoUrn src, TopoUrn dst, AllDirectedPaths<TopoUrn, TopoAdjcy> ap, Integer maxLength){
        List<GraphPath<TopoUrn, TopoAdjcy>> paths;


        boolean versionChanged = false;
        Version current = topologyStore.getVersion();

        if (this.cacheTopoVersion == null) {
            versionChanged = true;
        } else if (!this.cacheTopoVersion.getUpdated().equals(current.getUpdated())) {
            versionChanged = true;
        }
        Pair<TopoUrn, TopoUrn> srcdst = Pair.of(src, dst);
        boolean inCache = pathsCache.containsKey(srcdst);

        boolean mustCalculate = false;
        if (versionChanged) {
            mustCalculate = true;
        } else if (!inCache) {
            mustCalculate = true;
        }
        if (mustCalculate) {
            Instant ps = Instant.now();
            paths = ap.getAllPaths(src, dst, true, maxLength);
            Instant pe = Instant.now();
            log.info(paths.size() + " distinct paths found between " + src.getUrn() +
                    " and " + dst.getUrn() + " found in time " + Duration.between(ps, pe));
            this.cacheTopoVersion = current;
            pathsCache.put(srcdst, paths);

        } else {
            paths = pathsCache.get(srcdst);
        }


        return paths;

    }

    public PceResponse calculatePaths(VlanPipe requestPipe,
                                      Map<String, Integer> availIngressBw,
                                      Map<String, Integer> availEgressBw,
                                      PathConstraint constraint) throws PCEException {
        Set<String> exclude = constraint.getExclude();
        List<String> ero = constraint.getEro();

        Map<String, TopoUrn> baseline = topologyStore.getTopoUrnMap();

        List<TopoAdjcy> topoAdjcies = topologyStore.getTopoAdjcies();
        // two kinds of costing; one by hop count, one by metric

        Map<TopoAdjcy, Double> hopCosts = new HashMap<>();
        Map<TopoAdjcy, Double> metricCosts = new HashMap<>();

        for (TopoAdjcy adjcy : topoAdjcies) {
            hopCosts.put(adjcy, 1D);

            double cost = 0;
            for (Long metric : adjcy.getMetrics().values()) {
                if (metric > cost) {
                    cost = metric.doubleValue();
                }
            }
            metricCosts.put(adjcy, cost);
        }


        TopoUrn src = topologyStore.getTopoUrnMap().get(requestPipe.getA().getDeviceUrn());
        TopoUrn dst = topologyStore.getTopoUrnMap().get(requestPipe.getZ().getDeviceUrn());

        if (src == null) {
            throw new PCEException(requestPipe.getA().getDeviceUrn() + " not found in topology");
        }
        if (!src.getUrnType().equals(UrnType.DEVICE)) {
            throw new PCEException(requestPipe.getA().getDeviceUrn() + " must point to a DEVICE but is " + src.getUrnType());
        }

        if (dst == null) {
            throw new PCEException(requestPipe.getZ().getDeviceUrn() + " not found in topology");
        }
        if (!dst.getUrnType().equals(UrnType.DEVICE)) {
            throw new PCEException(requestPipe.getZ().getDeviceUrn() + " must point to a DEVICE but is " + dst.getUrnType());
        }

        // first, get the shortest path (by metric)

        DirectedWeightedMultigraph<TopoUrn, TopoAdjcy> byMetricGraph = PceLibrary.makeGraph(topoAdjcies, metricCosts);
        DirectedWeightedMultigraph<TopoUrn, TopoAdjcy> byHopsGraph = PceLibrary.makeGraph(topoAdjcies, hopCosts);


        PcePath shortest = DijkstraPCE.shortestPath(byMetricGraph, src, dst);
        // first, get the shortest path (by metric)
        PcePath leastHops = DijkstraPCE.shortestPath(byHopsGraph, src, dst);
        PceLibrary.pathBandwidths(shortest, baseline, availIngressBw, availEgressBw);
        PceLibrary.pathBandwidths(leastHops, baseline, availIngressBw, availEgressBw);


        Integer shortestPathLength = shortest.getAzEro().size();

        // dynamic detour: if it's a long path, use a different detour size
        // this is to reduce response time with very long paths
        int maxLength = shortestPathLength + shortPathDetour;

        if (PceLibrary.diameter == null) {
            // cache graph diameter
            PceLibrary.calculateDiameter(topoAdjcies, hopCosts);
        }

        double lengthRatio = shortestPathLength / PceLibrary.diameter;

        if (lengthRatio > longPathRatio) {
            maxLength = shortestPathLength + longPathDetour;
            log.info("long path; using long path max-length (" + maxLength + ")");
        } else {
            log.info("short path; using max-length for short paths (" + maxLength + ") : spl: "+shortestPathLength+" + detour: "+shortPathDetour);

        }

        AllDirectedPaths<TopoUrn, TopoAdjcy> ap = new AllDirectedPaths<>(byMetricGraph);

        List<GraphPath<TopoUrn, TopoAdjcy>> paths = this.cachedPaths(src, dst, ap, maxLength);


        PcePath widestAZ = null;
        PcePath widestSum = null;
        PcePath widestZA = null;
        PcePath fits = null;

        Instant es = Instant.now();
        for (GraphPath<TopoUrn, TopoAdjcy> path : paths) {
            List<EroHop> azEro = PceLibrary.toEro(path);
            if (azEro == null) {
                continue;
            }
            List<String> hopUrnList = new ArrayList<>();
            Set<String> hopUrnSet = new HashSet<>();

            List<EroHop> zaEro = new ArrayList<>();
            for (EroHop hop : azEro) {
                zaEro.add(EroHop.builder().urn(hop.getUrn()).build());
                hopUrnList.add(hop.getUrn());
                hopUrnSet.add(hop.getUrn());
            }

            boolean excludedOk = true;

            if (exclude != null) {
                Set<String> mustBeExcluded = Sets.intersection(exclude, hopUrnSet);
                if (!mustBeExcluded.isEmpty()) {
                    excludedOk = false;
                }
            }

            boolean includedOk = true;

            if (ero != null) {
                List<Integer> indices = new ArrayList<>();
                for (String urn : ero) {
                    indices.add(hopUrnList.indexOf(urn));
                }
                boolean sorted = Ordering.natural().isOrdered(indices);
                if (!sorted) {
                    includedOk = false;
                }
            }

            if (!includedOk || !excludedOk) {
                continue;
            }


            Collections.reverse(zaEro);

            PcePath pcePath = PcePath.builder()
                    .azEro(azEro)
                    .zaEro(zaEro)
                    .build();

            PceLibrary.pathBandwidths(pcePath, baseline, availIngressBw, availEgressBw);

            PceLibrary.pathCost(pcePath, path, metricCosts);

            // path that fits:
            if (pcePath.getAzAvailable() >= requestPipe.getAzBandwidth()
                    && pcePath.getZaAvailable() >= requestPipe.getZaBandwidth()) {
                // accept first path that matches; otherwise prefer the least-cost one
                if (fits == null) {
                    fits = pcePath;
                } else {
                    fits = preferredOf(fits, pcePath);
                }
            }

            // first path is best by default
            if (widestAZ == null) {
                widestAZ = pcePath;
                widestSum = pcePath;
                widestZA = pcePath;
            } else {

                // to get the widest paths, check for larger bandwidth.
                // if bandwidth is equal, then lowest cost; if equal, least hops.
                if (widestAZ.getAzAvailable() < pcePath.getAzAvailable()) {
                    widestAZ = pcePath;
                } else if (widestAZ.getAzAvailable().equals(pcePath.getAzAvailable())) {
                    widestAZ = preferredOf(widestAZ, pcePath);
                }
                if (widestZA.getZaAvailable() < pcePath.getZaAvailable()) {
                    widestZA = pcePath;
                } else if (widestZA.getZaAvailable().equals(pcePath.getZaAvailable())) {
                    widestZA = preferredOf(widestZA, pcePath);
                }

                Integer prevSum = widestSum.getAzAvailable() + widestSum.getZaAvailable();
                Integer newSum = pcePath.getAzAvailable() + pcePath.getZaAvailable();
                if (prevSum < newSum) {
                    widestSum = pcePath;
                } else if (prevSum.equals(newSum)) {
                    widestSum = preferredOf(widestSum, pcePath);
                }
            }

        }
        Instant ee = Instant.now();
        log.info("widest paths found in time " + Duration.between(es, ee));

        return PceResponse.builder()
                .widestAZ(widestAZ)
                .widestZA(widestZA)
                .widestSum(widestSum)
                .shortest(shortest)
                .leastHops(leastHops)
                .fits(fits)
                .evaluated(paths.size())
                .build();
    }


    private PcePath preferredOf(PcePath a, PcePath b) {
        if (a.getCost() < b.getCost()) {
            return a;
        } else if (a.getCost() == b.getCost()) {
            if (a.getAzEro().size() < b.getAzEro().size()) {
                return a;
            }
        }
        return b;
    }

}
