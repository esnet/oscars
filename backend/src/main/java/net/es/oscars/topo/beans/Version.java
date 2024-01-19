package net.es.oscars.topo.beans;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Version {

    @NonNull
    private Boolean valid;

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    private Instant updated;
}
