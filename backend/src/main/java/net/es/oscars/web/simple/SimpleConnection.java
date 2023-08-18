package net.es.oscars.web.simple;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.oscars.resv.enums.BuildMode;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.enums.State;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleConnection {
    protected String connectionId;
    protected Integer begin;
    protected Integer end;
    protected Integer connection_mtu;
    protected Integer last_modified;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Integer heldUntil;
    protected String username;
    protected Phase phase;
    protected BuildMode mode;
    protected State state;
    @Builder.Default
    protected List<Pipe> pipes = new ArrayList<>();
    @Builder.Default
    protected List<Junction> junctions = new ArrayList<>();
    @Builder.Default
    protected List<Fixture> fixtures = new ArrayList<>();
    protected List<SimpleTag> tags;
    protected String description;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Validity validity;

}
