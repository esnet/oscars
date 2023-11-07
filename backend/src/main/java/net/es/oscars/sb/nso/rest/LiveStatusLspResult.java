package net.es.oscars.sb.nso.rest;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class LiveStatusLspResult extends LiveStatusResult {

    List<LspEntry> lsps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class LspEntry {

        private String name;
        private String to;
        private Integer tunnelId;
        private Boolean fastFailConf;

        private Boolean adminState;
        private Boolean operationalState;

    }

}