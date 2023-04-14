package net.es.oscars.pss.equip;

import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PSSException;
import net.es.oscars.pss.params.*;
import net.es.oscars.pss.params.alu.*;
import net.es.oscars.pss.beans.*;
import net.es.oscars.pss.svc.KeywordValidator;
import net.es.oscars.pss.tpl.Assembler;
import net.es.oscars.pss.tpl.Stringifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
public class AluCommandGenerator {
    private Stringifier stringifier;
    private Assembler assembler;
    private KeywordValidator validator;

    @Autowired
    public AluCommandGenerator(Stringifier stringifier, Assembler assembler, KeywordValidator validator) {
        this.stringifier = stringifier;
        this.assembler = assembler;
        this.validator = validator;
    }


    public List<String> getTemplateFilenames() {
        List<String> result = new ArrayList<>();

        result.add("alu/alu-top.ftl");
        result.add("alu/build-alu-mpls-lsp.ftl");
        result.add("alu/build-alu-qos.ftl");
        result.add("alu/build-alu-sdp.ftl");
        result.add("alu/build-alu-mpls-path.ftl");
        result.add("alu/build-alu-vpls-service.ftl");
        result.add("alu/build-alu-vpls-loopback.ftl");

        result.add("alu/dismantle-alu-mpls-lsp.ftl");
        result.add("alu/dismantle-alu-qos.ftl");
        result.add("alu/dismantle-alu-sdp.ftl");
        result.add("alu/dismantle-alu-mpls-path.ftl");
        result.add("alu/dismantle-alu-vpls-service.ftl");
        result.add("alu/dismantle-alu-vpls-loopback.ftl");

        return result;
    }


    public void validate(AluParams params) throws PSSException {
        if (params == null) {
            throw new PSSException("null ALU params!");
        }
        this.protectVsNulls(params);
        this.verifyKeywords(params);
        this.verifyAluQosParams(params);
        this.verifySdpIds(params);
        this.verifyPaths(params);

    }

    public String show(AluParams params) throws PSSException {
        this.validate(params);
        String top = "alu/show.ftl";

        Map<String, Object> root = new HashMap<>();
        root.put("paths", params.getPaths());
        root.put("lsps", params.getLsps());
        root.put("sdps", params.getSdps());
        root.put("vpls", params.getAluVpls());
        try {
            return stringifier.stringify(root, top).getProcessed();
        } catch (IOException | TemplateException ex) {
            log.error("templating error", ex);
            throw new PSSException("template system error");
        }

    }


    public String dismantle(AluParams params) throws PSSException {

        this.validate(params);
        AluTemplatePaths atp = AluTemplatePaths.builder()
                .lsp("alu/dismantle-alu-mpls-lsp.ftl")
                .qos("alu/dismantle-alu-qos.ftl")
                .sdp("alu/dismantle-alu-sdp.ftl")
                .path("alu/dismantle-alu-mpls-path.ftl")
                .vpls("alu/dismantle-alu-vpls-service.ftl")
                .loopback("alu/dismantle-alu-vpls-loopback.ftl")
                .build();
        return fill(atp, params, true);
    }


    public String build(AluParams params) throws PSSException {
        this.validate(params);

        AluTemplatePaths atp = AluTemplatePaths.builder()
                .lsp("alu/build-alu-mpls-lsp.ftl")
                .qos("alu/build-alu-qos.ftl")
                .sdp("alu/build-alu-sdp.ftl")
                .path("alu/build-alu-mpls-path.ftl")
                .vpls("alu/build-alu-vpls-service.ftl")
                .loopback("alu/build-alu-vpls-loopback.ftl")
                .build();
        return fill(atp, params, false);
    }

