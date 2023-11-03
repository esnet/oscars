package net.es.oscars.sb.nso.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class MacInfoResult extends LiveStatusResult {

    @JsonProperty("fdb")
    private String fdbQueryResult = null;

}
