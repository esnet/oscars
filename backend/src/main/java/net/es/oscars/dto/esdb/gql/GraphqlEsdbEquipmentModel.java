package net.es.oscars.dto.esdb.gql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GraphqlEsdbEquipmentModel {
    private String id;
    private String name;
    private GraphqlEsdbEquipmentModelManufacturer manufacturer;

    public Integer getId() {
        return Integer.parseInt(id);
    }
}
