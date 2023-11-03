package net.es.oscars.sb.nso.rest;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class OperationalStateInfoResult extends LiveStatusResult {

    @JsonProperty("sdp")
    private List<Sdp> sdps;

    @JsonProperty("lsp")
    private List<Lsp> lsps;

    @JsonProperty("sap")
    private List<Sap> saps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper=false)
    private static class Sdp extends StateAttribute {
        Integer id;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper=false)
    private static class Lsp extends StateAttribute {
        String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper=false)
    private static class Sap extends StateAttribute {
        String port;
        Integer vlan;
    }

    private static class StateAttribute {
        @JsonProperty("admin-state")
        Boolean adminState;
        @JsonProperty("oper-state")
        Boolean operationalState;
    }

}
