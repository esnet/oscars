package net.es.oscars.pce;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.dto.IntRange;
import net.es.oscars.resv.ent.RequestedVlanFixtureE;
import net.es.oscars.resv.ent.RequestedVlanJunctionE;
import net.es.oscars.resv.ent.RequestedVlanPipeE;
import net.es.oscars.topo.beans.TopoEdge;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.ent.IntRangeE;
import net.es.oscars.topo.ent.ReservableBandwidthE;
import net.es.oscars.topo.ent.ReservableVlanE;
import net.es.oscars.topo.svc.TopoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@Component
public class PruningService {


    public Topology pruneForPipe(Topology topo, RequestedVlanPipeE pipe, List<ReservableBandwidthE> bandwidths,
                                 List<ReservableVlanE> rVlans){
        Integer azBw = pipe.getAzMbps();
        Integer zaBw = pipe.getZaMbps();
        Set<Integer> vlans = new HashSet<>();
        vlans.addAll(getVlansFromJunction(pipe.getAJunction()));
        vlans.addAll(getVlansFromJunction(pipe.getZJunction()));
        return pruneTopology(topo, azBw, zaBw, vlans, bandwidths, rVlans);
    }

    private Set<Integer> getVlansFromJunction(RequestedVlanJunctionE junction){
        return junction.getFixtures().stream().map(RequestedVlanFixtureE::getVlanExpression)
                .map(Integer::parseInt).collect(Collectors.toSet());
    }

    private Topology pruneTopology(Topology topo, Integer azBw, Integer zaBw, Set<Integer> vlans,
                                   List<ReservableBandwidthE> bandwidths, List<ReservableVlanE> rVlans){
        Topology pruned = new Topology();
        pruned.setLayer(topo.getLayer());
        pruned.setVertices(topo.getVertices());
        Set<TopoEdge> availableEdges = topo.getEdges().stream()
                .filter(e -> availableBW(e, azBw, zaBw, bandwidths))
                .filter(e -> availableVlans(e, vlans, rVlans))
                .collect(Collectors.toSet());
        pruned.setEdges(availableEdges);
        return pruned;
    }

    private boolean availableVlans(TopoEdge edge, Set<Integer> vlans, List<ReservableVlanE> rVlans) {
        String aUrn = edge.getA().getUrn();
        String zUrn = edge.getZ().getUrn();
        List<ReservableVlanE> aMatching = rVlans.stream().filter(v -> v.getUrn().getUrn().equals(aUrn))
                .collect(Collectors.toList());
        List<ReservableVlanE> zMatching = rVlans.stream().filter(v -> v.getUrn().getUrn().equals(zUrn))
                .collect(Collectors.toList());

        assert aMatching.size() <= 1 && zMatching.size() <= 1;
        if(aMatching.isEmpty() || zMatching.isEmpty()){
            return true;
        } else{
            Stream<IntRange> aVlanRangeStream = aMatching.get(0).getVlanRanges().stream().map(IntRangeE::toDtoIntRange);
            Stream<IntRange> zVlanRangeStream = zMatching.get(0).getVlanRanges().stream().map(IntRangeE::toDtoIntRange);
            for(Integer requestedVlan : vlans){
                boolean aContainsVlan = aVlanRangeStream.anyMatch(vr -> vr.contains(requestedVlan));
                boolean zContainsVlan = zVlanRangeStream.anyMatch(vr -> vr.contains(requestedVlan));
                if(!aContainsVlan || !zContainsVlan){
                    return false;
                }
            }
            return true;
        }
    }


    private boolean availableBW(TopoEdge edge, Integer azBw, Integer zaBw, List<ReservableBandwidthE> bandwidths){
        String aUrn = edge.getA().getUrn();
        String zUrn = edge.getZ().getUrn();
        log.debug("checking if " + aUrn + " and " + zUrn + " have enough bandwidth ");
        List<ReservableBandwidthE> aMatching = bandwidths.stream().filter(bw -> bw.getUrn().getUrn().equals(aUrn))
                .collect(Collectors.toList());
        List<ReservableBandwidthE> zMatching = bandwidths.stream().filter(bw -> bw.getUrn().getUrn().equals(zUrn))
                .collect(Collectors.toList());

        assert aMatching.size() <= 1 && zMatching.size() <=1;
        if (aMatching.isEmpty() || zMatching.isEmpty()) {
            log.info("bandwidth does not apply to " + aUrn + " or " + zUrn);
            return true;
        } else {
            return aMatching.get(0).getEgressBw() >= azBw && aMatching.get(0).getIngressBw() >= zaBw
                    && zMatching.get(0).getIngressBw() >= azBw && zMatching.get(0).getEgressBw() >= zaBw;

        }
    }

}
