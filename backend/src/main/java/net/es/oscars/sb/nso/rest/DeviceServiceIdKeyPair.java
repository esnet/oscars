package net.es.oscars.sb.nso.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceServiceIdKeyPair {

    private String device;
    private int serviceId;

}
