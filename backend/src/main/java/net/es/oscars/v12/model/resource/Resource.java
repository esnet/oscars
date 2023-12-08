package net.es.oscars.v12.model.resource;

import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.BaseDbObject;
import net.es.oscars.v12.model.TimeInterval;
import net.es.oscars.v12.model.Versioning;
import net.es.oscars.v12.model.intent.EndpointIntent;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@MappedSuperclass
@AllArgsConstructor
@Entity
@Builder
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Resource extends BaseDbObject  {
    @Id
    @GeneratedValue
    private Long id;

    @OneToOne
    Versioning version;

    @OneToOne
    TimeInterval schedule;

    @OneToOne
    TimeInterval validity;

}
