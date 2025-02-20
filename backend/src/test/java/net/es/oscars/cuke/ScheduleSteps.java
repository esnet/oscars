//package net.es.oscars.cuke;
//
//import io.cucumber.java.en.Given;
//import io.cucumber.java.en.Then;
//import lombok.extern.slf4j.Slf4j;
//import net.es.oscars.ctg.UnitTests;
//import net.es.oscars.resv.ent.Schedule;
//import net.es.oscars.resv.enums.Phase;
//import net.es.oscars.resv.svc.ResvLibrary;
//
//import org.junit.experimental.categories.Category;
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Category({UnitTests.class})
//public class ScheduleSteps extends CucumberSteps {
//    private List<Schedule> schedules;
//
//
//    @Given("^I clear all schedules$")
//    public void i_clear_all_schedules() throws Throwable {
//        this.schedules = new ArrayList<>();
//    }
//    @Given("^I add these schedules$")
//    public void i_add_these_schedules(List<List<String>> scheduleRows) throws Throwable {
//        for (List<String> row : scheduleRows) {
//            String cId = row.get(0);
//            Integer b = Integer.parseInt(row.get(1));
//            Integer e = Integer.parseInt(row.get(2));
//            Phase p = Phase.valueOf(row.get(3));
//            Schedule s = Schedule.builder()
//                    .beginning(Instant.ofEpochSecond(b))
//                    .ending(Instant.ofEpochSecond(e))
//                    .connectionId(cId)
//                    .phase(p)
//                    .build();
//            this.schedules.add(s);
//        }
//    }
//
//
////    @Then("^a schedule between (\\d+) and (\\d+) does overlap$")
////    public void a_schedule_between_and_overlap(int b, int e, List<String> connectionIds) throws Throwable {
////        List<Schedule> overlapping =
////                ResvLibrary.schedulesOverlapping(this.schedules, Instant.ofEpochSecond(b), Instant.ofEpochSecond(e));
////
////        List<String> overlappingConnectionIds = overlapping.stream()
////                .map(Schedule::getConnectionId).collect(Collectors.toList());
////        assert connectionIds.containsAll(overlappingConnectionIds);
////        assert overlappingConnectionIds.containsAll(connectionIds);
////
////    }
////    @Then("^a schedule between (\\d+) and (\\d+) does not overlap$")
////    public void a_schedule_between_and_overlap(int b, int e) throws Throwable {
////        List<Schedule> overlapping =
////                ResvLibrary.schedulesOverlapping(this.schedules, Instant.ofEpochSecond(b), Instant.ofEpochSecond(e));
////
////        List<String> overlappingConnectionIds = overlapping.stream()
////                .map(Schedule::getConnectionId).collect(Collectors.toList());
////
////        assert overlappingConnectionIds.isEmpty();
////
////    }
//
//}