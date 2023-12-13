package net.es.oscars.v12.model.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.common.BaseDbObject;
import net.es.oscars.v12.model.common.QosMode;
import net.es.oscars.v12.model.common.TimeInterval;
import net.es.oscars.v12.model.common.Versioning;

import java.util.List;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class L2VPNResource extends BaseDbObject {

    @OneToOne
    Versioning version;

    @OneToOne
    TimeInterval schedule;

    @OneToOne
    TimeInterval validity;

    @JsonProperty("should-be-deployed")
    Boolean shouldBeDeployed;

    @JsonProperty("is-deployed")
    Boolean deployed;

    @JsonProperty("qos-mode")
    @Enumerated(EnumType.STRING)
    QosMode qosMode;

    @JsonProperty("service-id")
    String serviceId;

    @OneToOne
    OverlayResource overlay;

    @OneToMany
    @ToString.Exclude
    List<EndpointResource> endpoints;

}
