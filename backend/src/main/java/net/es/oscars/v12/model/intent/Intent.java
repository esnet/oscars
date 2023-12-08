package net.es.oscars.v12.model.intent;

import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.BaseDbObject;
import net.es.oscars.v12.model.TimeInterval;
import net.es.oscars.v12.model.Versioning;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@MappedSuperclass
@AllArgsConstructor
@Entity
@Builder
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Intent extends BaseDbObject {

    @OneToOne
    Versioning version;

    @OneToOne
    TimeInterval schedule;

    @OneToOne
    TimeInterval validity;

    Boolean satisfied;

    Boolean satisfiable;

}
