package net.es.oscars.topo.ent;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;
import net.es.oscars.dto.topo.DeviceModel;
import net.es.oscars.topo.beans.IntRange;
import net.es.oscars.topo.enums.DeviceType;
import net.es.oscars.topo.enums.Layer;
import org.hibernate.annotations.NaturalId;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class,
                  property = "urn")
public class Device {
    @Id
    @GeneratedValue
    private Long id;

    @NonNull
    @Column(unique = true)
    @NaturalId
    private String urn;

    @NonNull
    @Column

    private DeviceModel model;
    @Column
    private Integer esdbEquipmentId;

    @NonNull
    @Builder.Default
    @Column
    private Integer locationId = 0;

    @NonNull
    @Builder.Default
    @Column
    private Double latitude = 0D;

    @NonNull
    @Builder.Default
    @Column
    private Double longitude = 0D;

    @NonNull
    @Builder.Default
    @Column
    private String location = "";

    @NonNull
    @Column
    private DeviceType type;

    @NonNull
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String ipv4Address;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String ipv6Address;

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

    @NonNull
    @ElementCollection(fetch = FetchType.EAGER)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Set<Port> ports = new HashSet<>();


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
        Device other = (Device) obj;
        return id != null && id.equals(other.getId());
    }


    public String toString() {
        return this.getClass().getSimpleName() + "-" + getUrn();
    }
}