package net.es.oscars.dto.pss.cmd;


import lombok.*;
import net.es.oscars.dto.topo.DeviceModel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceConfigRequest {
    @NonNull
    private String device;
    @NonNull
    private DeviceModel model;

    @NonNull
    private String profile;


}
