package net.es.oscars.web.beans;

import lombok.*;

import java.time.Instant;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BandwidthRangeResponse {
    @NonNull
    protected String connectionId;

    boolean allowed;

    @Builder.Default
    String explanation = "";

    @NonNull
    protected Integer floor;

    @NonNull
    protected Integer ceiling;


}
