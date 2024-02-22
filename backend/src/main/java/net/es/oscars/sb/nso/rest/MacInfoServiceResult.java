package net.es.oscars.sb.nso.rest;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper=false)
public class MacInfoServiceResult extends MacInfoResult {
    private Integer serviceId;

    public MacInfoResult getMacInfoResult() {
        return MacInfoResult.builder()
                .device(this.getDevice())
                .status(this.getStatus())
                .errorMessage(this.getErrorMessage())
                .timestamp(this.getTimestamp())
                .fdbQueryResult(this.getFdbQueryResult())
                .build();
    }
}
