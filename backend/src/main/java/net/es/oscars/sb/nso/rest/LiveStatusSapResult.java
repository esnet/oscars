package net.es.oscars.sb.nso.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

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
public class LiveStatusSapResult extends LiveStatusResult {

    private String port;

    private Integer vlan;

    @JsonProperty("ingress-qos")
    private Integer ingressQos;

    @JsonProperty("egress-qos")
    private Integer egressQos;

    @JsonProperty("admin-state")
    private Boolean adminState;

    @JsonProperty("oper-state")
    private Boolean operationalState;
}
