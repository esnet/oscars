package net.es.oscars.sb.nso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import net.es.topo.common.dto.nso.NsoVPLS;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
public class NsoVplsResponse {
    @JsonProperty("esnet-vpls:vpls")
    List<NsoVPLS> nsoVpls = new ArrayList<>();

}
