package net.es.oscars.v12.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.oscars.v12.model.intent.L2VPNIntent;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class L2VPNIntentRequest {
    String connectionId;

    L2VPNIntent intent;
}
