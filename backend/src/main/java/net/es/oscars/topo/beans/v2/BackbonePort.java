package net.es.oscars.topo.beans.v2;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.ArrayList;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BackbonePort {
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


    @JsonProperty("esdb-equipment-interface-id")
    private Integer esdbEquipmentInterfaceId;

    private ArrayList<String> description;

}