package net.es.oscars.v12.model.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.QosMarking;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class QosResource extends Resource {

    Integer mbps;

    @JsonProperty("in-profile-marking")
    QosMarking inProfileMarking;

    @JsonProperty("out-of-profile-marking")
    QosMarking ooProfileMarking;

}
