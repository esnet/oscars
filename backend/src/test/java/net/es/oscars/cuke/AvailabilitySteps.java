package net.es.oscars.cuke;

import io.cucumber.java.en.Then;
import io.cucumber.datatable.DataTable;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.resv.beans.PeriodBandwidth;
import net.es.oscars.resv.enums.BwDirection;
import net.es.oscars.resv.svc.ResvLibrary;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Category({UnitTests.class})
public class AvailabilitySteps extends CucumberSteps {

    @Autowired
    private Jackson2ObjectMapperBuilder builder;

    @Autowired
    private CucumberWorld world;



    @Then("^the \"([^\"]*)\" bw availability map between (\\d+) and (\\d+) is$")
    public void the_bw_avail_map_between_and_is(BwDirection dir, int b, int e, Map<String, Integer> table) throws Throwable {

        Map<String, List<PeriodBandwidth>> forDir = world.bwMaps.get(dir);

        // first, from all the fake resvs, choose the ones that overlap this period
        Map<String, List<PeriodBandwidth>> filtered = new HashMap<>();
        for (String urn : forDir.keySet()) {
            List<PeriodBandwidth> candidatesForUrn = forDir.get(urn);
            List<PeriodBandwidth> overlapping = ResvLibrary.pbwsOverlapping(candidatesForUrn,
                    Instant.ofEpochSecond(b), Instant.ofEpochSecond(e));

            filtered.put(urn, overlapping);

        }

        Map<String, Integer> availMap = ResvLibrary.availableBandwidthMap(dir, world.topoBaseline, filtered);

        for (String urn : availMap.keySet()) {
            // log.info("avail "+urn);
            assert table.keySet().contains(urn);
            assert table.get(urn).equals(availMap.get(urn));
        }
        for (String urn : table.keySet()) {
            // log.info("table "+urn);
            assert availMap.keySet().contains(urn);
            assert availMap.get(urn).equals(table.get(urn));
        }


    }

    @Then("^the vlan availability map between (\\d+) and (\\d+) is$")
    public void the_vlan_availability_map_between_and_is(int arg1, int arg2, DataTable arg3) throws Throwable {

    }


}