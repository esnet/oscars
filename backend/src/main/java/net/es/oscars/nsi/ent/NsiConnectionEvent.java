package net.es.oscars.nsi.ent;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.oscars.nsi.beans.NsiConnectionEventType;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class NsiConnectionEvent {
    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @JsonProperty("id")
    private String nsiConnectionId;

    @Enumerated(EnumType.STRING)
    private NsiConnectionEventType type;

    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant timestamp;

    private String message;
    private int version;

}
