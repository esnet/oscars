package net.es.oscars.v12.model.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.common.BaseDbObject;
import net.es.oscars.v12.model.common.TunnelType;
import net.es.oscars.v12.model.common.Waypoint;

import java.util.List;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class TunnelResource extends BaseDbObject {

    TunnelType type;

    String a;

    String z;

    @JsonProperty("az-qos")
    @OneToOne
    QosResource azQos;

    @JsonProperty("za-qos")
    @OneToOne
    QosResource zaQos;

    @OneToMany
    @ToString.Exclude
    List<Waypoint> waypoints;

    Boolean protect;

}
