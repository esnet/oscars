package net.es.oscars.sb.nso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import net.es.topo.common.dto.nso.NsoPort;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class NsoPortResponse {
    @JsonProperty("esnet-port:port")
    @Builder.Default
    List<NsoPort> nsoPorts = new ArrayList<>();
}
