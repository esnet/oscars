package net.es.oscars.v12.model.intent;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.common.BaseDbObject;
import net.es.oscars.v12.model.resource.OverlayResource;

import java.util.List;


@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class OverlayIntent extends BaseDbObject {

    @JsonProperty("satisfied-by")
    @OneToOne
    @JoinColumn(name = "overlay_resource_id", referencedColumnName = "id")
    OverlayResource satisfiedBy;


    @OneToMany
    @ToString.Exclude
    List<TunnelIntent> tunnels;


}
