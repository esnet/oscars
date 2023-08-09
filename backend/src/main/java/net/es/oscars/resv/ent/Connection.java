package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import net.es.oscars.resv.enums.BuildMode;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.enums.State;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Connection {
    @JsonCreator
    public Connection(@JsonProperty("connectionId") @NonNull String connectionId,
                      @JsonProperty("phase") @NonNull Phase phase,
                      @JsonProperty("mode") @NonNull BuildMode mode,
                      @JsonProperty("state") @NonNull State state,
                      @JsonProperty("username") @NonNull String username,
                      @JsonProperty("description") @NonNull String description,
                      @JsonProperty("reserved") Reserved reserved,
                      @JsonProperty("tags") List<Tag> tags,
                      @JsonProperty("held") Held held,
                      @JsonProperty("archived") Archived archived,
                      @JsonProperty("connection_mtu") Integer connection_mtu,
                      @JsonProperty("last_modified") Integer last_modified) {
        this.connectionId = connectionId;
        this.phase = phase;
        this.mode = mode;
        this.state = state;
        this.username = username;
        this.description = description;
        this.reserved = reserved;
        this.tags = tags;
        this.held = held;
        this.archived = archived;
        this.connection_mtu = connection_mtu;
        this.last_modified = last_modified;
    }


    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @NonNull
    @Column(unique = true)
    private String connectionId;

    @NonNull
    private Phase phase;

    @NonNull
    private BuildMode mode;

    @NonNull
    private State state;

    @NonNull
    private String description;

    @NonNull
    private String username;

    @OneToMany(cascade = CascadeType.ALL)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Tag> tags;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Reserved reserved;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Held held;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Archived archived;

    @NonNull
    private Integer connection_mtu;

    @NonNull
    private Integer last_modified;
}
