package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import net.es.oscars.resv.enums.EventType;

import jakarta.persistence.*;
import java.time.Instant;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    @JsonCreator
    public Event(@JsonProperty("connectionId") @NonNull String connectionId,
                 @JsonProperty("occurrence") @NonNull Instant occurrence,
                 @JsonProperty("type") @NonNull EventType type,
                 @JsonProperty("description") @NonNull String description,
                 @JsonProperty("username") @NonNull String username) {
        this.connectionId = connectionId;
        this.occurrence = occurrence;
        this.type = type;
        this.description = description;
        this.username = username;
    }

    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @NonNull
    private String connectionId;

    @NonNull
    private Instant occurrence;

    @NonNull
    private EventType type;

    @NonNull
    private String description;

    private String username;

}
