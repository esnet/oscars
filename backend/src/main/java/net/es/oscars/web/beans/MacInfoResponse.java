package net.es.oscars.web.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import net.es.oscars.sb.nso.rest.MacInfoResult;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class MacInfoResponse extends LiveStatusResponse {

    private List<MacInfoResult> results;

}
