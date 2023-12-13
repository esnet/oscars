package net.es.oscars.v12.model.common;

import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.intent.EndpointIntent;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@MappedSuperclass
@AllArgsConstructor
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class BaseDbObject {
    @Id
    @GeneratedValue
    private Long id;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        EndpointIntent that = (EndpointIntent) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
