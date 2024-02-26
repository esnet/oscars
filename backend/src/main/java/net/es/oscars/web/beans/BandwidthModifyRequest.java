package net.es.oscars.web.beans;

import lombok.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BandwidthModifyRequest {
    @NonNull
    protected String connectionId;

    @NonNull
    protected Integer bandwidth;
}
