package net.es.oscars.v12.model.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.QosMode;

import java.util.List;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class L2VPNResource extends Resource {

    @JsonProperty("is-deployed")
    Boolean deployed;

    @JsonProperty("qos-mode")
    QosMode qosMode;

    @JsonProperty("service-id")
    String serviceId;

    @OneToOne
    OverlayResource overlay;

    @OneToMany
    @ToString.Exclude
    List<EndpointResource> endpoints;

}
