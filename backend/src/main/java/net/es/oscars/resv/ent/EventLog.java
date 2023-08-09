package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventLog {
    @JsonCreator
    public EventLog(@JsonProperty("connectionId") @NonNull String connectionId,
                    @JsonProperty("created") @NonNull Instant created,
                    @JsonProperty("archived") @NonNull Instant archived,
                    @JsonProperty("events") @NonNull List<Event> events) {
        this.connectionId = connectionId;
        this.created = created;
        this.archived = archived;
        this.events = events;
    }

    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @NonNull
    private String connectionId;

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    private Instant created;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    private Instant archived;

    @OneToMany(cascade = CascadeType.ALL)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Event> events;


}
