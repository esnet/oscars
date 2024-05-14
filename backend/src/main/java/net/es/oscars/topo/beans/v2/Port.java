package net.es.oscars.topo.beans.v2;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Set;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Port {
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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("vlan-availability")
    private VlanAvailability vlanAvailability;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("esdb-equipment-interface-id")
    private Integer esdbEquipmentInterfaceId;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private ArrayList<String> description;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("used-by")
    private Set<String> usedBy;

}