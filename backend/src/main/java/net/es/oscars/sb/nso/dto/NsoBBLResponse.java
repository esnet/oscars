package net.es.oscars.sb.nso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import net.es.topo.common.dto.nso.NsoBBL;

import java.util.ArrayList;
import java.util.List;

@Data
public class NsoBBLResponse {
    @JsonProperty("esnet-bbl:bbl")
    List<NsoBBL> nsoBbls = new ArrayList<>();
}
