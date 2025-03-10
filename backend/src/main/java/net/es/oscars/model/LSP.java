package net.es.oscars.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import net.es.oscars.model.enums.Role;

import java.util.List;

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


    @ElementCollection
    @CollectionTable(name="path", joinColumns=@JoinColumn(name="lsp_id"))
    @Column(name="path")
    public List<String> path;


    @Schema(description= "The role the LSP plays", defaultValue="PRIMARY", allowableValues = { "PRIMARY", "PROTECT", "TERTIARY" })
    @Enumerated(EnumType.STRING)
    @Builder.Default
    protected Role role = Role.PRIMARY;

}
