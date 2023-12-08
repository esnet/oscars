package net.es.oscars.v12.model.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class EndpointResource extends Resource {

    String device;

    String port;

    @JsonProperty("vlan-id")
    int vlanId;

    @OneToOne(cascade= CascadeType.ALL)
    QosResource ingress;

    @OneToOne(cascade=CascadeType.ALL)
    QosResource egress;

}