    private String fill(AluTemplatePaths atp, AluParams params, boolean reverse) throws PSSException {

        String top = "alu/alu-top.ftl";

        Map<String, Object> root = new HashMap<>();

        List<String> fragments = new ArrayList<>();

        try {

            if (params.getQoses() == null || params.getQoses().isEmpty()) {
                log.info("No QOS config (weird!)");
            } else {
                root.put("qosList", params.getQoses());
                root.put("apply", params.getApplyQos());
                TemplateOutput qosConfig = stringifier.stringify(root, atp.getQos());
                if (reverse) {
                    fragments.add(0, qosConfig.getProcessed());
                } else {
                    fragments.add(qosConfig.getProcessed());
                }
            }

            if (params.getPaths() == null || params.getPaths().isEmpty()) {
                log.debug("No paths, skipping..");

            } else {
                root = new HashMap<>();
                root.put("paths", params.getPaths());
                TemplateOutput pathConfig = stringifier.stringify(root, atp.getPath());
                if (reverse) {
                    fragments.add(0, pathConfig.getProcessed());
                } else {
                    fragments.add(pathConfig.getProcessed());
                }
            }

            if (params.getLsps() == null || params.getLsps().isEmpty()) {
                log.debug("No LSPs, skipping..");
            } else {
                root = new HashMap<>();
                root.put("lsps", params.getLsps());
                TemplateOutput lspConfig = stringifier.stringify(root, atp.getLsp());
                if (reverse) {
                    fragments.add(0, lspConfig.getProcessed());
                } else {
                    fragments.add(lspConfig.getProcessed());
                }
            }

            if (params.getSdps() == null || params.getSdps().isEmpty()) {
                log.debug("No SDPs, skipping..");
            } else {
                root = new HashMap<>();
                root.put("sdps", params.getSdps());
                TemplateOutput sdpConfig = stringifier.stringify(root, atp.getSdp());
                if (reverse) {
                    fragments.add(0, sdpConfig.getProcessed());
                } else {
                    fragments.add(sdpConfig.getProcessed());
                }
            }

            if (params.getLoopbackInterface() == null) {
                log.debug("No loopback, skipping..");
            } else {
                root = new HashMap<>();
                root.put("loopback_ifce_name", params.getLoopbackInterface());
                root.put("loopback_address", params.getLoopbackAddress());
                TemplateOutput loopbackCfg = stringifier.stringify(root, atp.getLoopback());
                if (reverse) {
                    fragments.add(0, loopbackCfg.getProcessed());
                } else {
                    fragments.add(loopbackCfg.getProcessed());
                }
            }


            root = new HashMap<>();
            root.put("vpls", params.getAluVpls());
            TemplateOutput vplsServiceConfig = stringifier.stringify(root, atp.getVpls());
            if (reverse) {
                fragments.add(0, vplsServiceConfig.getProcessed());
            } else {
                fragments.add(vplsServiceConfig.getProcessed());
            }
            return assembler.assemble(fragments, top);
        } catch (IOException | TemplateException ex) {
            log.error("templating error", ex);
            throw new PSSException("template system error");
        }
    }


    private void protectVsNulls(AluParams params) {

        if (params == null) {
            log.error("whoa whoa whoa there, no passing null params!");
            params = AluParams.builder().build();
        }
        if (params.getAluVpls() == null) {
            params.setAluVpls(AluVpls.builder().build());
        }
        if (params.getSdps() == null) {
            params.setSdps(new ArrayList<>());
        }
        if (params.getAluVpls().getSdpToVcIds() == null) {
            params.getAluVpls().setSdpToVcIds(new ArrayList<>());
        }
        if (params.getSdps() == null) {
            params.setSdps(new ArrayList<>());
        }
        if (params.getPaths() == null) {
            params.setPaths(new ArrayList<>());
        }
        if (params.getLsps() == null) {
            params.setLsps(new ArrayList<>());
        }
    }

    private void verifyPaths(AluParams params) throws PSSException {


        Set<String> lspNamesFromSdps = new HashSet<>();
        Set<String> lspNamesFromLsps = new HashSet<>();

        Set<String> pathNamesFromLsps = new HashSet<>();
        Set<String> pathNamesFromPaths = new HashSet<>();


        for (AluSdp sdp : params.getSdps()) {
            lspNamesFromSdps.add(sdp.getLspName());
        }
        for (Lsp lsp : params.getLsps()) {
            lspNamesFromLsps.add(lsp.getName());
            pathNamesFromLsps.add(lsp.getPathName());
        }
        for (MplsPath path : params.getPaths()) {
            pathNamesFromPaths.add(path.getName());
        }
        if (!lspNamesFromLsps.equals(lspNamesFromSdps)) {
            throw new PSSException("LSP name mismatch");
        }
        if (!pathNamesFromLsps.equals(pathNamesFromPaths)) {
            throw new PSSException("Path name mismatch");
        }


    }

    private void verifySdpIds(AluParams params) throws PSSException {
        List<AluSdpToVcId> aluSdpToVcIds = params.getAluVpls().getSdpToVcIds();
        Set<Integer> sdpIdsA = new HashSet<>();
        Set<Integer> sdpIdsB = new HashSet<>();
        for (AluSdpToVcId aluSdpToVcId : aluSdpToVcIds) {
            sdpIdsA.add(aluSdpToVcId.getSdpId());
        }
        for (AluSdp sdp : params.getSdps()) {
            sdpIdsB.add(sdp.getSdpId());
        }
        if (!sdpIdsA.equals(sdpIdsB)) {
            throw new PSSException("SDP ID mismatch!");
        }

    }

