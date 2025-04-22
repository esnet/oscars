package net.es.oscars.sb.nso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.NsoVPLS;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
public class NsoLspResponse {
    @JsonProperty("esnet-lsp:lsp")
    public List<NsoLSP> nsoLSPs = new ArrayList<>();

}
