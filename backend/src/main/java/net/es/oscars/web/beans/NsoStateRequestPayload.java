package net.es.oscars.web.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.topo.common.dto.nso.NsoVPLS;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NsoStateRequestPayload {
    @JsonProperty("esnet-vpls:vpls")
    private List<NsoVPLS> vpls;
}
