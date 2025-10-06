package net.es.oscars.sb;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import net.es.oscars.dto.pss.cmd.CommandType;
import net.es.oscars.nsi.svc.NsiService;
import net.es.oscars.resv.enums.DeploymentIntent;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.sb.nso.NsoAdapter;
import net.es.oscars.sb.beans.QueueName;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.enums.DeploymentState;
import net.es.oscars.resv.enums.State;
import net.es.oscars.sb.nso.resv.NsoResourceService;
import net.es.oscars.sb.nso.resv.NsoResvException;
import net.es.topo.common.devel.DevelUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
@Slf4j
public class SouthboundQueuer {

    @Autowired
    private NsoAdapter nsoAdapter;

    @Autowired
    private NsoResourceService nsoResourceService;

    @Autowired
    private ConnectionRepository cr;

    private final List<SouthboundTask> running = new ArrayList<>();
    private final List<SouthboundTask> waiting = new ArrayList<>();
    private final List<SouthboundTask> done = new ArrayList<>();
    @Autowired
    private NsiService nsiService;

    @Transactional
    public void process() {

        // Skip "duplicate" tasks that are already running (or planned) - if we are already
        // performing (or planning to perform) a DISMANTLE for connection ABCD, we don't
        // want to do that again

        // We initialize a set of <connectionId, commandType> tuples to keep track,
        // and fill it with data from all the currently running tasks..
        Set<Pair<String, CommandType>> runningOrPlanned = new HashSet<>();
        for (SouthboundTask rt : running) {
            runningOrPlanned.add(Pair.of(rt.getConnectionId(), rt.getCommandType()));
        }

        List<SouthboundTask> dupesEliminated = new ArrayList<>();
        for (SouthboundTask wt : waiting) {
            Pair<String, CommandType> tuple = Pair.of(wt.getConnectionId(), wt.getCommandType());
            // If the tuple set does _not_ contain our <connectionId, commandType>, we should run the task
            // We also add the tuple to the set.
            if (!runningOrPlanned.contains(tuple)) {
                runningOrPlanned.add(tuple);
                dupesEliminated.add(wt);
            }
        }


        // Now we reorder the list by doing DISMANTLEs before any BUILDs and REDEPLOYs
        // Reason is, if this scenario comes up:
        // connection ABCD is now deployed, using resource 1000, we are asked to DISMANTLE it
        // connection DEFG is undeployed, plans to use resource 1000, we are asked to BUILD it
        //
        // then we must do the ABCD DISMANTLE before the DEFG build or NSO will throw an error

        // make two lists, filter our tasks to either...
        List<SouthboundTask> dismantles = new ArrayList<>();
        List<SouthboundTask> others = new ArrayList<>();
        for (SouthboundTask sbt : dupesEliminated) {
            if (sbt.getCommandType().equals(CommandType.DISMANTLE)) {
                dismantles.add(sbt);
            } else {
                others.add(sbt);
            }
        }
        // then join the two lists, putting the dismantles first
        List<SouthboundTask> dismantlesFirst = new ArrayList<>(dismantles);
        dismantlesFirst.addAll(others);

        for (SouthboundTask wt : dismantlesFirst) {
            log.info("running task : " + wt.getConnectionId() + " " + wt.getCommandType());
            cr.findByConnectionId(wt.getConnectionId()).ifPresent(conn -> {
                if (wt.getCommandType().equals(CommandType.BUILD)) {
                    conn.setDeploymentState(DeploymentState.BEING_DEPLOYED);
                } else if (wt.getCommandType().equals(CommandType.DISMANTLE)) {
                    conn.setDeploymentState(DeploymentState.BEING_UNDEPLOYED);
                } else if (wt.getCommandType().equals(CommandType.REDEPLOY)) {
                    conn.setDeploymentState(DeploymentState.BEING_REDEPLOYED);
                }
                cr.save(conn);

                log.info(wt.getConnectionId() + " " + wt.getCommandType() + " dst: " + conn.getDeploymentState());
                this.completeTask(nsoAdapter.processTask(conn, wt.getCommandType(), wt.getIntent()));
            });
        }
        waiting.clear();

    }

    @Transactional
    public void completeTask(SouthboundTaskResult result) {
        DevelUtils.dumpDebug("complete task", result);
        SouthboundTask completed = null;
        for (SouthboundTask task : running) {
            if (task.getCommandType().equals(result.getCommandType()) &&
                    task.getConnectionId().equals(result.getConnectionId())) {
                completed = task;

                CommandType rct = result.getCommandType();
                // when the task was to build, dismantle or redeploy we update the connection state
                if (rct.equals(CommandType.BUILD) || rct.equals(CommandType.DISMANTLE) || rct.equals(CommandType.REDEPLOY)) {
                    nsiService.updateDataplane(result);

                    // this is kinda funky
                    cr.findByConnectionId(task.getConnectionId()).ifPresent(c -> {
                                c.setState(result.getState());
                                c.setDeploymentState(result.getDeploymentState());
                                if (rct.equals(CommandType.REDEPLOY)) {
                                    c.setDeploymentIntent(DeploymentIntent.SHOULD_BE_DEPLOYED);
                                }
                                cr.save(c);
                            }
                    );
                }

                // special case for DISMANTLE: once complete we release the NSO resources
                // - if the resulting DeploymentState was UNDEPLOYED (instead of i.e. FAILED)
                // AND
                // - if the connection phase is now ARCHIVED
                if (rct.equals(CommandType.DISMANTLE)) {
                    if (result.getDeploymentState().equals(DeploymentState.UNDEPLOYED)) {
                        cr.findByConnectionId(task.getConnectionId()).ifPresent(c -> {
                            if (c.getPhase().equals(Phase.ARCHIVED)) {
                                try {
                                    nsoResourceService.release(c);
                                } catch (NsoResvException e) {
                                    log.error("failed to release NSO resources " + c.getConnectionId(), e);
                                }
                            }
                        });
                    }


                }
                log.info("completed : " + result.getConnectionId() + " " + result.getCommandType()+" "+result.getDeploymentState());
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