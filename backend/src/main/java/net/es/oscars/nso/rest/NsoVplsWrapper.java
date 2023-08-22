package net.es.oscars.nso.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import net.es.topo.common.dto.nso.NsoVPLS;

import java.util.ArrayList;
import java.util.List;

@Data
public class NsoVplsWrapper {
    @JsonProperty("esnet-vpls:vpls")
    List<NsoVPLS> vplsInstances = new ArrayList<>();
}
