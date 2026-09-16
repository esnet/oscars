package net.es.oscars.dto.esdb.gql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GraphqlEsdbInterfaceBandwidth {
    private String id;
    private String name;
    private Integer speed;

    public Integer getId() { return Integer.parseInt(id); }
}
