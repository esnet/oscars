package net.es.oscars.sb.nso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import net.es.topo.common.dto.nso.NsoVPLS;

import java.util.ArrayList;
import java.util.List;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NsoVplsResponse {
    @JsonProperty("esnet-vpls:vpls")
    @Builder.Default
    List<NsoVPLS> nsoVpls = new ArrayList<>();

}
