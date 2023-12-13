package net.es.oscars.v12.model.intent;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.common.*;
import net.es.oscars.v12.model.resource.L2VPNResource;

import java.util.List;


@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class L2VPNIntent extends BaseDbObject {

    @OneToOne
    Versioning version;

    @OneToOne
    TimeInterval schedule;

    @OneToOne
    TimeInterval validity;

    Boolean satisfied;

    @JsonProperty("previous_intent")
    @OneToOne
    @JoinColumn(name = "previous_intent_id", referencedColumnName = "id")
    L2VPNIntent previousIntent;

    @Enumerated(EnumType.STRING)
    ServiceTransition transition;

    @JsonProperty("satisfied-by")
    @OneToOne
    @JoinColumn(name = "l2vpn_resource_id", referencedColumnName = "id")
    L2VPNResource satisfiedBy;

    @JsonProperty("service-id")
    String serviceId;

    @JsonProperty("qos-mode")
    @Enumerated(EnumType.STRING)
    QosMode qosMode;

    @OneToOne
    OverlayIntent overlay;

    @OneToMany
    @ToString.Exclude
    List<EndpointIntent> endpoints;


}
