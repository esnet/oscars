package net.es.oscars.pce;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PCEException;
import net.es.oscars.resv.ent.EroHop;
import net.es.oscars.resv.enums.BwDirection;
import net.es.oscars.resv.svc.ResvLibrary;
import net.es.oscars.topo.beans.TopoAdjcy;
import net.es.oscars.topo.beans.TopoUrn;
import net.es.oscars.topo.enums.UrnType;
import net.es.oscars.web.beans.PcePath;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.GraphMeasurer;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PceLibrary {

    public static Double diameter = null;

    public static void calculateDiameter(List<TopoAdjcy> adjcies, Map<TopoAdjcy, Double> weights) {
        DirectedWeightedMultigraph<TopoUrn, TopoAdjcy> byHopsGraph = PceLibrary.makeGraph(adjcies, weights);
        GraphMeasurer<TopoUrn, TopoAdjcy> gm = new GraphMeasurer<>(byHopsGraph);
        PceLibrary.diameter = gm.getDiameter();

    }


    public static DirectedWeightedMultigraph<TopoUrn, TopoAdjcy>
    makeGraph(List<TopoAdjcy> adjcies, Map<TopoAdjcy, Double> weights) {

        DirectedWeightedMultigraph<TopoUrn, TopoAdjcy> graph
                = new DirectedWeightedMultigraph<>(TopoAdjcy.class);

        for (TopoAdjcy adjcy : adjcies) {
            if (!graph.containsVertex(adjcy.getA())) {
                graph.addVertex(adjcy.getA());
            }
            if (!graph.containsVertex(adjcy.getZ())) {
                graph.addVertex(adjcy.getZ());
            }
            graph.addEdge(adjcy.getA(), adjcy.getZ(), adjcy);

            graph.setEdgeWeight(adjcy, weights.get(adjcy));
        }
        return graph;

    }

    public static void pathCost(PcePath pcePath, GraphPath<TopoUrn, TopoAdjcy> path, Map<TopoAdjcy, Double> costs) {
        double cost = 0D;
        for (TopoAdjcy ta : path.getEdgeList()) {
            cost = cost + costs.get(ta);
        }
        pcePath.setCost(cost);
    }

    public static void pathBandwidths(PcePath pcePath,
                                      Map<String, TopoUrn> baseline,
                                      Map<String, Integer> availIngressBw,
                                      Map<String, Integer> availEgressBw) throws PCEException {

        Map<String, Integer> baselineIngressBw = ResvLibrary
                .availableBandwidthMap(BwDirection.INGRESS, baseline, new HashMap<>());

        Map<String, Integer> baselineEgressBw = ResvLibrary
                .availableBandwidthMap(BwDirection.EGRESS, baseline, new HashMap<>());

        if (baselineEgressBw == null || baselineIngressBw == null) {
            String error = "";
            if (baselineIngressBw == null) {
                error = "baseline ingress is null! ";
            }
            if (baselineIngressBw == null) {
                error += "baseline egress is null! ";
            }
            throw new PCEException(error);
        }


        pcePath.setAzAvailable(Integer.MAX_VALUE);
        pcePath.setZaAvailable(Integer.MAX_VALUE);
        pcePath.setAzBaseline(Integer.MAX_VALUE);
        pcePath.setZaBaseline(Integer.MAX_VALUE);

        List<EroHop> azHops = pcePath.getAzEro();

        for (int i = 0; i < azHops.size(); i++) {
            String urn = azHops.get(i).getUrn();
            Integer base = pcePath.getAzBaseline();
            Integer avail = pcePath.getAzAvailable();

            if (i % 3 == 1) {

                if (!baselineEgressBw.containsKey(urn)) {

                    log.error("problem processing "+urn+" in ERO:" + hopsToLogFormat(azHops));
                    throw new PCEException("Could not locate " + urn + " in baseline egress");
                } else {
                    if (base > baselineEgressBw.get(urn)) {
                        pcePath.setAzBaseline(baselineEgressBw.get(urn));
                    }
                }
                if (!availEgressBw.containsKey(urn)) {
                    log.error("problem processing "+urn+" in ERO:" + hopsToLogFormat(azHops));
                    throw new PCEException("Could not locate " + urn + " in avail egress");
                } else {
                    if (avail > availEgressBw.get(urn)) {
                        pcePath.setAzAvailable(availEgressBw.get(urn));
                    }

                }

            } else if (i % 3 == 2) {
                if (!baselineIngressBw.containsKey(urn)) {
                    log.error("problem processing "+urn+" in ERO:" + hopsToLogFormat(azHops));
                    throw new PCEException("Could not locate " + urn + " in baseline ingress");
                } else {
                    if (base > baselineIngressBw.get(urn)) {
                        pcePath.setAzBaseline(baselineIngressBw.get(urn));
                    }

                }
                if (!availIngressBw.containsKey(urn)) {
                    log.error("problem processing "+urn+" in ERO:" + hopsToLogFormat(azHops));
                    throw new PCEException("Could not locate " + urn + " in avail ingress");
                } else {
                    if (avail > availIngressBw.get(urn)) {
                        pcePath.setAzAvailable(availIngressBw.get(urn));
                    }
                }
            }
        }
        List<EroHop> zaHops = pcePath.getZaEro();

        for (int i = 0; i < zaHops.size(); i++) {
            String urn = zaHops.get(i).getUrn();
            Integer base = pcePath.getZaBaseline();
            Integer avail = pcePath.getZaAvailable();
            if (i % 3 == 1) {
                if (base > baselineEgressBw.get(urn)) {
                    pcePath.setZaBaseline(baselineEgressBw.get(urn));
                }
                if (avail > availEgressBw.get(urn)) {
                    pcePath.setZaAvailable(availEgressBw.get(urn));
                }

            } else if (i % 3 == 2) {
                if (base > baselineIngressBw.get(urn)) {
                    pcePath.setZaBaseline(baselineIngressBw.get(urn));
                }
                if (avail > availIngressBw.get(urn)) {
                    pcePath.setZaAvailable(availIngressBw.get(urn));
                }
            }
        }


        if (pcePath.getAzEro().isEmpty()) {
            pcePath.setAzAvailable(-1);
            pcePath.setZaAvailable(-1);
            pcePath.setAzBaseline(-1);
            pcePath.setZaBaseline(-1);
        }

    }

    public static List<EroHop> toEro(GraphPath<TopoUrn, TopoAdjcy> path) {

        List<EroHop> ero = new ArrayList<>();
        if (path != null && !path.getEdgeList().isEmpty()) {
            int i = 0;
            for (TopoUrn topoUrn: path.getVertexList()) {
                if (i % 3 == 0) {
                    if (!topoUrn.getUrnType().equals(UrnType.DEVICE)) {
                        return null;
                    }
                } else {
                    if (!topoUrn.getUrnType().equals(UrnType.PORT)) {
                        return null;
                    }
                }
                i++;

            }

            path.getVertexList().forEach(v -> {
                ero.add(EroHop.builder().urn(v.getUrn()).build());
            });
        }
        return ero;

    }
    public static String hopsToLogFormat(List<EroHop> ero) {
        String out = "\n";
        for (EroHop hop: ero) {
            out = out + hop.getUrn()+"\n";

        }
        return out;
    }
}
