package net.es.oscars.sb.nso.rest;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LiveStatusResult {

    private String device;
    @Builder.Default
    private Instant timestamp = null;
    @Builder.Default
    private Boolean status = false;

    @Builder.Default
    @JsonProperty("error-message")
    private String errorMessage = null;

}
