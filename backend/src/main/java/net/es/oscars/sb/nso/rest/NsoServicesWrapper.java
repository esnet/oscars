package net.es.oscars.sb.nso.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.NsoVPLS;
import net.es.topo.common.dto.nso.enums.NsoLspPathType;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NsoServicesWrapper {
    @Builder.Default
    @JsonProperty("esnet-lsp:lsp")
    List<NsoLSP> lspInstances = new ArrayList<>();
    @Builder.Default
    @JsonProperty("esnet-vpls:vpls")
    List<NsoVPLS> vplsInstances = new ArrayList<>();

    public String asCliCommands() {
        StringBuilder lspSetCmds = new StringBuilder();

        for (NsoLSP lsp : lspInstances) {
            lspSetCmds.append("set services lsp %s %s routing-domain %s%n".formatted(lsp.getName(), lsp.getDevice(), lsp.getRoutingDomain()));
            lspSetCmds.append("set services lsp %s %s metric %d %n".formatted(lsp.getName(), lsp.getDevice(), lsp.getMetric()));
            lspSetCmds.append("set services lsp %s %s target device %s %n".formatted(lsp.getName(), lsp.getDevice(), lsp.getTarget().getDevice()));
            lspSetCmds.append("set services lsp %s %s primary setup-priority %d %n".formatted(lsp.getName(), lsp.getDevice(), lsp.getPrimary().getSetupPriority()));
            lspSetCmds.append("set services lsp %s %s primary hold-priority %d %n".formatted(lsp.getName(), lsp.getDevice(), lsp.getPrimary().getHoldPriority()));
            lspSetCmds.append("set services lsp %s %s primary path-type %s %n".formatted(lsp.getName(), lsp.getDevice(), lsp.getPrimary().getPathType().toString().toLowerCase()));
            if (lsp.getPrimary().getPathType().equals(NsoLspPathType.STRICT)) {
                for (NsoLSP.Hop hop : lsp.getPrimary().getHop()) {
                    lspSetCmds.append("set services lsp %s %s primary hop %d ipv4 %s %n".formatted(lsp.getName(), lsp.getDevice(), hop.getNumber(), hop.getIpv4()));
                }
            }
        }


        StringBuilder vplsSetCmds = new StringBuilder();
        for (NsoVPLS vpls : vplsInstances) {
            vplsSetCmds.append("set services vpls %d name %s%n".formatted(vpls.getVcId(), vpls.getName()));
            vplsSetCmds.append("set services vpls %d routing-domain %s%n".formatted(vpls.getVcId(), vpls.getRoutingDomain()));
            vplsSetCmds.append("set services vpls %d qos-mode %s%n".formatted(vpls.getVcId(), vpls.getQosMode().toString().toLowerCase()));

            for (NsoVPLS.DeviceContainer dc : vpls.getDevice()) {
                for (NsoVPLS.Endpoint ep : dc.getEndpoint()) {
                    vplsSetCmds.append("set services vpls %d device %s endpoint %s %d layer2-description %s%n".formatted(vpls.getVcId(), dc.getDevice(), ep.getIfce(), ep.getVlanId(), ep.getLayer2Description()));
                    if (ep.getQos() != null) {
                        String cflowd = "";
                        if (ep.getCflowd() != null && ep.getCflowd()) {
                            cflowd = "cflowd";
                        }

                        vplsSetCmds.append("set services vpls %d device %s endpoint %s %d qos qos-id %d ingress-mbps %d egress-mbps %d excess-action %s %s%n"
                                .formatted(vpls.getVcId(), dc.getDevice(), ep.getIfce(), ep.getVlanId(),
                                        ep.getQos().getQosId(), ep.getQos().getIngressMbps(), ep.getQos().getEgressMbps(),
                                        ep.getQos().getExcessAction().toString().toLowerCase(), cflowd));
                    }
                }
                for (NsoVPLS.SDP sdp : vpls.getSdp()) {
                    vplsSetCmds.append("set services vpls %d sdp %d precedence %s%n".formatted(vpls.getVcId(), sdp.getSdpId(), sdp.getPrecedence().toString().toLowerCase()  ));
                    vplsSetCmds.append("set services vpls %d sdp %d a device %s%n".formatted(vpls.getVcId(), sdp.getSdpId(), sdp.getA().getDevice()  ));
                    vplsSetCmds.append("set services vpls %d sdp %d a mode %s%n".formatted(vpls.getVcId(), sdp.getSdpId(), sdp.getA().getMode().toString().toLowerCase()  ));
                    vplsSetCmds.append("set services vpls %d sdp %d a lsp %s%n".formatted(vpls.getVcId(), sdp.getSdpId(), sdp.getA().getLsp()  ));
                    if (sdp.getA().getVcId() != null) {
                        vplsSetCmds.append("set services vpls %d sdp %d a vc-id %d%n".formatted(vpls.getVcId(), sdp.getSdpId(), sdp.getA().getVcId()  ));
                    }
                    vplsSetCmds.append("set services vpls %d sdp %d z device %s%n".formatted(vpls.getVcId(), sdp.getSdpId(), sdp.getZ().getDevice()  ));
                    vplsSetCmds.append("set services vpls %d sdp %d z mode %s%n".formatted(vpls.getVcId(), sdp.getSdpId(), sdp.getZ().getMode().toString().toLowerCase()  ));
                    vplsSetCmds.append("set services vpls %d sdp %d z lsp %s%n".formatted(vpls.getVcId(), sdp.getSdpId(), sdp.getZ().getLsp()  ));
                    if (sdp.getZ().getVcId() != null) {
                        vplsSetCmds.append("set services vpls %d sdp %d z vc-id %d%n".formatted(vpls.getVcId(), sdp.getSdpId(), sdp.getZ().getVcId()  ));
                    }
                }
            }
        }
        return "%s %s".formatted(vplsSetCmds.toString(), lspSetCmds.toString());
    }

}
