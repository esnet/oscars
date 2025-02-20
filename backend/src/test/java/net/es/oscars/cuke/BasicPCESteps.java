package net.es.oscars.cuke;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
//import net.es.oscars.pce.AllPathsPCE;
import net.es.oscars.pce.PceService;
import net.es.oscars.resv.ent.EroHop;
import net.es.oscars.resv.ent.VlanJunction;
import net.es.oscars.resv.ent.VlanPipe;
import net.es.oscars.resv.enums.BwDirection;
import net.es.oscars.resv.enums.EroDirection;
import net.es.oscars.resv.svc.ResvLibrary;
import net.es.oscars.topo.beans.TopoUrn;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.oscars.web.beans.PcePath;
import net.es.oscars.web.beans.PceRequest;
import net.es.oscars.web.beans.PceResponse;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Slf4j
@Category({UnitTests.class})
public class BasicPCESteps extends CucumberSteps {
    @Autowired
    private CucumberWorld world;

    @Autowired
    private PceService widestPathsPCE;
    @Autowired
    private TopologyStore topoStore;

    @When("^I ask for a path from \"([^\"]*)\" to \"([^\"]*)\" with az: (\\d+) and za: (\\d+)$")
    public void i_ask_for_a_path_from_to_with_az_and_za(String a, String z, int azBw, int zaBw) throws Throwable {

//        VlanJunction aj = VlanJunction.builder()
//                .refId(a)
//                .deviceUrn(a)
//                .build();
//        VlanJunction zj = VlanJunction.builder()
//                .refId(z)
//                .deviceUrn(z)
//                .build();

//        VlanPipe vp = VlanPipe.builder()
//                .a(aj)
//                .z(zj)
//                .protect(true)
//                .azBandwidth(azBw)
//                .zaBandwidth(zaBw).build();

//        Map<String, Integer> availIngressBw;
//        Map<String, Integer> availEgressBw;
//        Map<String, TopoUrn > baseline = topoStore.getTopoUrnMap();


//        availIngressBw = ResvLibrary.availableBandwidthMap(BwDirection.INGRESS, baseline, new HashMap<>());
//        availEgressBw = ResvLibrary.availableBandwidthMap(BwDirection.EGRESS, baseline, new HashMap<>());

        PceRequest pceRequest = new PceRequest();
        pceRequest.setA(a);
        pceRequest.setZ(z);
        pceRequest.setAzBw(azBw);
        pceRequest.setZaBw(zaBw);

        PceResponse response = widestPathsPCE.calculatePaths(pceRequest);
        PcePath shortest = response.getShortest();

        world.pipeEros = new HashMap<>();
        world.pipeEros.put(EroDirection.A_TO_Z, shortest.getAzEro());
        world.pipeEros.put(EroDirection.Z_TO_A, shortest.getZaEro());


    }

    @Then("^the resulting AZ ERO is:$")
    public void the_resulting_AZ_ERO_is(List<String> ero) throws Throwable {
        List<EroHop> hops = world.pipeEros.get(EroDirection.A_TO_Z);
        assert hops != null;
        assert hops.size() == ero.size();
        for (int i = 0 ; i < ero.size() ; i++) {
            assert (hops.get(i).getUrn().equals(ero.get(i)));
        }

    }



}