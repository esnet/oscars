package net.es.oscars.v12.model.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.common.BaseDbObject;
import net.es.oscars.v12.model.common.QosMarking;


@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class QosResource extends BaseDbObject {

    Integer mbps;

    @JsonProperty("in-profile-marking")
    @Enumerated(EnumType.STRING)
    QosMarking inProfileMarking;

    @JsonProperty("out-of-profile-marking")
    @Enumerated(EnumType.STRING)
    QosMarking ooProfileMarking;

}
