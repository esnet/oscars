package net.es.oscars.v12.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.intent.L2VPNIntent;
import net.es.oscars.v12.model.resource.L2VPNResource;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class L2VPN {
    @Id
    @GeneratedValue
    Long id;

    @JsonProperty("connection-id")
    String connectionId;

    Boolean released;

    Boolean markedForRelease;

    @OneToMany
    @ToString.Exclude
    List<L2VPNIntent> intents;

    @OneToMany
    @ToString.Exclude
    List<L2VPNResource> resources;

    public void invalidateLastIntent(Instant effectiveUntil) {
        if (intents != null) {
            L2VPNIntent last = intents.get(intents.size() - 1);
            last.getValidity().setIndefinite(false);
            last.getValidity().setEnd(effectiveUntil);
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        L2VPN that = (L2VPN) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
