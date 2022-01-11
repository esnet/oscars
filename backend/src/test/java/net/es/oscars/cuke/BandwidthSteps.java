package net.es.oscars.cuke;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.resv.beans.PeriodBandwidth;
import net.es.oscars.resv.enums.BwDirection;
import net.es.oscars.resv.svc.ResvLibrary;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.*;

@Slf4j
@Category({UnitTests.class})
public class BandwidthSteps extends CucumberSteps {
    @Autowired
    private CucumberWorld world;

    @Given("^I set this \"([^\"]*)\" bandwidth baseline$")
    public void i_set_this_bandwidth_baseline(BwDirection direction, Map<String, Integer> baselineBws)
            throws Throwable {
        world.bwBaseline.put(direction, baselineBws);
    }

    @Given("^I set these \"([^\"]*)\" bandwidth reservations$")
    public void i_set_these_bandwidth_reservations(BwDirection direction, List<List<String>> data) throws Throwable {
        Map<String, List<PeriodBandwidth>> resvs = new HashMap<>();
        for (List<String> row :data) {

            String urn = row.get(0);
            Integer bw = Integer.parseInt(row.get(1));
            Integer beg = Integer.parseInt(row.get(2));
            Integer end = Integer.parseInt(row.get(3));

            PeriodBandwidth periodBw = PeriodBandwidth.builder()
                    .bandwidth(bw)
                    .beginning(Instant.ofEpochSecond(beg))
                    .ending(Instant.ofEpochSecond(end))
                    .build();
            if (!resvs.containsKey(urn)) {
                resvs.put(urn, new ArrayList<>());
            }
            resvs.get(urn).add(periodBw);
        }
        world.bwMaps.put(direction, resvs);
    }

    @Then("^the available \"([^\"]*)\" bandwidth for \"([^\"]*)\" at (\\d+) is (\\d+)$")
    public void the_available_bandwidth_for_at_is(BwDirection dir, String urn, int when, int bw) throws Throwable {
        List<PeriodBandwidth> periodBws = world.bwMaps.get(dir).get(urn);
        Integer baselineBw = world.bwBaseline.get(dir).get(urn);

        assert ResvLibrary.availBandwidth(baselineBw, periodBws, Instant.ofEpochSecond(when)) == bw;
    }

    @Then("^the overall available \"([^\"]*)\" bw for \"([^\"]*)\" is (\\d+)$")
    public void the_overall_available_bw_for_is(BwDirection dir, String urn, int bw) throws Throwable {
        List<PeriodBandwidth> periodBws = world.bwMaps.get(dir).get(urn);
        Integer baselineBw = world.bwBaseline.get(dir).get(urn);
        assert ResvLibrary.overallAvailBandwidth(baselineBw, periodBws) == bw;
    }


}