package net.es.oscars.topo.beans;

import lombok.*;
import net.es.oscars.topo.enums.Layer;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper=false)
public class TopoAdjcy extends DefaultWeightedEdge {
    private TopoUrn a;

    private TopoUrn z;

    @Builder.Default
    private Map<Layer, Long> metrics = new HashMap<>();

}
