package net.es.oscars.nso.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import net.es.topo.common.dto.nso.NsoLSP;

import java.util.ArrayList;
import java.util.List;

@Data
public class NsoLspWrapper {
    @JsonProperty("esnet-lsp:lsp")
    List<NsoLSP> lspInstances = new ArrayList<>();
}
