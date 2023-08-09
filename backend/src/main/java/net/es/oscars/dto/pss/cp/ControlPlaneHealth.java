package net.es.oscars.dto.pss.cp;


import lombok.*;
import net.es.oscars.dto.pss.st.*;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControlPlaneHealth {
    @NonNull
    private Map<String, ControlPlaneStatus> deviceStatus;


}
