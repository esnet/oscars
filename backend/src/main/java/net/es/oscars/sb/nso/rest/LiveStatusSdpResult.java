package net.es.oscars.sb.nso.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class LiveStatusSdpResult extends LiveStatusResult {

    @JsonProperty("vc-id")
    private Integer vcId;

    @JsonProperty("sdp-id")
    private Integer sdpId;

    private String type;

    @JsonProperty("far-end")
    private String farEndAddress;

    @JsonProperty("admin-state")
    private Boolean adminState;

    @JsonProperty("oper-state")
    private Boolean operationalState;

}
