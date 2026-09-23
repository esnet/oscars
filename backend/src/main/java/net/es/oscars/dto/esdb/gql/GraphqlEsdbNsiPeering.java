package net.es.oscars.dto.esdb.gql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GraphqlEsdbNsiPeering {
    private String id;
    private String localName;
    private String inAlias;
    private String outAlias;
    private String biAlias;
    private GraphqlEsdbEquipmentInterfaceForNsiPeering equipmentInterface;
    private String uuid;
    private Integer bandwidth;
    private List<GraphqlEsdbVlanRange> allowedVlanRanges;
    public Integer getId() { return Integer.parseInt(id); }
}
