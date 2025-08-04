package net.es.oscars.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import net.es.oscars.topo.enums.UrnType;

@Jacksonized
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
public class Waypoint {
    @GeneratedValue
    @JsonIgnore
    @Id
    private Long id;
    protected String urn;
    protected UrnType type;
}
