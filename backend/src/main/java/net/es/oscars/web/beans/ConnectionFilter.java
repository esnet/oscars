package net.es.oscars.web.beans;

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
        CONNECTION_ID,
        USERNAME,
        MTU,
        MODE,
        STATE,
        PHASE,
        LAST_MODIFIED,
        DESCRIPTION,
        DEPLOYMENT_INTENT,
        DEPLOYMENT_STATE,
        SERVICE_ID,
        PORT,
        TAGS,
        VLAN_ID
    }

}
