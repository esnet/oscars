package net.es.oscars.v12.model.resource;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class OverlayResource extends Resource {

    @OneToMany(cascade = CascadeType.ALL)
    @ToString.Exclude
    List<TunnelResource> tunnels;


}
