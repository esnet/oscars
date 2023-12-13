package net.es.oscars.v12.model.intent;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.common.BaseDbObject;
import net.es.oscars.v12.model.common.QosMarking;
import net.es.oscars.v12.model.resource.QosResource;

@Builder
@AllArgsConstructor
@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class QosIntent extends BaseDbObject {

    @JsonProperty("satisfied-by")
    @OneToOne
    @JoinColumn(name = "qos_resource_id", referencedColumnName = "id")
    QosResource satisfiedBy;

    Integer mbps;

    @JsonProperty("in-profile-marking")
    @Enumerated(EnumType.STRING)
    QosMarking inProfileMarking;

    @JsonProperty("out-of-profile-marking")
    @Enumerated(EnumType.STRING)
    QosMarking ooProfileMarking;


}
