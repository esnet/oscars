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

    @JsonProperty("vlan-availability")
    private VlanAvailability vlanAvailability;

    @JsonProperty("vlan-usage")
    private Map<Integer, Set<String>> vlanUsage;

    @JsonProperty("esdb-equipment-interface-id")
    private Integer esdbEquipmentInterfaceId;

    private ArrayList<String> description;


}