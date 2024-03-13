package net.es.oscars.web.beans;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import net.es.oscars.sb.nso.rest.OperationalStateInfoResult;
import net.es.topo.common.dto.nso.enums.NsoVplsSdpPrecedence;


@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class OperationalStateInfoResponse extends LiveStatusResponse {

    private OperationalState state;

    private List<OperationalStateInfoResult> results = new ArrayList<>();

    private List<TunnelOpInfo> tunnels = new ArrayList<>();

    private List<EndpointOpInfo> endpoints = new ArrayList<>();


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EndpointOpInfo {
        @JsonProperty("vlan-id")
        int vlanId;
        String device;
        String port;
        OperationalState state;
        @JsonProperty("admin-state")
        UpDown adminState;
        @JsonProperty("oper-state")
        UpDown operState;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TunnelOpInfo {
        OperationalState state;
        String device;
        String remote;
        @JsonProperty("sdp")
        List<SdpOpInfo> sdps;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SdpOpInfo {
        @JsonProperty("sdp-id")
        int sdpId;
        @JsonProperty("vc-id")
        int vcId;
        NsoVplsSdpPrecedence precedence;
        @JsonProperty("admin-state")
        UpDown adminState;
        @JsonProperty("oper-state")
        UpDown operState;
    }

    public enum UpDown {
        UP, DOWN;
    }



}
