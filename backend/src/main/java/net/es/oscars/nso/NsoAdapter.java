package net.es.oscars.nso;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.dto.pss.cmd.CommandType;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.enums.DeploymentState;
import net.es.oscars.resv.enums.State;
import net.es.oscars.sb.SouthboundTaskResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NsoAdapter {
    public SouthboundTaskResult processTask(Connection conn, CommandType commandType, State intent) {
        log.info("processing southbound NSO task");
        DeploymentState depState = DeploymentState.DEPLOYED;
        if (commandType.equals(CommandType.DISMANTLE)) {
            depState = DeploymentState.UNDEPLOYED;
        } else if (commandType.equals(CommandType.BUILD)) {
            depState = DeploymentState.DEPLOYED;
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return SouthboundTaskResult.builder()
                .connectionId(conn.getConnectionId())
                .deploymentState(depState)
                .state(intent)
                .commandType(commandType)
                .build();

    }
}
