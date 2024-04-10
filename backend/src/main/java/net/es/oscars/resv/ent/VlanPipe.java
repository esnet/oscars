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
@NoArgsConstructor
@AllArgsConstructor
public class VlanPipe {
    @JsonCreator
    public VlanPipe(@JsonProperty("connectionId") String connectionId,
                    @JsonProperty("a") @NonNull VlanJunction a,
                    @JsonProperty("z") @NonNull VlanJunction z,
                    @JsonProperty("protect") @NonNull Boolean protect,
                    @JsonProperty("azBandwidth") @NonNull Integer azBandwidth,
                    @JsonProperty("zaBandwidth") @NonNull Integer zaBandwidth,
                    @JsonProperty("azERO") List<EroHop> azERO,
                    @JsonProperty("zaERO") List<EroHop> zaERO,
                    @JsonProperty("schedule") Schedule schedule) {
        this.connectionId = connectionId;
        this.azBandwidth = azBandwidth;
        this.zaBandwidth = zaBandwidth;
        this.azERO = azERO;
        this.zaERO = zaERO;
        this.a = a;
        this.z = z;
        this.protect = protect;
        this.schedule = schedule;
    }


    @Id
    @JsonIgnore
    @GeneratedValue
    private Long id;

    @NonNull
    @ManyToOne
    private VlanJunction a;

    @NonNull
    @ManyToOne
    private VlanJunction z;

    @NonNull
    private Boolean protect;

    @NonNull
    private Integer azBandwidth;
    @NonNull
    private Integer zaBandwidth;

    // EROs are optional
    @OneToMany(cascade = CascadeType.ALL)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @ToString.Exclude
    @OrderBy("id ASC")
    private List<EroHop> azERO;

    @OneToMany(cascade = CascadeType.ALL)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @ToString.Exclude
    @OrderBy("id ASC")
    private List<EroHop> zaERO;

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
        VlanPipe vlanPipe = (VlanPipe) o;
        return getId() != null && Objects.equals(getId(), vlanPipe.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
