package net.es.oscars.dto.esdb.gql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GraphqlEsdbVlan {
    private String id;
    private String url;
    private String uuid;
    private Integer vlanId;
    private String description;
    @JsonProperty("bridgeId")
    private String bridge_id;
    private GraphqlEsdbEquipment equipment = new GraphqlEsdbEquipment();
    private GraphqlEsdbEquipmentInterface equipmentInterface = new GraphqlEsdbEquipmentInterface();

    public Integer getId() {
        return Integer.parseInt(id);
    }

    public Integer getEquipment() {
        return equipment.getId();
    }

    public Integer getEquipmentInterface() {
        return equipmentInterface.getId();
    }
}
