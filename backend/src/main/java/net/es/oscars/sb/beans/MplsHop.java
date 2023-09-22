package net.es.oscars.sb.beans;

import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MplsHop {

    private Integer order;

    private String address;

}
