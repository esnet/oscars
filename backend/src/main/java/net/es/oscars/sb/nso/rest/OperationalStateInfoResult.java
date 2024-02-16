package net.es.oscars.sb.nso.rest;

import java.time.Instant;
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
public class OperationalStateInfoResult extends LiveStatusResult {

    @JsonProperty("sdp")
    private List<LiveStatusSdpResult> sdps;

    @JsonProperty("sap")
    private List<LiveStatusSapResult> saps;

    @JsonProperty("lsp")
    private List<LiveStatusLspResult> lsps;

}
