package net.es.oscars.web.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import net.es.oscars.sb.nso.rest.MacInfoResult;

import java.time.Instant;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MacInfoResponse {

    @JsonProperty("connection-id")
    private String connectionId;
    private Instant timestamp;
    private List<MacInfoResult> results;

}
