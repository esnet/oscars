package net.es.oscars.dto.esdb.gql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GraphqlEsdbEquipmentPlatform {
    private String id;
    private String name;
    private GraphqlEsdbEquipmentPlatformManufacturer manufacturer;
    private String description;

    public Integer getId() {
        return Integer.parseInt(id);
    }
}
