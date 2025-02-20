//package net.es.oscars.cuke;
//
//import io.cucumber.java.en.Given;
//import io.cucumber.java.en.Then;
//import lombok.extern.slf4j.Slf4j;
//import net.es.oscars.ctg.UnitTests;
//import net.es.oscars.resv.ent.Vlan;
//import net.es.oscars.resv.svc.ResvLibrary;
//import net.es.topo.common.model.oscars1.IntRange;
//import net.es.oscars.topo.beans.TopoUrn;
//import org.junit.experimental.categories.Category;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.util.*;
//
//@Slf4j
//@Category({UnitTests.class})
//public class VlanSteps extends CucumberSteps {
//    @Autowired
//    private CucumberWorld world;
//
//
//    @Given("^I set this topology baseline$")
//    public void i_set_this_topology_baseline(Map<String, String> baselineVlans) throws Throwable {
//        world.topoBaseline = new HashMap<>();
//        baselineVlans.forEach((urn, vlanExpr)-> {
//            Set<IntRange> reservable = IntRange.fromExpression(vlanExpr);
//            TopoUrn tu = TopoUrn.builder().urn(urn).reservableVlans(reservable).build();
//            world.topoBaseline.put(urn, tu);
//        });
//    }
//
//    @Given("^I set these eternal vlan reservations$")
//    public void i_set_these_eternal_vlan_reservations(List<List<String>> data) throws Throwable {
//        world.reservedVlans = new ArrayList<>();
//        for (List<String> row :data) {
//
//            String urn = row.get(0);
//            Integer vlan = Integer.parseInt(row.get(1));
//            Vlan v = Vlan.builder().vlanId(vlan).urn(urn).build();
//            world.reservedVlans.add(v);
//        }
//
//    }
//
//    @Then("^the available vlans for \"([^\"]*)\" are \"([^\"]*)\"$")
//    public void the_available_vlans_for_are(String portUrn, String expr) throws Throwable {
//        Map<String, Set<IntRange>> availVlanMap = ResvLibrary.availableVlanMap(world.topoBaseline, world.reservedVlans);
//        assert IntRange.asString(availVlanMap.get(portUrn)).equals(expr);
//
//    }
//
//}