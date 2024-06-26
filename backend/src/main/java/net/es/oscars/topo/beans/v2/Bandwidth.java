package net.es.oscars.topo.beans.v2;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Bandwidth {

    @NonNull
    private Unit unit;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Integer available;

    @NonNull
    private Integer physical;

    @JsonGetter
    private Double utilization() {

        if (physical == 0) {
            return 0.0;
        }
        if (available == null) {
            return 0.0;
        }
        BigDecimal bd = BigDecimal.valueOf(1.0 - (available.doubleValue() / physical.doubleValue()));
        bd = bd.setScale(3, RoundingMode.HALF_UP);
        return bd.doubleValue();

    }

    public enum Unit {
        MBPS, GBPS
    }
}

