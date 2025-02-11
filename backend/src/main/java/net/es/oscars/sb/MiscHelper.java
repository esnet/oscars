package net.es.oscars.sb;


import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PSSException;
import net.es.oscars.sb.beans.MplsHop;
import net.es.oscars.resv.ent.*;
import net.es.oscars.sb.nso.resv.NsoResvException;
import net.es.oscars.topo.beans.Adjcy;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.topo.common.dto.nso.enums.NsoVplsSdpPrecedence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class MiscHelper {

    @Autowired
    private TopologyStore topologyStore;


    public List<MplsHop> mplsHops(List<EroHop> hops) throws PSSException {

        // eroHops look like this:
        // 0: A
        // 1: A:1/1   +
        // 2: B:2/1   *
        // 3: B
        // 4: B:3/2   +
        // 5: C:4/1   *
        // 6: C
        // 7: C:8/2   +
        // 8: Z:2/1   *
        // 9: Z

        // to construct the MPLS path, we need the IP addresses for the ERO hops marked with *.
        // we get that IP address from the adjacency between the hops marked with + and *
        //

        List<MplsHop> mplsHops = new ArrayList<>();

        // the _output_ hop order field starts at 1 (not 0)
        int order = 1;
        EroHop prevHop = null;
        for (int i = 0; i < hops.size(); i++) {
            if (i % 3 == 1) {
                prevHop = hops.get(i);
            }
            if (i % 3 == 2) {
                EroHop hop = hops.get(i);
                if (prevHop == null) {
                    throw new PSSException("Unexpected null previous hop for " + hop.getUrn());
                }
//                log.debug("hop " + hop.getUrn());
 //               log.debug("prevHop " + prevHop.getUrn());
                String address = this.findAddress(prevHop.getUrn(), hop.getUrn());

                MplsHop mplsHop = MplsHop.builder()
                        .address(address)
                        .order(order)
                        .build();
                order = order + 1;
                mplsHops.add(mplsHop);
            }
        }

        return mplsHops;
    }

    private String findAddress(String aPort, String zPort) throws PSSException {
        for (Adjcy adjcy : topologyStore.getTopology().getAdjcies()) {
            if (adjcy.getA().getPortUrn().equals(aPort)
                    && adjcy.getZ().getPortUrn().equals(zPort)) {
                return adjcy.getZ().getAddr();
            } else if (adjcy.getA().getPortUrn().equals(zPort)
                    && adjcy.getZ().getPortUrn().equals(aPort)) {
                return adjcy.getA().getAddr();
            }
        }
        throw new PSSException("Could not find an adjacency for "+aPort+" -- "+zPort);
    }

    public static Map<String, Set<Integer>> generateAvailableDeviceScopedIdentifiers(
            Set<Integer> allowed, Map<String, Set<Integer>> used, Set<String> devices) {

        Map<String, Set<Integer>> available = new HashMap<>();
        for (String device : devices) {
            Set<Integer> availableOnDevice = new HashSet<>(allowed);
            if (used.containsKey(device)) {
                availableOnDevice.removeAll(used.get(device));
            }
            available.put(device, availableOnDevice);
        }

        return available;
    }


    public static Map<NsoVplsSdpPrecedence, Integer> getUnusedIntResourceFromTwoSets(
            Set<Integer> availableOnA, Set<Integer> availableOnZ, boolean needTwoResources)
            throws NsoResvException {

        Map<NsoVplsSdpPrecedence, Integer> result = new HashMap<NsoVplsSdpPrecedence, Integer>();
        if (needTwoResources) {
            for (Integer resource : availableOnA.stream().sorted().toList()) {
                Integer nextResource = resource + 1;
                boolean nextIsAlsoAvailable = availableOnA.contains(nextResource) && availableOnZ.contains(nextResource);
                if (availableOnZ.contains(resource) && nextIsAlsoAvailable) {
                    result.put(NsoVplsSdpPrecedence.PRIMARY, resource);
                    result.put(NsoVplsSdpPrecedence.SECONDARY, nextResource);
                    return result;
                }
            }

        } else {
            for (Integer resource : availableOnA.stream().sorted().toList()) {
                if (availableOnZ.contains(resource)) {
                    result.put(NsoVplsSdpPrecedence.PRIMARY, resource);
                    return result;
                }
            }
        }
        throw new NsoResvException("unable to locate common unused integer resource(s)");
    }

}