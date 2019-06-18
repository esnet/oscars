package net.es.oscars.cuke;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.dto.pss.cmd.CommandType;
import net.es.oscars.pss.beans.PssTask;
import net.es.oscars.pss.beans.QueueName;
import net.es.oscars.pss.svc.PSSQueuer;
import net.es.oscars.resv.enums.State;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Category({UnitTests.class})
public class PssQueueSteps extends CucumberSteps {
    @Autowired
    private PSSQueuer queuer;

    @Given("^I clear all sets$")
    public void i_clear_all_sets() {
        queuer.clear(QueueName.DONE);
        queuer.clear(QueueName.RUNNING);
        queuer.clear(QueueName.WAITING);
    }

    @When("^I add a \"([^\"]*)\" task for \"([^\"]*)\" intending \"([^\"]*)\"$")
    public void i_add_a_task_for_on(CommandType ct, String connId, State intent) {
        queuer.add(ct, connId, intent);
    }

    @Then("^the \"([^\"]*)\" set has (\\d+) entries$")
    public void the_set_has_entries(QueueName qn, int num) {
        assert queuer.entries(qn).size() == num;
    }

    @When("^I trigger the queue processor$")
    public void i_trigger_the_queue_processor() {
        queuer.process();
    }

    @When("^I make all running tasks complete$")
    public void i_make_all_running_tasks_complete() {
        List<PssTask> running = new ArrayList<>(queuer.entries(QueueName.RUNNING));
        for (PssTask task : running) {
            queuer.complete(task.getCommandType(), task.getConnectionId());
        }
    }

}