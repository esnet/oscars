package net.es.oscars.web.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduleRangeRequest {
    @NonNull
    protected String connectionId;
    @NonNull
    protected ScheduleModifyType type;

}
