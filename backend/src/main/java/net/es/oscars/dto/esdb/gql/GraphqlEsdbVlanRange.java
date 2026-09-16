package net.es.oscars.dto.esdb.gql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class GraphqlEsdbVlanRange {
    private String id;
    private Integer beginRange;
    private Integer endRange;

    public Integer getId() { return Integer.parseInt(id); }
}
