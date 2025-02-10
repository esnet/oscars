package net.es.oscars.topo.beans;

import lombok.*;
import net.es.oscars.topo.enums.*;
import net.es.topo.common.model.oscars1.IntRange;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopoUrn {

    @NonNull
    private String urn;

    private UrnType urnType;

    // if this is a port, this will be set to the parent device entity
    // if this is an adjcy, it will be null
    private Device device;

    // may be null
    private Port port;

    // may be null
    private Adjcy adjcy;

    private Integer reservableIngressBw;

    private Integer reservableEgressBw;

    private Set<IntRange> reservableVlans;

    private Set<ReservableCommandParam> reservableCommandParams;

    @Builder.Default
    private Set<Layer> capabilities = new HashSet<>();


}
