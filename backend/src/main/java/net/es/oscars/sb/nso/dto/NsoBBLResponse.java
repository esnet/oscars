package net.es.oscars.sb.nso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import net.es.topo.common.dto.nso.NsoBBL;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class NsoBBLResponse {
    @JsonProperty("esnet-bbl:bbl")
    @Builder.Default
    List<NsoBBL> nsoBbls = new ArrayList<>();
}
