package net.es.oscars.topo.beans.v2;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Bandwidth {

    @NonNull
    private Unit unit;

    @NonNull
    private Integer available;

    @NonNull
    private Integer physical;

    public enum Unit {
        MBPS, GBPS
    }
}

