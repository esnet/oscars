package net.es.oscars.web.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.oscars.model.Interval;
import net.es.oscars.resv.enums.State;

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
    private String description;
    private String southbound;
    private String phase;
    private State state;
    private Interval interval;
    private int page;
    private int sizePerPage;

}