    private void verifyAluQosParams(AluParams params) throws PSSException {
        List<AluSap> saps = params.getAluVpls().getSaps();
        List<AluQos> qoses = params.getQoses();
        Set<Integer> sapInQosIds = new HashSet<>();
        Set<Integer> sapEgQosIds = new HashSet<>();
        saps.forEach(sap -> {
            if (sap.getIngressQosId() != null) {
                sapInQosIds.add(sap.getIngressQosId());
            }
            if (sap.getEgressQosId() != null) {
                sapEgQosIds.add(sap.getEgressQosId());
            }
        });
        Set<Integer> inQosIds = new HashSet<>();
        Set<Integer> egQosIds = new HashSet<>();
        for (AluQos qos : qoses) {
            if (qos.getPolicyId() == null) {
                throw new PSSException("qos policy id missing");
            }
            if (qos.getType() == null) {
                throw new PSSException("qos type missing");
            }
            if (qos.getType().equals(AluQosType.SAP_INGRESS)) {
                if (inQosIds.contains(qos.getPolicyId())) {
                    throw new PSSException("duplicate ingress qos policy id " + qos.getPolicyId());
                }
                inQosIds.add(qos.getPolicyId());
            } else if (qos.getType().equals(AluQosType.SAP_EGRESS)) {
                if (egQosIds.contains(qos.getPolicyId())) {
                    throw new PSSException("duplicate egress qos policy id" + qos.getPolicyId());
                }
                egQosIds.add(qos.getPolicyId());
            }
        }
        boolean ok = true;
        String error = "";
        if (!sapInQosIds.equals(inQosIds)) {
            ok = false;
            error = "Ingress qos id mismatch";
        }
        if (!sapEgQosIds.equals(egQosIds)) {
            ok = false;
            error += " Egress qos id mismatch";
        }
        if (!ok) {
            throw new PSSException(error);
        }


    }
    private void verifyKeywords(AluParams params) throws PSSException {
        StringBuilder errorStr = new StringBuilder("");
        boolean hasError = false;

        Map<KeywordWithContext, KeywordValidationCriteria> keywordMap = new HashMap<>();
        KeywordValidationCriteria alphanum_criteria = KeywordValidationCriteria.builder()
                .format(KeywordFormat.ALPHANUMERIC_DASH_UNDERSCORE)
                .length(32)
                .build();
        KeywordValidationCriteria ip_criteria = KeywordValidationCriteria.builder()
                .format(KeywordFormat.IPV4_ADDRESS)
                .length(32)
                .build();

        keywordMap.put(KeywordWithContext.builder()
                .context("VPLS service name")
                .keyword(params.getAluVpls().getServiceName())
                .build(), alphanum_criteria);
        for (AluSap aluSap : params.getAluVpls().getSaps()) {
            if (aluSap.getVlan() < 2 || aluSap.getVlan() > 4094) {
                String err = aluSap.getPort() + " : vlan " + aluSap.getVlan() + " out of range (2-4094)\n";
                errorStr.append(err);
                hasError = true;
            }
        }

        if (params.getLoopbackAddress() != null) {
            keywordMap.put(KeywordWithContext.builder()
                    .context("loopback address")
                    .keyword(params.getLoopbackAddress())
                    .build(), ip_criteria);
        }
        if (params.getAluVpls().getEndpointNames() != null) {

            for (String endpointName : params.getAluVpls().getEndpointNames()) {
                keywordMap.put(KeywordWithContext.builder()
                        .context("VPLS endpoint name")
                        .keyword(endpointName)
                        .build(), alphanum_criteria);
            }
        }

        for (Lsp lsp : params.getLsps()) {

            keywordMap.put(KeywordWithContext.builder()
                    .context("LSP path name").keyword(lsp.getPathName())
                    .build(), alphanum_criteria);
            keywordMap.put(KeywordWithContext.builder()
                    .context("LSP name").keyword(lsp.getName())
                    .build(), alphanum_criteria);
            keywordMap.put(KeywordWithContext.builder()
                    .context("LSP to").keyword(lsp.getTo())
                    .build(), ip_criteria);
        }

        for (AluSdp sdp : params.getSdps()) {
            keywordMap.put(KeywordWithContext.builder()
                    .context("SDP far end ").keyword(sdp.getFarEnd())
                    .build(), ip_criteria);
            keywordMap.put(KeywordWithContext.builder()
                    .context("SDP LSP name").keyword(sdp.getLspName())
                    .build(), alphanum_criteria);
        }
        for (AluQos qos : params.getQoses()) {
            keywordMap.put(KeywordWithContext.builder()
                    .context("QoS policy name").keyword(qos.getPolicyName())
                    .build(), alphanum_criteria);
        }

        for (MplsPath path : params.getPaths()) {
            keywordMap.put(KeywordWithContext.builder()
                    .context("MPLS path name").keyword(path.getName())
                    .build(), alphanum_criteria);

            if (path.getHops() != null) {
                for (MplsHop hop : path.getHops()) {
                    keywordMap.put(KeywordWithContext.builder()
                            .context("MPLS hop address").keyword(hop.getAddress())
                            .build(), ip_criteria);
                }
            }
        }

        Map<KeywordWithContext, KeywordValidationResult> results = validator.validate(keywordMap);
        KeywordValidationResult overall = validator.gatherErrors(results);
        errorStr.append(overall.getError());
        if (!overall.getValid()) {
            hasError = true;
        }

        if (hasError) {
            log.error(errorStr.toString());
            throw new PSSException(errorStr.toString());
        }

    }


}
