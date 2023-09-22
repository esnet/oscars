package net.es.oscars.sb.ent;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import net.es.oscars.dto.pss.cmd.CommandType;
import net.es.oscars.dto.pss.st.ConfigStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouterCommandHistory {

    @Id
    @GeneratedValue
    private Long id;

    @NonNull
    private String deviceUrn;

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    private Instant date;

    @NonNull
    private String connectionId;

    @NonNull
    private CommandType type;

    @NonNull
    private ConfigStatus configStatus;

    @NonNull
    @Column(length = 65536)
    private String commands;

    @NonNull
    @Column(length = 65536)
    private String output;

    @NonNull
    private String templateVersion;

}
