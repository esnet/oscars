package net.es.oscars.sb.nso.rest;

import java.time.Instant;
import java.util.List;

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
public class OperationalStateInfoResult extends LiveStatusResult {

    @Builder
    OperationalStateInfoResult(String device, Instant timestamp, Boolean status, String errorMessage,
            List<LiveStatusSdpResult> sdps, List<LiveStatusSapResult> saps, List<LiveStatusLspResult> lsps) {
        super(device, timestamp, status, errorMessage);
        this.sdps = sdps;
        this.saps = saps;
        this.lsps = lsps;
    }

    @JsonProperty("sdp")
    private List<LiveStatusSdpResult> sdps;

    @JsonProperty("sap")
    private List<LiveStatusSapResult> saps;

    @JsonProperty("lsp")
    private List<LiveStatusLspResult> lsps;

}
