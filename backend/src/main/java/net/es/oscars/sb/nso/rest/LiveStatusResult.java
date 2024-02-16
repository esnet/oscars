package net.es.oscars.sb.nso.rest;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveStatusResult {

    private String device;
    private Instant timestamp = null;
    private Boolean status = false;
    @JsonProperty("error-message")
    private String errorMessage = null;

}
