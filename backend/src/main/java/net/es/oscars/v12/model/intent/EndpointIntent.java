package net.es.oscars.v12.model.intent;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.common.BaseDbObject;
import net.es.oscars.v12.model.resource.EndpointResource;

@AllArgsConstructor
@Entity
@Builder
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class EndpointIntent extends BaseDbObject {

    @JsonProperty("satisfied-by")
    @OneToOne
    @JoinColumn(name = "endpoint_resource_id", referencedColumnName = "id")
    EndpointResource satisfiedBy;

    String device;

    String port;

    @JsonProperty("vlan-expression")
    String vlanExpression;

    @OneToOne
    QosIntent ingress;

    @OneToOne
    QosIntent egress;



}
