package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import net.es.oscars.resv.enums.EthFixtureType;

import jakarta.persistence.*;
import java.util.Set;

@Data
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
    private Set<CommandParam> commandParams;


}
