package net.es.oscars.topo.beans;

import lombok.*;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class Layer3Ifce {
    private String urn;
    private String port;
    private String ipv4Address;
    private String ipv6Address;



}
