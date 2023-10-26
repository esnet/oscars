package net.es.oscars.sb;

import lombok.Builder;
import lombok.Data;
import net.es.oscars.dto.pss.cmd.CommandType;
import net.es.oscars.resv.enums.DeploymentState;
import net.es.oscars.resv.enums.State;


@Data
@Builder
public class SouthboundTaskResult {
    public State state;
    public DeploymentState deploymentState;
    public CommandType commandType;
    public String connectionId;
}
