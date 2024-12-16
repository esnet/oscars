package net.es.oscars.web.beans.v2;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.oscars.pce.beans.PipeSpecification;
import net.es.oscars.web.beans.Interval;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllPathsPceResponse {
    protected String connectionId;
    protected Interval interval;
    protected int bandwidth;
    protected List<PipeSpecification> pipes;


}
