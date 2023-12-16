package net.es.oscars.web.beans;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtIpInterfaceListResponse {

    @JsonProperty("output")
    private List<ListEntry> list = new ArrayList<>();

    @Data
    public static class ListEntry {

            private String device;
            @JsonProperty("ip-address")
            private List<String> ipInterfaces;

    }

}
