package net.es.oscars.sb.nso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.NsoVPLS;

import java.util.ArrayList;
import java.util.List;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NsoLspResponse {
    @JsonProperty("esnet-lsp:lsp")
    @Builder.Default
    public List<NsoLSP> nsoLSPs = new ArrayList<>();

}
