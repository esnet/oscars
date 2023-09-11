package net.es.oscars.topo.ent;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import net.es.oscars.topo.beans.IntRange;
import net.es.oscars.topo.enums.Layer;
import org.hibernate.annotations.NaturalId;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class,
                  property = "urn")
public class Port {
    @Id
    @GeneratedValue
    private Long id;

    @NonNull
    @NaturalId
    @Column(unique = true)
    private String urn;

    @Basic
    @Column(length = 65535)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private ArrayList<String> tags;

    @NonNull
    @ManyToOne
    @JsonBackReference(value="device")
    private Device device;

    @Column
    @NonNull
    private Integer reservableIngressBw;

    @Column
    @NonNull
    private Integer reservableEgressBw;

    @Column
    private Integer esdbEquipmentInterfaceId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable
    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Set<Layer3Ifce> ifces = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable
    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Set<IntRange> reservableVlans = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable
    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Set<Layer> capabilities = new HashSet<>();

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null ) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Port other = (Port) obj;
        return id != null && id.equals(other.getId());
    }


    public String toString() {
        return this.getClass().getSimpleName() + "-" + getUrn();
    }

}