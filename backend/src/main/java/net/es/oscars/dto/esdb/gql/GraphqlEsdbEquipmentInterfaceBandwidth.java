package net.es.oscars.dto.esdb.gql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GraphqlEsdbEquipmentInterfaceBandwidth {
    private String id;
    private Integer speed;

    public Integer getId() { return Integer.parseInt(id); }
}
