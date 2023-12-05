package net.es.oscars.v12.model.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.constraint.L2VPNEndpointConstraint;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class L2VPNEndpointResource {

    @Id
    @GeneratedValue
    Long id;

    @OneToOne(cascade=CascadeType.ALL)
    @JoinTable(name="ENDPOINT_TO_CONSTRAINT")
    L2VPNEndpointConstraint constraint;

    @JsonProperty("pe-router")
    String peRouter;

    @JsonProperty("pe-router-port")
    String peRouterPort;

    @JsonProperty("vlan-id")
    int vlanId;

    @OneToOne(cascade= CascadeType.ALL)
    QosResource ingress;

    @OneToOne(cascade=CascadeType.ALL)
    QosResource egress;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        L2VPNEndpointResource that = (L2VPNEndpointResource) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
