package net.es.oscars.pce;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.pce.beans.YenkEdge;
import net.es.oscars.pce.beans.YenkVertex;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.beans.Adjcy;
import net.es.oscars.topo.enums.Layer;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class YenkGraphFactory {

    public static SimpleWeightedGraph<YenkVertex, YenkEdge> yenkGraph(Topology topology,
                                                                      Map<String, Integer> availIngressBw,
                                                                      Map<String, Integer> availEgressBw,
                                                                      Layer metric) {
        SimpleWeightedGraph<YenkVertex, YenkEdge> graph = new SimpleWeightedGraph<>(YenkEdge.class);
        Set<String> graphPortUrns = new HashSet<>();

        // first iterate all devices and ports
        // each device gets infinite-capacity edges to its ports
        topology.getDevices().forEach((deviceUrn, device) -> {
            YenkVertex dv = YenkVertex.builder().type(YenkVertex.Type.DEVICE).urn(device.getUrn()).build();
            if (!graph.containsVertex(dv)) {
                graph.addVertex(dv);
            }
            device.getPorts().forEach(port -> {
                if (port.getCapabilities().contains(Layer.MPLS)) {
                    graphPortUrns.add(port.getUrn());
                    YenkVertex pv = YenkVertex.builder().type(YenkVertex.Type.PORT).urn(port.getUrn()).build();
                    if (!graph.containsVertex(pv)) {
                        graph.addVertex(pv);
                    }
                    YenkEdge e = YenkEdge.builder().a(dv).z(pv).azCapacity(Long.MAX_VALUE).zaCapacity(Long.MAX_VALUE).build();
                    graph.addEdge(dv, pv, e);
                    graph.setEdgeWeight(e, 0.01);

                }
            });
        });
        // iterate all adjacencies
        // make an edge with capacity of
        topology.getAdjcies().forEach(adjcy -> {
            if (validateAdjacency(adjcy, graphPortUrns, availIngressBw.keySet(), availEgressBw.keySet(), metric)) {
                String aPortUrn = adjcy.getA().getPortUrn();
                String zPortUrn = adjcy.getZ().getPortUrn();
                YenkVertex aPortV = YenkVertex.builder().type(YenkVertex.Type.PORT).urn(aPortUrn).build();
                YenkVertex zPortV = YenkVertex.builder().type(YenkVertex.Type.PORT).urn(zPortUrn).build();
                long aAvailEgress = availEgressBw.get(aPortUrn);
                long aAvailIngress = availIngressBw.get(aPortUrn);
                long zAvailEgress = availEgressBw.get(zPortUrn);
                long zAvailIngress = availIngressBw.get(zPortUrn);

                long azCapacity = aAvailEgress;
                if (zAvailIngress < azCapacity) {
                    azCapacity = zAvailIngress;
                }
                long zaCapacity = zAvailEgress;
                if (aAvailIngress < zaCapacity) {
                    zaCapacity = aAvailIngress;
                }

                YenkEdge e = YenkEdge.builder().a(aPortV).z(zPortV).azCapacity(azCapacity).zaCapacity(zaCapacity).build();
                graph.addEdge(aPortV, zPortV, e);
                if (metric != null) {
                    if (adjcy.getMetrics().containsKey(metric)) {
                        graph.setEdgeWeight(e, adjcy.getMetrics().get(metric));
                    }

                } else {
                    graph.setEdgeWeight(e, 10);
                }

            }
        });

        return graph;
    }

    public static boolean validateAdjacency(Adjcy adjcy,
                                            Set<String> graphPortUrns,
                                            Set<String> ingBwPortUrns,
                                            Set<String> egBwPortUrns,
                                            Layer metric) {
        if (metric != null && !adjcy.getMetrics().containsKey(metric)) {
            log.info("adjacency does not contain " + metric + " metric");
            return false;
        }
        return validateAdjcyPort(adjcy.getA().getPortUrn(), graphPortUrns, ingBwPortUrns, egBwPortUrns) &&
                validateAdjcyPort(adjcy.getZ().getPortUrn(), graphPortUrns, ingBwPortUrns, egBwPortUrns);
    }

    public static boolean validateAdjcyPort(String portUrn, Set<String> graphPortUrns, Set<String> ingBwPortUrns, Set<String> egBwPortUrns) {
        if (!graphPortUrns.contains(portUrn)) {
            log.info("adjacency port " + portUrn + " does not belong to a device");
            return false;
        } else if (!ingBwPortUrns.contains(portUrn)) {
            log.info("adjacency port " + portUrn + " does not have ingress bandwidth info");
            return false;
        } else if (!egBwPortUrns.contains(portUrn)) {
            log.info("adjacency port " + portUrn + " does not have egress bandwidth info");
            return false;
        }
        return true;
    }

}
