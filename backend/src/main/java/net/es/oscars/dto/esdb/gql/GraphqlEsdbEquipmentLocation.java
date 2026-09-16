package net.es.oscars.dto.esdb.gql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GraphqlEsdbEquipmentLocation {
    private String id;
    private String shortName;

    public Integer getId() {
        return Integer.parseInt(id);
    }
}
