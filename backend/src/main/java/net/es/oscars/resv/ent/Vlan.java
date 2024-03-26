package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Vlan {

    @JsonCreator
    public Vlan(@JsonProperty("connectionId") String connectionId,
                @JsonProperty("schedule") Schedule schedule,
                @JsonProperty("urn") @NonNull String urn,
                @JsonProperty("vlanId") @NonNull Integer vlanId) {
        this.connectionId = connectionId;
        this.urn = urn;
        this.vlanId = vlanId;
        this.schedule = schedule;
    }

    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @NonNull
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String urn;


    // mandatory; must be set even if empty

    @NonNull
    private Integer vlanId;


    // these will be populated by the system when designing
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String connectionId;

    @ManyToOne
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Schedule schedule;


    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Vlan vlan = (Vlan) o;
        return getId() != null && Objects.equals(getId(), vlan.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
