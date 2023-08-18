package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import jakarta.persistence.*;
import java.util.List;

@Data
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
    private List<VlanJunction> junctions;

    @NonNull
    @OneToMany(cascade = CascadeType.ALL)
    private List<VlanFixture> fixtures;

    @OneToMany(cascade = CascadeType.ALL)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<VlanPipe> pipes;

}
