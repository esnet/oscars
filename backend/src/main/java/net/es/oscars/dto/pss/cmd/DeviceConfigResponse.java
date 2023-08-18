package net.es.oscars.dto.pss.cmd;


import lombok.*;
import net.es.oscars.dto.topo.DeviceModel;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceConfigResponse {
    @NonNull
    private String device;
    @NonNull
    private DeviceModel model;

    @NonNull
    private String asJson;

    @NonNull
    private Instant lastUpdated;


}
