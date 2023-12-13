package net.es.oscars.v12.model.resource;

import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.common.BaseDbObject;

import java.util.List;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class OverlayResource extends BaseDbObject {

    @OneToMany(cascade = CascadeType.ALL)
    @ToString.Exclude
    List<TunnelResource> tunnels;


}
