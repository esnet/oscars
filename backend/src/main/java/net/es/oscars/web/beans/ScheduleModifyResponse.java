package net.es.oscars.web.beans;

import lombok.*;
import net.es.oscars.resv.ent.Connection;

import java.time.Instant;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleModifyResponse {
    @NonNull
    protected Boolean success;

    protected String explanation;
    protected Connection connection;
}
