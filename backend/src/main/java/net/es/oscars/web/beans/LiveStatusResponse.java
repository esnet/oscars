package net.es.oscars.web.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.Instant;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiveStatusResponse {

    @JsonProperty("connection-id")
    private String connectionId;
    private Instant timestamp;

}
