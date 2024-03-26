package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VlanFixture {
    @JsonCreator
    public VlanFixture(@JsonProperty("connectionId") String connectionId,
                       @JsonProperty("junction") @NonNull VlanJunction junction,
                       @JsonProperty("portUrn") @NonNull String portUrn,
                       @JsonProperty("ingressBandwidth") @NonNull Integer ingressBandwidth,
                       @JsonProperty("egressBandwidth") @NonNull Integer egressBandwidth,
                       @JsonProperty("vlan") @NonNull Vlan vlan,
                       @JsonProperty("strict") @NonNull Boolean strict,
                       @JsonProperty("schedule") Schedule schedule,
                       @JsonProperty("commandParams") Set<CommandParam> commandParams) {
        this.connectionId = connectionId;
        this.junction = junction;
        this.portUrn = portUrn;
        this.ingressBandwidth = ingressBandwidth;
        this.egressBandwidth = egressBandwidth;
        this.strict = strict;
        this.vlan = vlan;
        this.schedule = schedule;
        this.commandParams = commandParams;
    }


    @Id
    @JsonIgnore
    @GeneratedValue
    private Long id;

    // mandatory; a fixture is always associated with a junction
    @NonNull
    @ManyToOne(cascade=CascadeType.ALL)
    private VlanJunction junction;

    // mandatory; a fixture is always associated with a specific port
    @NonNull
    private String portUrn;

    // mandatory; a fixture always has ingress and egress bws (even if 0)
    @NonNull
    private Integer ingressBandwidth;

    @NonNull
    private Integer egressBandwidth;

    @NonNull
    private Boolean strict;

    // mandatory; a fixture always has a vlan specification associated with it
    @ManyToOne(cascade = CascadeType.ALL)
    @NonNull
    private Vlan vlan;


    // leave the following empty when requesting
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String connectionId;

    @ManyToOne
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Schedule schedule;


    @OneToMany(cascade = CascadeType.ALL)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @ToString.Exclude
    private Set<CommandParam> commandParams;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        VlanFixture that = (VlanFixture) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
