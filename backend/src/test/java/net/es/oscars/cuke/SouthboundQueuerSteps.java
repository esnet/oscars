package net.es.oscars.cuke;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.dto.pss.cmd.CommandType;
import net.es.oscars.sb.SouthboundQueuer;
import net.es.oscars.sb.SouthboundTask;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.experimental.categories.Category;

import java.util.*;

@Slf4j
@Category({UnitTests.class})
public class SouthboundQueuerSteps extends CucumberSteps {
    private List<SouthboundTask> processedTasks;
    private List<SouthboundTask> running;
    private List<SouthboundTask> waiting;

    @When("^I submit multiple duplicate tasks to the queue preprocesor$")
    public void submit_dupe_tasks() {
        waiting.add(SouthboundTask.builder()
                .commandType(CommandType.BUILD)
                .connectionId("ABCD")
                .build());

        waiting.add(SouthboundTask.builder()
                .commandType(CommandType.BUILD)
                .connectionId("ABCD")
                .build());

        waiting.add(SouthboundTask.builder()
                .commandType(CommandType.DISMANTLE)
                .connectionId("DEFG")
                .build());

        waiting.add(SouthboundTask.builder()
                .commandType(CommandType.DISMANTLE)
                .connectionId("DEFG")
                .build());

        processedTasks = SouthboundQueuer.preprocessQueue(waiting, running);
    }


    @Then("the processed queue does not have duplicate tasks for the same connection id")
    public void theProcessedQueueDoesNotHaveDuplicateTasksForTheSameConnectionId() {
        Set<Pair<String, CommandType>> dupeCheckerMap = new HashSet<>();
        for (SouthboundTask task : processedTasks) {
            Pair<String, CommandType> pair = Pair.of(task.getConnectionId(), task.getCommandType());
            if (!dupeCheckerMap.contains(pair)) {
                dupeCheckerMap.add(pair);
            } else {
                assert false;
            }
        }
    }


    @When("I submit a mix of tasks to the queue preprocesor")
    public void iSubmitAMixOfTasksToTheQueuePreprocesor() {
        waiting.add(SouthboundTask.builder()
                .commandType(CommandType.BUILD)
                .connectionId("ABCD")
                .build());

        waiting.add(SouthboundTask.builder()
                .commandType(CommandType.BUILD)
                .connectionId("ABCD")
                .build());
        waiting.add(SouthboundTask.builder()
                .commandType(CommandType.REDEPLOY)
                .connectionId("ABCD")
                .build());

        waiting.add(SouthboundTask.builder()
                .commandType(CommandType.DISMANTLE)
                .connectionId("ABCD")
                .build());

        waiting.add(SouthboundTask.builder()
                .commandType(CommandType.BUILD)
                .connectionId("DEFG")
                .build());

        waiting.add(SouthboundTask.builder()
                .commandType(CommandType.DISMANTLE)
                .connectionId("DEFG")
                .build());

        waiting.add(SouthboundTask.builder()
                .commandType(CommandType.DISMANTLE)
                .connectionId("DEFG")
                .build());

        processedTasks = SouthboundQueuer.preprocessQueue(waiting, running);

    }

    @Then("the processed queue does not have any non-dismantle tasks ahead of any dismantle task")
    public void theProcessedQueueDoesNotHaveAnyNonDismantleTasksAheadOfAnyDismantleTask() {
        boolean gotNonDismantle = false;
        for (SouthboundTask task : processedTasks) {
            if (task.getCommandType() == CommandType.DISMANTLE) {
                // it is a DISMANTLE task
                if (gotNonDismantle) {
                    assert false;
                }
            } else {
                gotNonDismantle = true;
            }

        }
    }

    @When("I submit a task that is already in the running queue to the queue preprocesor")
    public void iSubmitATaskThatIsAlreadyInTheRunningQueueToTheQueuePreprocesor() {
        running.add(SouthboundTask.builder()
                .commandType(CommandType.BUILD)
                .connectionId("ABCD")
                .build());

        waiting.add(SouthboundTask.builder()
                .commandType(CommandType.BUILD)
                .connectionId("ABCD")
                .build());

        waiting.add(SouthboundTask.builder()
                .commandType(CommandType.DISMANTLE)
                .connectionId("DEFG")
                .build());
        processedTasks = SouthboundQueuer.preprocessQueue(waiting, running);
    }

    @Then("the processed queue does not contain any tasks that are in the running queue")
    public void theProcessedQueueDoesNotContainAnyTasksThatAreInTheRunningQueue() {
        Set<Pair<String, CommandType>> dupeCheckerMap = new HashSet<>();
        for (SouthboundTask task : running) {
            Pair<String, CommandType> pair = Pair.of(task.getConnectionId(), task.getCommandType());
            if (!dupeCheckerMap.contains(pair)) {
                dupeCheckerMap.add(pair);
            } else {
                assert false;
            }
        }

        for (SouthboundTask task : processedTasks) {
            Pair<String, CommandType> pair = Pair.of(task.getConnectionId(), task.getCommandType());
            if (!dupeCheckerMap.contains(pair)) {
                dupeCheckerMap.add(pair);
            } else {
                assert false;
            }
        }
    }

    @Given("I clear waiting and running queues")
    public void iClearWaitingAndRunningQueues() {
        running = new ArrayList<>();
        waiting = new ArrayList<>();
    }
}
