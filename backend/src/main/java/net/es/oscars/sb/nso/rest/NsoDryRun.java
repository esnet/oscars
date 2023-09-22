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
public class NsoDryRun {
    @JsonProperty("dry-run-result")
    DryRunResult dryRunResult;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DryRunResult {
        Cli cli;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cli {
        @JsonProperty("local-node")
        LocalNode localNode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocalNode {
        String data;
    }
}

