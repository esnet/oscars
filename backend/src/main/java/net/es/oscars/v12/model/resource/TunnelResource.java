package net.es.oscars.v12.model.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.TunnelType;
import net.es.oscars.v12.model.Waypoint;
import net.es.oscars.v12.model.intent.TunnelIntent;
import org.hibernate.proxy.HibernateProxy;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class TunnelResource extends Resource {

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
}
