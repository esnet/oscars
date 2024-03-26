package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Components {
    @JsonCreator
    public Components(@JsonProperty("junctions") @NonNull List<VlanJunction> junctions,
                      @JsonProperty("fixtures") @NonNull List<VlanFixture> fixtures,
                      @JsonProperty("pipes") List<VlanPipe> pipes) {
        this.pipes = pipes;
        this.fixtures = fixtures;
        this.junctions = junctions;
    }


    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @NonNull
    @OneToMany(cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<VlanJunction> junctions;

    @NonNull
    @OneToMany(cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<VlanFixture> fixtures;

    @OneToMany(cascade = CascadeType.ALL)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @ToString.Exclude
    private List<VlanPipe> pipes;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Components that = (Components) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
