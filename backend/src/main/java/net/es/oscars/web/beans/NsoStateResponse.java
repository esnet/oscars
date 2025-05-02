package net.es.oscars.web.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Null;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.topo.common.dto.nso.NsoVPLS;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NsoStateResponse {
    @JsonProperty("synchronized")
    @Null
    @Builder.Default
    public boolean isSynchronized = false;

    @JsonProperty("esnet-vpls:vpls")
    @Null
    @Builder.Default
    public List<NsoVPLS> vpls = new ArrayList<>();
}
