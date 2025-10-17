package net.es.oscars.dto.esdb.gql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GraphqlEsdbOrganization {
    private String uuid;
    private String shortName;
    private List<GraphEsdbContact> contacts;

    @Data
    public static class GraphEsdbContact {
        private Contact contact;

        @Data
        public static class Contact {
            private String uuid;
            private String orcid;
            private String primaryEmailAddress;
        }
    }


}
