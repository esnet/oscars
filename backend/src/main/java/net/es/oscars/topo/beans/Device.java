package net.es.oscars.topo.beans;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;
import net.es.oscars.dto.topo.DeviceModel;
import net.es.oscars.topo.enums.DeviceType;
import net.es.oscars.topo.enums.Layer;
import net.es.topo.common.model.oscars1.IntRange;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class,
                  property = "urn")
public class Device {

    @NonNull
    private String urn;

    @NonNull
    private DeviceModel model;

    private Integer esdbEquipmentId;

    @NonNull
    @Builder.Default
    private Integer locationId = 0;

    @NonNull
    @Builder.Default
    private Double latitude = 0D;

    @NonNull
    @Builder.Default
    private Double longitude = 0D;

    @NonNull
    @Builder.Default
    private String location = "";

    @NonNull
    private DeviceType type;

    @NonNull
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String ipv4Address;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String ipv6Address;

    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Set<IntRange> reservableVlans = new HashSet<>();

    @Builder.Default
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Set<Layer> capabilities = new HashSet<>();

    @NonNull
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
        return other.getUrn().equals(this.getUrn());
    }


    public String toString() {
        return this.getClass().getSimpleName() + "-" + getUrn();
    }
}