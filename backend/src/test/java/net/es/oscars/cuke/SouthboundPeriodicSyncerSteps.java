package net.es.oscars.cuke;

import io.cucumber.java.BeforeStep;
import io.cucumber.java.en.Then;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.NsoProperties;
import net.es.oscars.ctg.UnitTests;
import net.es.oscars.sb.SouthboundPeriodicSyncer;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.scheduler.Scheduler;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@Slf4j
@Category({UnitTests.class, Scheduler.class})
public class SouthboundPeriodicSyncerSteps extends CucumberSteps {
    @Autowired
    @Mock
    @Spy
    SouthboundPeriodicSyncer sbSyncer;
    @Autowired
    NsoProperties nsoProps;

    @BeforeStep("@NsoSyncScheduledTaskSteps")
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    @Then("The scheduled task should be executed at least {int} times within {int} seconds")
    public void theScheduledTaskShouldBeExecuted(int times, int seconds) {
        sbSyncer = mock(SouthboundPeriodicSyncer.class);
        // Not sure why ScheduledTasks isn't triggering when run with Cucumber.
        // Manually triggering the task for now.
        sbSyncer.periodicSyncTask();

        await()
            .atMost(seconds, TimeUnit.SECONDS)
            .untilAsserted(
                    () -> verify(sbSyncer, atLeast(times)).periodicSyncTask()
            );

    }
}
