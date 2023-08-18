package net.es.oscars.sb;

import lombok.extern.slf4j.Slf4j;

import net.es.oscars.dto.pss.cmd.CommandType;
import net.es.oscars.nso.NsoAdapter;
import net.es.oscars.pss.beans.QueueName;
import net.es.oscars.pss.svc.PSSAdapter;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.enums.DeploymentState;
import net.es.oscars.resv.enums.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;


@Component
@Slf4j
public class SouthboundQueuer {

    @Autowired
    private NsoAdapter adapter;

    @Autowired
    private ConnectionRepository cr;

    private final List<SouthboundTask> running = new ArrayList<>();
    private final List<SouthboundTask> waiting = new ArrayList<>();
    private final List<SouthboundTask> done = new ArrayList<>();

    public void process() {
        for (SouthboundTask rt : running) {
            log.info("running : " + rt.getConnectionId() + " " + rt.getCommandType());
        }
        for (SouthboundTask wt : waiting) {
            log.info("waiting : " + wt.getConnectionId() + " " + wt.getCommandType());
        }
        running.addAll(waiting);

        int threadNum = waiting.size();
        if (threadNum == 0) {
            return;
        }
        ExecutorService executor = Executors.newFixedThreadPool(threadNum);

        List<FutureTask<SouthboundTaskResult>> taskList = new ArrayList<>();
        for (SouthboundTask wt : waiting) {
            cr.findByConnectionId(wt.getConnectionId()).ifPresent(conn -> {
                FutureTask<SouthboundTaskResult> task = new FutureTask<>(() -> adapter.processTask(conn, wt.getCommandType(), wt.getIntent()));
                if (wt.getCommandType().equals(CommandType.BUILD)) {
                    conn.setDeploymentState(DeploymentState.BEING_DEPLOYED);
                    cr.save(conn);
                } else if (wt.getCommandType().equals(CommandType.DISMANTLE)) {
                    conn.setDeploymentState(DeploymentState.BEING_UNDEPLOYED);
                    cr.save(conn);
                }
                taskList.add(task);
            });
        }
        waiting.clear();

        for (FutureTask<SouthboundTaskResult> ft : taskList) {
            try {
                executor.execute(ft);
                SouthboundTaskResult result = ft.get();
                this.completeTask(result);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
    }

    private void completeTask(SouthboundTaskResult result) {
        SouthboundTask completed = null;
        for (SouthboundTask task : running) {
            if (task.getCommandType().equals(result.getCommandType()) &&
                    task.getConnectionId().equals(result.getConnectionId())) {
                completed = task;

                CommandType rct = result.getCommandType();
                // when the task was to build or dismantle we update the connection state
                if (rct.equals(CommandType.BUILD) || rct.equals(CommandType.DISMANTLE)) {
                    cr.findByConnectionId(task.getConnectionId()).ifPresent(c -> {
                            c.setState(result.getState());
                            c.setDeploymentState(result.getDeploymentState());
                            cr.save(c);
                        }
                    );

                }

                log.info("completed : " + result.getConnectionId() + " " + result.getCommandType());
            }
        }
        if (completed != null) {
            running.remove(completed);
            done.add(completed);
        }
    }


    public void clear(QueueName name) {
        switch (name) {
            case DONE -> this.done.clear();
            case RUNNING -> this.running.clear();
            case WAITING -> this.waiting.clear();
        }
    }


    public void add(CommandType ct, String connId, State intent) {

        SouthboundTask pt = SouthboundTask.builder()
                .commandType(ct)
                .connectionId(connId)
                .intent(intent)
                .build();

        boolean add = true;

        for (SouthboundTask task : running) {
            if (task.getConnectionId().equals(connId)) {
                if (task.getCommandType().equals(ct)) {
                    add = false;
                    log.info("will not add since already running: " + connId + " " + ct);
                }
            }
        }
        if (add) {
            boolean removeFromWaiting = false;
            SouthboundTask removeThis = null;
            for (SouthboundTask task : waiting) {
                if (task.getConnectionId().equals(connId)) {
                    if (task.getCommandType().equals(ct)) {
                        add = false;
                        log.info("will not add since already waiting: " + connId + " " + ct);
                    } else if (task.getCommandType().equals(CommandType.DISMANTLE) && ct.equals(CommandType.BUILD)) {
                        add = false;
                        removeFromWaiting = true;
                        removeThis = task;
                        log.info("incoming dismantle canceled a build " + connId);
                    } else if (task.getCommandType().equals(CommandType.BUILD) && ct.equals(CommandType.DISMANTLE)) {
                        add = false;
                        removeFromWaiting = true;
                        removeThis = task;
                        log.info("incoming build canceled a dismantle " + connId);
                    }
                }
            }
            if (removeFromWaiting) {
                log.info("removing a cancelled task from waiting");
                waiting.remove(removeThis);
            }
        }
        if (add) {
            log.info("adding task to waiting: " + connId + " " + ct);
            waiting.add(pt);
        }
    }

    public List<SouthboundTask> entries(QueueName name) {
        return switch (name) {
            case DONE -> this.done;
            case RUNNING -> this.running;
            case WAITING -> this.waiting;
        };
    }

}