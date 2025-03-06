package net.es.oscars.sb.nso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.es.topo.common.dto.nso.NsoVPLS;

import java.util.ArrayList;
import java.util.List;

public class NsoVplsResponse {
    @JsonProperty("esnet-vpls:bbl")
    List<NsoVPLS> nsoVpls = new ArrayList<>();

}
