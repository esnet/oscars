package net.es.oscars.sb.nso.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveStatusOutput {

//    @JsonProperty("tailf-ned-alu-sr-stats:output")
    @JsonProperty("esnet-status:output")
    WrappedOutput output;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class WrappedOutput {
        private String result;
    }

    public String getOutput() {
        return output.getResult();
    }

}
