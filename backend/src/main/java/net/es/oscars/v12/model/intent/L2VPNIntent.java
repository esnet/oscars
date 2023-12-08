package net.es.oscars.v12.model.intent;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.QosMode;
import net.es.oscars.v12.model.resource.L2VPNResource;

import java.util.List;


@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class L2VPNIntent extends Intent {
    @JsonProperty("satisfied-by")
    @OneToOne
    L2VPNResource satisfiedBy;

    @JsonProperty("service-id")
    String serviceId;

    @JsonProperty("qos-mode")
    QosMode qosMode;

    @OneToOne
    OverlayIntent overlay;

    @OneToMany
    @ToString.Exclude
    List<EndpointIntent> endpoints;


}
