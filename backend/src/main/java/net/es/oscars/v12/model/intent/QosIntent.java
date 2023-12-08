package net.es.oscars.v12.model.intent;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.*;
import net.es.oscars.v12.model.BaseDbObject;
import net.es.oscars.v12.model.QosMarking;
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
    QosResource satisfiedBy;

    Integer mbps;

    @JsonProperty("in-profile-marking")
    QosMarking inProfileMarking;

    @JsonProperty("out-of-profile-marking")
    QosMarking ooProfileMarking;


}
