package net.es.oscars.sb.nso.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class LiveStatusSdpResult extends LiveStatusResult {

    private Integer sdpId;
    private String type;
    private String farEndAddress;

    private Boolean adminState;
    private Boolean operationalState;

}
