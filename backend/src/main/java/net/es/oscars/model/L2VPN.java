package net.es.oscars.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import net.es.oscars.model.enums.Flavor;
import net.es.oscars.model.enums.QosExcessAction;
import net.es.oscars.model.enums.QosMode;
import net.es.oscars.resv.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Jacksonized
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter

@Entity
@Table
public class L2VPN {
    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @Schema(description = "Human readable name, from '234679CDFGHJKMNPRTWXYZ'", minLength = 4, maxLength = 4, example = "CXA5")
    @Column(unique = true)
    protected String name;

    @Embedded
    protected Meta meta;

    @Embedded
    protected Interval schedule;

    @Embedded
    protected Status status;

    @Embedded
    protected Qos qos;

    @Embedded
    protected Tech tech;

    @OneToMany
    @Builder.Default
    protected List<Bundle> bundles = new ArrayList<>();

    @OneToMany
    @Builder.Default
    protected List<Endpoint> endpoints = new ArrayList<>();


    @Jacksonized
    @Builder
    @Getter
    @Setter
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Qos {
        @Schema(description= "QoS Mode", defaultValue="GUARANTEED", allowableValues = { "GUARANTEED", "BEST_EFFORT", "SCAVENGER" })
        @Builder.Default
        @Enumerated(EnumType.STRING)
        @Column(name = "qos_mode")
        protected QosMode mode = QosMode.GUARANTEED;

        @Schema(description= "QoS Excess Packet action", defaultValue="SCAVENGER", allowableValues = { "SCAVENGER", "BEST_EFFORT", "DROP" })
        @Builder.Default
        @Enumerated(EnumType.STRING)
        @Column(name = "qos_excess_action")
        protected QosExcessAction excessAction = QosExcessAction.SCAVENGER;


        @Schema(description = "Bandwidth in Mbps, only for QoS mode GUARANTEED", defaultValue="0", minimum = "0", maximum = "1000000000", example = "1000")
        @Builder.Default
        protected int bandwidth = 0;
//
//        @Schema(description = "Percent of bandwidth that will be broadcast type. Typically zero.", minimum = "0", maximum = "100", defaultValue="0", example = "0")
//        @Builder.Default
//        protected int broadcastPct = 0;

    }


    @Jacksonized
    @Builder
    @Getter
    @Setter
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        @Schema(description = "Human readable description", minLength = 0, maxLength = 80, example = "LHC primary")
        protected String description;

        @Schema(description = "Username of the last modifier", minLength = 0, maxLength = 32, example = "haniotak")
        protected String username;

        @Schema(description = "Tracking identifier; used in LSP naming. 4-6 alphanumeric characters", minLength = 4, maxLength = 6, example = "CAST01")
        protected String trackingId;

        protected String orchId;

    }


    @Jacksonized
    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class Status {
        @Schema(description = "Current phase", accessMode = Schema.AccessMode.READ_ONLY)
        @Enumerated(EnumType.STRING)

        protected Phase phase;
        @Schema(description = "Current state", accessMode = Schema.AccessMode.READ_ONLY)
        @Enumerated(EnumType.STRING)
        protected State state;

        @Schema(description = "Current deployment state", accessMode = Schema.AccessMode.READ_ONLY)
        @Enumerated(EnumType.STRING)
        protected DeploymentState deploymentState;

        @Schema(description = "Current deployment intent", accessMode = Schema.AccessMode.READ_ONLY)
        @Enumerated(EnumType.STRING)
        protected DeploymentIntent deploymentIntent;
    }


    @Jacksonized
    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class Tech {
        @Builder.Default
        @Schema(description = "Build mode", defaultValue="AUTOMATIC", allowableValues = { "AUTOMATIC", "MANUAL" })
        @Enumerated(EnumType.STRING)
        @Column(name = "build_mode")
        protected BuildMode mode = BuildMode.AUTOMATIC;

        @Builder.Default
        @Schema(description= "VPLS for multipoint, EPIPE for p2p transparent Ethernet", defaultValue="VPLS", allowableValues = { "VPLS", "EPIPE" })
        @Enumerated(EnumType.STRING)
        protected Flavor flavor = Flavor.VPLS;

        @Schema(description = "Connection MTU", minimum = "1500", maximum = "9000", defaultValue="9000", example = "9000")
        @Builder.Default
        private Integer mtu = 9000;

        @Schema(description = "MACSEC passthrough for untagged ports", defaultValue="true", example = "true")
        @Builder.Default
        private boolean macsec = true;

    }


}
