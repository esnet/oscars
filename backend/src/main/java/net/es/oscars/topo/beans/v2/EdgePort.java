package net.es.oscars.topo.beans.v2;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EdgePort {
    @JsonGetter("urn")
    public String getUrn() {
        return device+":"+name;
    }

    @NonNull
    private String device;

    @NonNull
    private String name;

    @NonNull
    private Bandwidth bandwidth;

    @NonNull
    private Availability availability;

    @NonNull
    private Usage usage;

    @JsonProperty("esdb-equipment-interface-id")
    private Integer esdbEquipmentInterfaceId;

    private ArrayList<String> description;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Usage {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Map<Integer, Set<String>> vlan;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Map<String, Integer> bandwidth;
    }
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Availability {
        @JsonProperty("vlan")
        private VlanAvailability vlan;

    }


}