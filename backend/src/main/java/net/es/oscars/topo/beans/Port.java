package net.es.oscars.topo.beans;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import net.es.oscars.topo.enums.Layer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class,
                  property = "urn")
public class Port {

    @NonNull
    private String urn;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private ArrayList<String> tags;

    @NonNull
    @JsonBackReference(value="device")
    private Device device;

    @NonNull
    private Integer reservableIngressBw;

    @NonNull
    private Integer reservableEgressBw;

    private Integer esdbEquipmentInterfaceId;

    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Set<Layer3Ifce> ifces = new HashSet<>();

    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Set<IntRange> reservableVlans = new HashSet<>();

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
        return urn.equals(other.getUrn());
    }
    public boolean isEdge() {
        return (this.getCapabilities().contains(Layer.ETHERNET) && this.getCapabilities().contains(Layer.EDGE));
    }
    public boolean lspCapable() {
        return (this.getCapabilities().contains(Layer.MPLS));
    }

    public String toString() {
        return this.getClass().getSimpleName() + "-" + getUrn();
    }

}