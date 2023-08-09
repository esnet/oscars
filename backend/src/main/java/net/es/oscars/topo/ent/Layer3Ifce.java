package net.es.oscars.topo.ent;

import lombok.*;

import jakarta.persistence.Embeddable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Embeddable
@EqualsAndHashCode
public class Layer3Ifce {
    private String urn;
    private String port;
    private String ipv4Address;
    private String ipv6Address;



}
