package net.es.oscars.sb.nso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import net.es.topo.common.dto.nso.NsoSystem;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class NsoSystemResponse {
    @JsonProperty("esnet-system:system")
    @Builder.Default
    List<NsoSystem> nsoSystems = new ArrayList<>();
}

