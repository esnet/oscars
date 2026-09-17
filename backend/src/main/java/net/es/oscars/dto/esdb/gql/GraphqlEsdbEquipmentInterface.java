package net.es.oscars.dto.esdb.gql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GraphqlEsdbEquipmentInterface {
    private String id;
    private String equipmentInterface;
    private GraphqlEsdbEquipmentInterfaceBandwidth interfaceBandwidth;
    private boolean tagged;
    private Integer oscarsBandwidth;

    public Integer getId() {
        return Integer.parseInt(id);
    }
}
