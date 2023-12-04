package net.es.oscars.sb.nso.rest;

import java.time.Instant;

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
public class MacInfoResult extends LiveStatusResult {

    @Builder
    MacInfoResult(String device, Instant timestamp, Boolean status, String errorMessage, String fdbQueryResult) {
        super(device, timestamp, status, errorMessage);
        this.fdbQueryResult = fdbQueryResult;
    }

    @JsonProperty("fdb")
    @Builder.Default
    private String fdbQueryResult = null;

}
