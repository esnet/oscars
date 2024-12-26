package net.es.oscars.web.beans.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.oscars.model.Interval;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LspWaypointSearchRequest {
    private String term;
    private Interval interval;
    private String connectionId;
}
