package net.es.oscars.nso.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.NsoVPLS;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NsoServicesWrapper {
    @JsonProperty("esnet-lsp:lsp")
    List<NsoLSP> lspInstances;
    @JsonProperty("esnet-vpls:vpls")
    List<NsoVPLS> vplsInstances;

}
