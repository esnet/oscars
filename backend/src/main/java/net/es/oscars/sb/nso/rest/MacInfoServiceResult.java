package net.es.oscars.sb.nso.rest;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
