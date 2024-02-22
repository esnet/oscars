package net.es.oscars.sb.nso.rest;

import java.util.List;

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
public class LiveStatusLspResult extends LiveStatusResult {

    private String name;

    private String to;

    @JsonProperty("tunnel-id")
    private Integer tunnelId;

    @JsonProperty("fast-fail-config")
    private Boolean fastFailConf;

    @JsonProperty("admin-state")
    private Boolean adminState;

    @JsonProperty("oper-state")
    private Boolean operationalState;

}