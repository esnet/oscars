package net.es.oscars.web.beans;

import lombok.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BandwidthRangeRequest {
    @NonNull
    protected String connectionId;


}
