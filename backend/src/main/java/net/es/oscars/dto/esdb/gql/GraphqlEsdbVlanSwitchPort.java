package net.es.oscars.dto.esdb.gql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GraphqlEsdbVlanSwitchPort {
    private String id;
    private String remotePort;

    public Integer getId() { return Integer.parseInt(id); }
}
