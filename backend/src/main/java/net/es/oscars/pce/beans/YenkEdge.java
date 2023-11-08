package net.es.oscars.pce.beans;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jgrapht.graph.DefaultWeightedEdge;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
public class YenkEdge extends DefaultWeightedEdge {
    private YenkVertex a;
    private YenkVertex z;

    private long azCapacity;

    private long zaCapacity;
}
