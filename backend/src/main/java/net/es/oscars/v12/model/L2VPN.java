package net.es.oscars.v12.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.common.BaseDbObject;
import net.es.oscars.v12.model.intent.L2VPNIntent;
import net.es.oscars.v12.model.resource.L2VPNResource;

import java.util.List;


@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class L2VPN extends BaseDbObject {

    @JsonProperty("connection-id")
    String connectionId;

    @JsonProperty("is-released")
    Boolean released;

    @JsonProperty("should-be-released")
    Boolean shouldBeReleased;

    @OneToOne
    L2VPNIntent intent;

    @OneToOne
    @ToString.Exclude
    L2VPNResource resource;

    @OneToMany
    @ToString.Exclude
    @JsonProperty("resource-history")
    List<L2VPNResource> resourceHistory;

}
