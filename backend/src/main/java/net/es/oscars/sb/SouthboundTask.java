package net.es.oscars.sb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.oscars.dto.pss.cmd.CommandType;
import net.es.oscars.resv.enums.State;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SouthboundTask {
    private String connectionId;
    private CommandType commandType;
    private State intent;
}
