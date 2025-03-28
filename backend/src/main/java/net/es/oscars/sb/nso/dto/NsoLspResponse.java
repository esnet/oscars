package net.es.oscars.sb.nso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.NsoVPLS;

import java.util.ArrayList;
import java.util.List;

public class NsoLspResponse {
    @JsonProperty("esnet-lsp:lsp")
    List<NsoLSP> nsoLSPs = new ArrayList<>();

}
