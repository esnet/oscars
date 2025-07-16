package net.es.oscars.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import net.es.oscars.model.enums.Role;

import java.util.List;
import java.util.stream.Collectors;

@Jacksonized
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter

@Entity
@Table
public class LSP {
    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_id")
    @JsonIgnore
    private Bundle bundle;


    @OneToMany
    public List<Waypoint> path;

    public List<String> pathUrns() {
        return path.stream().map(Waypoint::getUrn).collect(Collectors.toList());
    }

    @Schema(description= "The role the LSP plays", defaultValue="PRIMARY", allowableValues = { "PRIMARY", "PROTECT", "TERTIARY" })
    @Enumerated(EnumType.STRING)
    @Builder.Default
    protected Role role = Role.PRIMARY;

}
