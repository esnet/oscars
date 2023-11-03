package net.es.oscars.web.beans;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.es.oscars.sb.nso.rest.LiveStatusResult;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class OperationalStateInfoResponse extends LiveStatusResponse {

    private List<LiveStatusResult> results;

}
