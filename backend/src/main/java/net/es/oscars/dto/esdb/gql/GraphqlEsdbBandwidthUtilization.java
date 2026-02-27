package net.es.oscars.dto.esdb.gql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GraphqlEsdbBandwidthUtilization {
    private String id;
    private String uuid;
    private Integer bandwidth;
    private String system;
    private String remoteSystemId;
    public Integer getId() {
        return Integer.parseInt(id);
    }

    private GraphqlEsdbEquipmentInterface equipmentInterface = new GraphqlEsdbEquipmentInterface();

}
