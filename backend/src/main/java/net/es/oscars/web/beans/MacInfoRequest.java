package net.es.oscars.web.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MacInfoRequest {

    @JsonProperty("connection-id")
    private String connectionId;

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("device-ids")
    private List<String> deviceIds = new LinkedList<>();

    @JsonSetter(nulls = Nulls.SKIP)
    @JsonProperty("refresh-if-older-than")
    private Instant refreshIfOlderThan = Instant.now().minus(10, ChronoUnit.SECONDS);
    //String refreshIfOlderThan;

}
