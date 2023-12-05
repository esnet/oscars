package net.es.oscars.v12.model.constraint;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@AllArgsConstructor
@Entity
@Builder
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class L2VPNEndpointConstraint {

    @Id
    @GeneratedValue
    private Long id;

    @JsonProperty("pe-router")
    String peRouter;
    @JsonProperty("pe-router-port")
    String peRouterPort;

    @OneToOne(cascade=CascadeType.ALL)
    QosConstraint ingress;

    @OneToOne(cascade=CascadeType.ALL)
    QosConstraint egress;

    @JsonProperty("vlan-expression")
    String vlanExpression;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        L2VPNEndpointConstraint that = (L2VPNEndpointConstraint) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
