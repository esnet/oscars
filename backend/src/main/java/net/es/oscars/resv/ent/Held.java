package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import jakarta.persistence.*;
import java.time.Instant;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Held {
    @JsonCreator
    public Held(@JsonProperty("connectionId") @NonNull String connectionId,
                @JsonProperty("cmp") @NonNull Components cmp,
                @JsonProperty("expiration") @NonNull Instant expiration,
                @JsonProperty("schedule") @NonNull Schedule schedule) {
        this.connectionId = connectionId;
        this.cmp = cmp;
        this.expiration = expiration;
        this.schedule = schedule;
    }

    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @NonNull
    @Column(unique = true)
    private String connectionId;

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    private Instant expiration;

    @NonNull
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Components cmp;

    @NonNull
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Schedule schedule;


}
