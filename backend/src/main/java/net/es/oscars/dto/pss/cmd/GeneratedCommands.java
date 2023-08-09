package net.es.oscars.dto.pss.cmd;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.oscars.dto.pss.cmd.CommandType;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedCommands {
    private String device;
    private Map<CommandType, String> generated;

}
