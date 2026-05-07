package net.es.oscars.web.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.oscars.model.Interval;
import net.es.oscars.resv.enums.State;
import org.hibernate.query.SortDirection;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionFilter {

    private String connectionId;
    private String username;
    private List<Integer> vlans;
    private List<String> ports;
    private String term;
    private String description;
    private String southbound;
    private String phase;
    private State state;
    private Interval interval;

    private SortProperty sortProperty;
    private SortDirection sortDirection;
    private int page;
    private int sizePerPage;

    public enum SortProperty {
        @JsonProperty("connectionId")
        CONNECTION_ID,
        @JsonProperty("username")
        USERNAME,
        @JsonProperty("connectionMTU")
        CONNECTION_MTU,
        @JsonProperty("mode")
        MODE,
        @JsonProperty("state")
        STATE,
        @JsonProperty("phase")
        PHASE,
        @JsonProperty("lastModified")
        LAST_MODIFIED,
        @JsonProperty("description")
        DESCRIPTION,
        @JsonProperty("deploymentIntent")
        DEPLOYMENT_INTENT,
        @JsonProperty("deploymentState")
        DEPLOYMENT_STATE,
        @JsonProperty("serviceId")
        SERVICE_ID,
        @JsonProperty("ports")
        PORT,
        @JsonProperty("tags")
        TAGS,
        @JsonProperty("vlans")
        VLANS
    }

}
