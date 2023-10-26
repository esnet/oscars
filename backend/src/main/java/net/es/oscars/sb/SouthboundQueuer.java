package net.es.oscars.sb;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import net.es.oscars.dto.pss.cmd.CommandType;
import net.es.oscars.resv.ent.VlanJunction;
import net.es.oscars.sb.ent.RouterCommands;
import net.es.oscars.sb.nso.NsoAdapter;
import net.es.oscars.sb.beans.QueueName;
import net.es.oscars.sb.rancid.RancidAdapter;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.enums.DeploymentState;
import net.es.oscars.resv.enums.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
@Slf4j
public class SouthboundQueuer {

    @Autowired
    private NsoAdapter nsoAdapter;

    @Autowired
    private RancidAdapter rancidAdapter;

    @Autowired
    private ConnectionRepository cr;

    private final List<SouthboundTask> running = new ArrayList<>();
    private final List<SouthboundTask> waiting = new ArrayList<>();
    private final List<SouthboundTask> done = new ArrayList<>();

    @Transactional
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

        for (SouthboundTask wt : waiting) {
            cr.findByConnectionId(wt.getConnectionId()).ifPresent(conn -> {
                if (wt.getCommandType().equals(CommandType.BUILD)) {
                    conn.setDeploymentState(DeploymentState.BEING_DEPLOYED);
                } else if (wt.getCommandType().equals(CommandType.DISMANTLE)) {
                    conn.setDeploymentState(DeploymentState.BEING_UNDEPLOYED);
                }
                cr.save(conn);

                if (isLegacy(conn)) {
                    if (wt.getCommandType().equals(CommandType.DISMANTLE)) {
                        this.completeTask(rancidAdapter.processTask(conn, wt.getCommandType(), wt.getIntent()));
                    } else {
                        log.warn("not performing non-dismantle task for legacy connection "+conn.getConnectionId());
                    }
                } else {
                    this.completeTask(nsoAdapter.processTask(conn, wt.getCommandType(), wt.getIntent()));
                }
            });
        }
        waiting.clear();

    }

    @Transactional
    public void completeTask(SouthboundTaskResult result) {
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

    // a connection is legacy if it has any old-style dismantle commands
    public boolean isLegacy(Connection conn) {
        for (VlanJunction j : conn.getArchived().getCmp().getJunctions()) {
            RouterCommands existing = rancidAdapter.existing(conn.getConnectionId(), j.getDeviceUrn(), CommandType.DISMANTLE);
            if (existing != null) {
                return !existing.getTemplateVersion().equals("NSO 1.1");
            }
        }
        return false;
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