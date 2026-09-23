package net.es.oscars.dto.esdb.gql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GraphqlEsdbEquipment {
    private String id;
    private String name;
    private String orchId;
    private GraphqlEsdbEquipmentRole role;
    private GraphqlEsdbEquipmentState equipmentState;
    private GraphqlEsdbEquipmentNetwork network;
    private GraphqlEsdbEquipmentLocation location;
    private GraphqlEsdbEquipmentModel model;
    private GraphqlEsdbEquipmentPlatform platform;
    private List<GraphqlEsdbEquipmentInterface> interfaces = new ArrayList<>();

    public Integer getId() {
        return Integer.parseInt(id);
    }
}
