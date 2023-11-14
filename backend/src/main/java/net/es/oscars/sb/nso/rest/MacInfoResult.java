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
    private Instant timestamp = null;
    private Boolean status = false;
    @JsonProperty("error-message")
    private String errorMessage = null;
    @JsonProperty("fdb")
    private String fdbQueryResult = null;

}
