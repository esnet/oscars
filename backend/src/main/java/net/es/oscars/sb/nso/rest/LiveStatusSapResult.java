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
public class LiveStatusSapResult extends LiveStatusResult {

    private String port;
    private Integer vlan;

    private Integer ingressQos;
    private Integer egressQos;

    private Boolean adminState;
    private Boolean operationalState;
}
