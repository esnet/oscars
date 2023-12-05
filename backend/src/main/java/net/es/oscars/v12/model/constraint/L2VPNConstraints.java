package net.es.oscars.v12.model.constraint;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.List;
import java.util.Objects;


@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class L2VPNConstraints {
    @Id
    @GeneratedValue
    Long id;

    Integer version;

    String user;

    @JsonProperty("effective-since")
    Instant effectiveSince;

    Instant start;
    Instant end;

    @OneToMany(cascade = CascadeType.ALL)
    @ToString.Exclude
    List<LspConstraint> lsps;

    @OneToMany(cascade = CascadeType.ALL)
    @ToString.Exclude
    List<L2VPNEndpointConstraint> endpoints;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        L2VPNConstraints that = (L2VPNConstraints) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
