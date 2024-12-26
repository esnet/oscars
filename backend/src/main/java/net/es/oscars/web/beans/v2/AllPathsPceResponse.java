package net.es.oscars.web.beans.v2;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.oscars.model.Bundle;
import net.es.oscars.model.Interval;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllPathsPceResponse {
    protected String connectionId;
    protected Interval interval;
    protected int bandwidth;
    protected List<Bundle> bundles;


}
