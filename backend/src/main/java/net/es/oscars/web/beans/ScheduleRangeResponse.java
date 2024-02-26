package net.es.oscars.web.beans;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import net.es.oscars.topo.beans.IntRange;

import java.time.Instant;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRangeResponse {
    @NonNull
    protected String connectionId;
    @NonNull
    protected ScheduleModifyType type;

    boolean allowed;

    @Builder.Default
    String explanation = "";

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    protected Instant floor;

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    protected Instant ceiling;

}
