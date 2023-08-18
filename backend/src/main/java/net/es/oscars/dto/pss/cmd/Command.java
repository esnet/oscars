package net.es.oscars.dto.pss.cmd;


import lombok.*;
import net.es.oscars.dto.topo.DeviceModel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Command {
    @NonNull
    private String device;
    @NonNull
    private CommandType type;
    @NonNull
    private DeviceModel model;
    @NonNull
    private String profile;

    private String connectionId;
    private boolean refresh;


}
