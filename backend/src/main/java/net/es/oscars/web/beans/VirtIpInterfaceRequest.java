package net.es.oscars.web.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtIpInterfaceRequest {

    @JsonProperty("connection-id")
    private String connectionId;

    private String device;

    @JsonProperty("ip-address")
    private String ipAndSubnet;

}
