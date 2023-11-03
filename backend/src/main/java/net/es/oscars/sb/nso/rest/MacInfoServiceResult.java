package net.es.oscars.sb.nso.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class MacInfoServiceResult extends MacInfoResult {
    private Integer serviceId;

    public MacInfoResult getMacInfoResult() {
        MacInfoResult ret = new MacInfoResult();
        ret.setDevice(this.getDevice());
        ret.setStatus(this.getStatus());
        ret.setErrorMessage(this.getErrorMessage());
        ret.setTimestamp(this.getTimestamp());
        ret.setFdbQueryResult(this.getFdbQueryResult());
        return ret;
    }
}
