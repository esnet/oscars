package net.es.oscars.web.beans.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.oscars.resv.enums.BuildMode;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.enums.State;
import io.swagger.v3.oas.annotations.media.Schema;
import net.es.oscars.web.beans.Interval;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor

public class L2VPN {
    protected String connectionId;

    protected Meta meta;
    protected Interval schedule;
    protected Status status;
    protected Qos qos;

    private Integer mtu;

    protected List<LSP> lsps = new ArrayList<>();
    protected List<Endpoint> endpoints = new ArrayList<>();


    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Qos {
        @Schema(description= "QoS Mode", allowableValues = { "GUARANTEED", "BEST_EFFORT", "SCAVENGER" })
        protected QosMode mode;
        @Schema(description = "Bandwidth in Mbps, for QoS mode GUARANTEED", minimum = "0", maximum = "1000000000", example = "1000")
        protected int bandwidth;
        @Schema(description = "Percent of bandwidth that will be broadcast type. Typically zero.", minimum = "0", maximum = "100", example = "0")
        protected int broadcastPct;
    }

    public enum QosMode {
        GUARANTEED, BEST_EFFORT, SCAVENGER
    }

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        @Schema(description = "Human readable description", minLength = 0, maxLength = 80, example = "CAST01")
        protected String description;
        @Schema(description = "Username of the last modifier", minLength = 0, maxLength = 32, example = "haniotak")
        protected String username;
        @Schema(description = "Tracking identifier; used in LSP naming. 4-6 alphanumeric characters", minLength = 4, maxLength = 4, example = "CAST01")
        protected String trackingId;
    }


    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Status {
        protected Phase phase;
        protected BuildMode mode;
        protected State state;
    }




}
