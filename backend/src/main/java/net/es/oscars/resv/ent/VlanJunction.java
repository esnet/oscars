package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

import jakarta.persistence.*;
import java.util.Set;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "refId")
public class VlanJunction {
    @JsonCreator
    public VlanJunction(@JsonProperty("connectionId") String connectionId,
                        @JsonProperty("deviceUrn") @NonNull String deviceUrn,
                        @JsonProperty("vlanId") Vlan vlan,
                        @JsonProperty("schedule") Schedule schedule,
                        @JsonProperty("commandParams") Set<CommandParam> commandParams) {
        this.connectionId = connectionId;
        this.deviceUrn = deviceUrn;
        this.vlan = vlan;
        this.schedule = schedule;
        this.commandParams = commandParams;
    }


    @Id
    @JsonIgnore
    @GeneratedValue
    private Long id;

    // always have a refId; junctions really only exist to be referred to
    @NonNull
    private String refId;

    // mandatory; a junction is always associated with a specific device
    @NonNull
    private String deviceUrn;

    // these will be populated by the system after designing is complete
    @ManyToOne(cascade = CascadeType.ALL)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Schedule schedule;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String connectionId;

    @OneToMany(cascade = CascadeType.ALL)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Set<CommandParam> commandParams;

    // really only for reserving a vlanId at a switch
    @ManyToOne(cascade = CascadeType.ALL)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Vlan vlan;

}
