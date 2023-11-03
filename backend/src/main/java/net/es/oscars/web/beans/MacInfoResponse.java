package net.es.oscars.web.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import net.es.oscars.sb.nso.rest.MacInfoResult;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MacInfoResponse extends LiveStatusResponse {

    private List<MacInfoResult> results;

}
