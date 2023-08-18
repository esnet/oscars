package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;
import net.es.oscars.resv.enums.CommandParamIntent;
import net.es.oscars.topo.enums.CommandParamType;

import jakarta.persistence.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "refId")
@Entity
public class CommandParam {
    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @NonNull
    private String urn;

    @NonNull
    @ManyToOne
    private Schedule schedule;

    @NonNull
    private String connectionId;

    @NonNull
    private CommandParamType paramType;

    private String refId;

    private CommandParamIntent intent;
    private String target;

    @NonNull
    private Integer resource;

}
