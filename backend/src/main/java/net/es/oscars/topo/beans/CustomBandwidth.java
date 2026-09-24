package net.es.oscars.topo.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomBandwidth {
    @JsonProperty
    String port;
    @JsonProperty
    String device;
    @JsonProperty
    Integer mbps;
}
