package net.es.oscars.sb.nso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import net.es.topo.common.dto.nso.NsoPort;

import java.util.ArrayList;
import java.util.List;

@Data
public class NsoPortResponse {
    @JsonProperty("esnet-port:port")
    List<NsoPort> nsoPorts = new ArrayList<>();
}
