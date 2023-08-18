package net.es.oscars.dto.pss.cmd;


import lombok.*;
import net.es.oscars.dto.pss.st.*;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandStatus {
    @NonNull
    private String device;

    @NonNull
    private String profile;

    @NonNull
    private CommandType type;

    @NonNull
    private String commands;
    @NonNull
    private String output;
    @NonNull
    private Date lastUpdated;

    private String connectionId;

    private ControlPlaneStatus controlPlaneStatus;
    private LifecycleStatus lifecycleStatus;
    private ConfigStatus configStatus;
    private OperationalStatus operationalStatus;


}
