package net.es.oscars.v12.model.intent;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.BaseDbObject;
import net.es.oscars.v12.model.Waypoint;
import net.es.oscars.v12.model.resource.TunnelResource;

import java.util.List;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class TunnelIntent extends BaseDbObject {

    @OneToOne
    @JsonProperty("satisfied-by")
    TunnelResource satisfiedBy;

    @JsonProperty("a")
    String a;

    @JsonProperty("z")
    String z;

    @JsonProperty("az-qos")
    @OneToOne
    QosIntent azQos;

    @JsonProperty("za-qos")
    @OneToOne
    QosIntent zaQos;

    @OneToMany
    @ToString.Exclude
    List<Waypoint> waypoints;

    @OneToMany
    @ToString.Exclude
    List<Waypoint> exclude;


}
