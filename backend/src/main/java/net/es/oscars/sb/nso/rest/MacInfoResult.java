package net.es.oscars.sb.nso.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MacInfoResult {

    private String device;

    @Builder.Default
    private Instant timestamp = null;

    @Builder.Default
    private Boolean status = false;

    @JsonProperty("error-message")
    @Builder.Default
    private String errorMessage = null;

    @JsonProperty("fdb")
    @Builder.Default
    private String fdbQueryResult = null;

}
