package net.es.oscars.web.beans;

import lombok.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BandwidthAvailabilityResponse {
    @NonNull
    protected Integer available;
    @NonNull
    protected Integer baseline;


}
