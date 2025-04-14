package net.es.oscars.web.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NsoSyncResponse {
    @JsonProperty("synchronized")
    public boolean isSynchronized = false;
}
