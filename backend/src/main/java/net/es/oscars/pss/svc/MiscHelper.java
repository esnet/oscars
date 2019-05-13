package net.es.oscars.pss.svc;


import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PSSException;
import net.es.oscars.dto.pss.params.MplsHop;
import net.es.oscars.resv.ent.*;
import net.es.oscars.topo.db.AdjcyRepository;
import net.es.oscars.topo.ent.Adjcy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class MiscHelper {

    @Autowired
    private AdjcyRepository adjcyRepo;


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
                log.debug("hop " + hop.getUrn());
                log.debug("prevHop " + prevHop.getUrn());
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
        for (Adjcy adjcy : adjcyRepo.findAll()) {
            if (adjcy.getA().getPortUrn().equals(aPort)
                    && adjcy.getZ().getPortUrn().equals(zPort)) {
                return adjcy.getZ().getAddr();
            }
        }
        throw new PSSException("Could not find an adjacency for "+aPort+" -- "+zPort);

    }
}