package net.es.oscars.v12.model.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.constraint.LspConstraint;
import org.hibernate.proxy.HibernateProxy;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class LspResource {
    @Id
    @GeneratedValue
    Long id;

    @OneToOne(cascade= CascadeType.ALL)
    @JoinTable(name="LSP_TO_CONSTRAINT")
    LspConstraint constraint;

    @JsonProperty("a-pe-router")
    String aPeRouter;

    @JsonProperty("z-pe-router")
    String zPeRouter;

    @JsonProperty("az-qos")
    @OneToOne(cascade = CascadeType.ALL)
    QosResource azQos;

    @JsonProperty("za-qos")
    @OneToOne(cascade = CascadeType.ALL)
    QosResource zaQos;

    @ElementCollection
    List<String> ero;


    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        LspResource that = (LspResource) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
