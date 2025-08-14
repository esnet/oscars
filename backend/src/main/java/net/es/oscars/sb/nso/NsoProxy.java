package net.es.oscars.sb.nso;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.spring.web.v3_1.SpringWebTelemetry;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.StartupProperties;
import net.es.oscars.app.util.HeaderRequestInterceptor;

import net.es.oscars.sb.nso.exc.NsoCommitException;
import net.es.oscars.app.props.NsoProperties;
import net.es.oscars.sb.nso.exc.NsoDryrunException;
import net.es.oscars.sb.nso.rest.NsoDryRun;
import net.es.oscars.sb.nso.rest.NsoResponseErrorHandler;
import net.es.oscars.sb.nso.rest.NsoServicesWrapper;
import net.es.oscars.sb.nso.rest.LiveStatusRequest;
import net.es.oscars.sb.nso.rest.LiveStatusMockData;
import net.es.oscars.sb.nso.rest.LiveStatusOutput;
import net.es.topo.common.devel.DevelUtils;
import net.es.topo.common.dto.nso.*;

import net.es.topo.common.dto.nso.enums.NsoCheckSyncState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import net.es.topo.common.dto.nso.enums.NsoService;
import net.es.oscars.sb.nso.dto.NsoLspResponse;
import net.es.oscars.sb.nso.dto.NsoVplsResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

@Slf4j
@Component
public class NsoProxy {

    private final NsoProperties props;
    private final StartupProperties startupProperties;

    @Getter
    @Setter
    static NsoResponseErrorHandler restErrorHandler = new NsoResponseErrorHandler();

    @Getter
    @Setter
    static NsoResponseErrorHandler patchErrorHandler = new NsoResponseErrorHandler();

    @Setter
    private RestTemplate restTemplate;
    private RestTemplate patchTemplate;
    final OpenTelemetry openTelemetry;


    @Autowired
    public NsoProxy(NsoProperties props, StartupProperties startupProperties, RestTemplateBuilder builder, OpenTelemetry openTelemetry) {

        this.props = props;
        this.startupProperties = startupProperties;
        this.openTelemetry = openTelemetry;
        try {
            // make sure we don't send empty values
            ObjectMapper customObjectMapper = new ObjectMapper();
            customObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
            converter.setObjectMapper(customObjectMapper);
            SpringWebTelemetry telemetry = SpringWebTelemetry.create(openTelemetry);

            this.restTemplate = builder.build();
            restTemplate.setErrorHandler(restErrorHandler);
            restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(props.getUsername(), props.getPassword()));
            restTemplate.getInterceptors().add(new HeaderRequestInterceptor(HttpHeaders.ACCEPT, "application/yang-data+json"));
            restTemplate.getInterceptors().add(new HeaderRequestInterceptor(HttpHeaders.CONTENT_TYPE, "application/yang-data+json"));
            restTemplate.getInterceptors().add(telemetry.newInterceptor());
            restTemplate.getMessageConverters().add(0, converter);


            // different http client for yang patch
            this.patchTemplate = builder.build();
            patchTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
            patchTemplate.setErrorHandler(patchErrorHandler);
            patchTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(props.getUsername(), props.getPassword()));
            patchTemplate.getInterceptors().add(new HeaderRequestInterceptor(HttpHeaders.ACCEPT, "application/yang-data+json"));
            patchTemplate.getInterceptors().add(new HeaderRequestInterceptor(HttpHeaders.CONTENT_TYPE, "application/yang-patch+json"));
            patchTemplate.getInterceptors().add(telemetry.newInterceptor());

        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        log.info("NSO server base URI: " + props.getUri());
    }

    @Retryable(backoff = @Backoff(delayExpression = "${nso.backoff-milliseconds}"), maxAttemptsExpression = "${nso.retry-attempts}")
    public void deleteServices(NsoAdapter.NsoOscarsDismantle dismantle) throws NsoCommitException {
        YangPatchWrapper wrapped = makeDismantleYangPatch(dismantle);
        String rollbackLabel = dismantle.getConnectionId() + "-dismantle";
        submitYangPatch(wrapped, rollbackLabel);

    }

    @Retryable(backoff = @Backoff(delayExpression = "${nso.backoff-milliseconds}"), maxAttemptsExpression = "${nso.retry-attempts}")
    public void redeployServices(NsoServicesWrapper wrapper, String connectionId) throws NsoCommitException {
        log.info("redeploying services");
        YangPatchWrapper wrapped = makeRedeployYangPatch(wrapper, connectionId);
        String rollbackLabel = connectionId + "-redeploy";
        submitYangPatch(wrapped, rollbackLabel);
    }

    public void submitYangPatch(YangPatchWrapper wrapped, String rollbackLabel) throws NsoCommitException {
        if (startupProperties.getStandalone()) {
            log.info("standalone mode - skipping southbound");
            return;
        }
        DevelUtils.dumpDebug("yang patch", wrapped);
        String path = "restconf/data/";
        String restPath = props.getUri() + path + "?rollback-label=" + rollbackLabel;

        final HttpEntity<YangPatchWrapper> entity = new HttpEntity<>(wrapped);
        UUID errorUuid = UUID.randomUUID();
        String errorRef = "Error reference: [" + errorUuid + "]\n";

        try {
            log.info("submitting yang patch to " + restPath);
            ResponseEntity<String> response = patchTemplate.exchange(restPath, HttpMethod.PATCH, entity, String.class);
            if (response.getStatusCode().isError()) {
                log.error("raw error: " + response.getBody() + "\n" + response.getHeaders());
                StringBuilder errorStr = new StringBuilder();
                try {
                    YangPatchErrorResponse errorResponse = new ObjectMapper().readValue(response.getBody(), YangPatchErrorResponse.class);
                    for (YangPatchErrorResponse.YangPatchError errObj : errorResponse.getStatus().getErrors().getErrorList()) {
                        errorStr.append(errObj.getErrorMessage()).append("\n");
                    }
                } catch (JsonProcessingException ex) {
                    log.error(errorRef + ex.getMessage() + "\n" + response.getBody());
                    throw new NsoCommitException(errorRef + "Unable to YANG patch. NSO response parse error.");
                }
                log.error(errorRef + "Unable to YANG patch. NSO error(s): " + errorStr);
                throw new NsoCommitException(errorRef + "Unable to YANG patch. NSO error(s): " + errorStr);
            }
        } catch (RestClientException ex) {
            log.error(errorRef + "REST error %s".formatted(ex.getMessage()));
            throw new NsoCommitException(errorRef + " REST Error: %s".formatted(ex.getMessage()));
        }
    }

    public NsoCheckSyncState checkSync(String device) {
        if (startupProperties.getStandalone()) {
            log.info("standalone mode - returning in-sync for {}", device);
            return NsoCheckSyncState.IN_SYNC;
        }
        String path = "restconf/data/tailf-ncs:devices/device="+device+"/check-sync";
        String restPath = props.getUri() + path;
        log.info("checking sync "+restPath);
        ResponseEntity<FromNsoCheckSync> response = restTemplate.postForEntity(restPath, null, FromNsoCheckSync.class);

        if (response.getStatusCode().isError()) {
            log.error("REST error during check-sync for {} : {}", device, response.getBody());
            return NsoCheckSyncState.UNKNOWN;
        } else {
            if (response.getBody() == null) {
                log.error("empty check-sync for {} : null body", device);
                return NsoCheckSyncState.UNKNOWN;
            } else if (response.getBody().getOutput() == null) {
                log.error("empty check-sync for {} : null output", device);
                return NsoCheckSyncState.UNKNOWN;
            } else if (response.getBody().getOutput().getResult() == null) {
                log.error("empty check-sync for {} : null result", device);
                return NsoCheckSyncState.UNKNOWN;
            } else {
                if (response.getBody().getOutput().getResult().equals(NsoCheckSyncState.ERROR)) {
                    log.error("NSO error during check-sync for {} : {}", device, response.getBody().getOutput().getInfo());
                }
                return response.getBody().getOutput().getResult();
            }
        }


    }

    @Retryable(backoff = @Backoff(delayExpression = "${nso.backoff-milliseconds}"), maxAttemptsExpression = "${nso.retry-attempts}")
    public void buildServices(NsoServicesWrapper wrapper, String connectionId) throws NsoCommitException {
        if (startupProperties.getStandalone()) {
            log.info("standalone mode - skipping southbound for BUILD {}", connectionId);
            return;
        }

        String rollbackLabel = connectionId+"-build";
        String path = "restconf/data/tailf-ncs:services";
        String restPath = props.getUri() + path + "?rollback-label="+rollbackLabel;
        UUID errorUuid = UUID.randomUUID();
        String errorRef = "Error reference: [" + errorUuid + "]\n";
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        DevelUtils.dumpDebug("build services", wrapper);

        try {
//            DevelUtils.dumpDebug("commit", wrapper);
            ResponseEntity<IetfRestconfErrorResponse> response = restTemplate.postForEntity(restPath, wrapper, IetfRestconfErrorResponse.class);

            if (response.getStatusCode().isError()) {
                log.error("raw error: " + response.getBody());
                StringBuilder errorStr = new StringBuilder();
                if (response.getBody() != null) {
                    for (IetfRestconfErrorResponse.IetfError errObj : response.getBody().getErrors().getErrorList()) {
                        errorStr.append(errObj.getErrorMessage()).append("\n");
                    }

                } else {
                    errorStr.append("empty response body\n");
                }
                log.error(errorRef + "Unable to commit. NSO error(s): " + errorStr);
                throw new NsoCommitException("Unable to commit. NSO error(s): " + errorStr);
            }
        } catch (RestClientException ex) {
            log.error(errorRef + "REST error %s".formatted(ex.getMessage()));
            if (ex instanceof RestClientResponseException) {
                log.info("Response body:\n" + ((RestClientResponseException) ex).getResponseBodyAsString());
            }
            ex.printStackTrace(pw);
            log.error(sw.toString());
            throw new NsoCommitException(ex.getMessage());
        }
    }

    @Retryable(backoff = @Backoff(delayExpression = "${nso.backoff-milliseconds}"), maxAttemptsExpression = "${nso.retry-attempts}")
    public void syncFrom(String device) {
        if (startupProperties.getStandalone()) {
            log.info("standalone mode - skipping southbound");
            return;
        }

        String path = "restconf/data/tailf-ncs:devices/device=%s/sync-from".formatted(device);
        String restPath = props.getUri() + path;
        restTemplate.postForLocation(restPath, HttpEntity.EMPTY);
    }

    @Retryable(backoff = @Backoff(delayExpression = "${nso.backoff-milliseconds}"), maxAttemptsExpression = "${nso.retry-attempts}")
    public String buildDryRun(NsoServicesWrapper wrapper) throws NsoDryrunException {
        if (startupProperties.getStandalone()) {
            log.info("standalone mode - skipping southbound");
            return "standalone dry run";
        }

        String path = "restconf/data/tailf-ncs:services?dry-run=cli&commit-queue=async";
        String restPath = props.getUri() + path;
        UUID errorUuid = UUID.randomUUID();
        String errorRef = "Error reference: [" + errorUuid + "]\n";

        try {
            ResponseEntity<NsoDryRun> dryRunResponse = restTemplate.postForEntity(restPath, wrapper, NsoDryRun.class);
            if (dryRunResponse.getStatusCode().isError()) {
                log.error("raw error: " + dryRunResponse.getBody());
                throw new NsoDryrunException("unable to perform dry run " + dryRunResponse.getBody());
            } else {
                if (dryRunResponse.getBody() == null) {
                    return "Null dry run body";
                } else if (dryRunResponse.getBody().getDryRunResult() == null) {
                    return "Null dry run result";
                } else {
                    return dryRunResponse.getBody().getDryRunResult().toString();
                }
            }
        } catch (RestClientException ex) {
            log.error(errorRef + "REST error %s".formatted(ex.getMessage()));
            throw new NsoDryrunException(ex.getMessage()+" "+errorRef);
        }
    }

    @Retryable(backoff = @Backoff(delayExpression = "${nso.backoff-milliseconds}"), maxAttemptsExpression = "${nso.retry-attempts}")
    public String dismantleDryRun(NsoAdapter.NsoOscarsDismantle dismantle) throws NsoDryrunException {
        if (startupProperties.getStandalone()) {
            log.info("standalone mode - skipping southbound");
            return "standalone dry run";
        }

        YangPatchWrapper wrapped = makeDismantleYangPatch(dismantle);
        return this.yangPatchDryRun(wrapped);
    }


    @Retryable(backoff = @Backoff(delayExpression = "${nso.backoff-milliseconds}"), maxAttemptsExpression = "${nso.retry-attempts}")
    public String redeployDryRun(NsoServicesWrapper wrapper, String connectionId) throws NsoDryrunException {
        if (startupProperties.getStandalone()) {
            log.info("standalone mode - skipping southbound");
            return "standalone dry run";
        }
        YangPatchWrapper wrapped = makeRedeployYangPatch(wrapper, connectionId);
        return this.yangPatchDryRun(wrapped);
    }

    private String yangPatchDryRun(YangPatchWrapper wrapped) throws NsoDryrunException {

        String path = "restconf/data?dry-run=cli&commit-queue=async";
        String restPath = props.getUri() + path;

        final HttpEntity<YangPatchWrapper> entity = new HttpEntity<>(wrapped);
        UUID errorUuid = UUID.randomUUID();
        String errorRef = "Error reference: [" + errorUuid + "]\n";

        try {
            NsoDryRun response = patchTemplate.patchForObject(restPath, entity, NsoDryRun.class);
            if (response != null && response.getDryRunResult() != null) {
                log.info(response.getDryRunResult().getCli().getLocalNode().getData());
                return response.getDryRunResult().getCli().getLocalNode().getData();
            } else {
                return "no dry-run available";
            }
        } catch (RestClientException ex) {
            log.error(errorRef + "REST error %s".formatted(ex.getMessage()));
            throw new NsoDryrunException(ex.getMessage());
        }
    }

    public static YangPatchWrapper makeDismantleYangPatch(NsoAdapter.NsoOscarsDismantle dismantle) {
        List<YangPatch.YangEdit> edits = new ArrayList<>();
        edits.add(YangPatch.YangEdit.builder()
                .editId("delete " + dismantle.getVcId())
                .operation("delete")
                .target("/tailf-ncs:services/esnet-vpls:vpls=" + dismantle.getVcId())
                .build());
        for (String lspInstanceKey : dismantle.getLspNsoKeys()) {
            edits.add(YangPatch.YangEdit.builder()
                    .editId("delete " + lspInstanceKey)
                    .operation("delete")
                    .target("/tailf-ncs:services/esnet-lsp:lsp=" + lspInstanceKey)
                    .build());
        }
        YangPatch deletePatch = YangPatch.builder()
                .patchId("delete VPLS and LSP for " + dismantle.getConnectionId())
                .edit(edits)
                .build();


        return YangPatchWrapper.builder().patch(deletePatch).build();
    }

    public static YangPatchWrapper makeDismantleLspYangPatch(String lspInstanceKey) {
        List<YangPatch.YangEdit> edits = new ArrayList<>();
        edits.add(YangPatch.YangEdit.builder()
                .editId("delete " + lspInstanceKey)
                .operation("delete")
                .target("/tailf-ncs:services/esnet-lsp:lsp=" + lspInstanceKey)
                .build());
        YangPatch deletePatch = YangPatch.builder()
                .patchId("delete LSP " + lspInstanceKey)
                .edit(edits)
                .build();


        return YangPatchWrapper.builder().patch(deletePatch).build();
    }

    public static YangPatchWrapper makeRedeployLspYangPatch(NsoLSP lsp ) {
        List<YangPatch.YangEdit> edits = new ArrayList<>();

        String lspKeyArg = '=' + lsp.instanceKey();
        String path = "/tailf-ncs:services/esnet-lsp:lsp" + lspKeyArg;

        YangPatchLspWrapper lspWrapper = YangPatchLspWrapper
            .builder()
            .lsp(lsp)
            .build();

        edits.add(
            YangPatch
                .YangEdit
                .builder()
                .editId("replace " + lsp.instanceKey())
                .operation("replace")
                .value( lspWrapper )
                .target(path)
                .build()
        );

        YangPatch replacePatch = YangPatch
            .builder()
            .patchId("replace LSP " + lsp.instanceKey())
            .edit(edits)
            .build();

        return YangPatchWrapper.builder().patch(replacePatch).build();
    }

    public void deleteLsp(YangPatchWrapper yangPatchWrapper, String lspInstanceKey) throws Exception {
        String rollbackLabel = lspInstanceKey + "-dismantle";
        submitYangPatch(yangPatchWrapper, rollbackLabel);
    }

    public void redeployLsp(YangPatchWrapper yangPatchWrapper, String lspInstanceKey) throws Exception {
        String rollbackLabel = lspInstanceKey + "-replace";
        submitYangPatch(yangPatchWrapper, rollbackLabel);
    }

    public static YangPatchWrapper makeRedeployYangPatch(NsoServicesWrapper wrapper, String connectionId) {
        List<YangPatch.YangEdit> edits = new ArrayList<>();
        if (wrapper.getVplsInstances() != null) {
            for (NsoVPLS vpls: wrapper.getVplsInstances()) {
                // TODO: (maybe) modify something else than the VPLS service endpoints
                int vcid = vpls.getVcId();
                int i = 0;
                for (NsoVPLS.DeviceContainer dc : vpls.getDevice()) {
                    String vplsKey = "=" + vcid;
                    String devKey = "=" + dc.getDevice();

                    String path = "/tailf-ncs:services/esnet-vpls:vpls" + vplsKey + "/device" + devKey;

                    YangPatchDeviceWrapper deviceWrapper = YangPatchDeviceWrapper.builder()
                            .device(dc)
                            .build();

                    edits.add(YangPatch.YangEdit.builder()
                            .editId("replace " + i)
                            .operation("replace")
                            .value(deviceWrapper)
                            .target(path)
                            .build());
                    i++;
                }
            }
        }
        if (wrapper.getLspInstances() != null) {
            for (NsoLSP lsp: wrapper.getLspInstances()) {
                String lspKeyArg = '=' + lsp.instanceKey();
                String path = "/tailf-ncs:services/esnet-lsp:lsp" + lspKeyArg;

                YangPatchLspWrapper lspWrapper = YangPatchLspWrapper
                        .builder()
                        .lsp(lsp)
                        .build();

                edits.add(
                        YangPatch
                                .YangEdit
                                .builder()
                                .editId("replace " + lsp.instanceKey())
                                .operation("replace")
                                .value(lspWrapper)
                                .target(path)
                                .build()
                );
            }
        }



        YangPatch patch = YangPatch.builder()
                .patchId("redeploy VPLS for " + connectionId)
                .edit(edits)
                .build();


        return YangPatchWrapper.builder().patch(patch).build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YangPatchDeviceWrapper {
        @JsonProperty("esnet-vpls:device")
        NsoVPLS.DeviceContainer device;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YangPatchLspWrapper {
        @JsonProperty("esnet-lsp:lsp")
        NsoLSP lsp;
    }

    // NSO live status fdb query stuff
    public String getLiveStatusFdbInfo(String device) {
        String args = "service fdb-info";
        if (props.isMockLiveShowCommands()) {
            return "This is mock data for 'show " + args + "'";
        }
        return getLiveStatusShowArgs(device, args);
    }

    public String getLiveStatusAllFdbMacs(String device) {
        String args = "service fdb-mac";
        if (props.isMockLiveShowCommands()) {
            return "This is mock data for 'show " + args + "'";
        }
        return getLiveStatusShowArgs(device, args);
    }

    public String getLiveStatusServiceMacs(String device, int serviceId) {
        String args = "service id " + serviceId + " fdb detail";
        log.info("getLiveStatusServiceMacs " + args);
        if (props.isMockLiveShowCommands()) {
            return "This is mock data for 'show " + args + "'";
        }
        return getLiveStatusShowArgs(device, args);
    }

    // SDP
    public String getLiveStatusServiceSdp(String device, int serviceId) {
        String args = "service id " + serviceId + " sdp";
        if (props.isMockLiveShowCommands()) {
            return LiveStatusMockData.SDP_MOCK_DATA;
        }
        return getLiveStatusShowArgs(device, args);
    }

    // SAP
    public String getLiveStatusServiceSap(String device, int serviceId) {
        String args = "service id " + serviceId + " sap";
        if (props.isMockLiveShowCommands()) {
            return LiveStatusMockData.SAP_MOCK_DATA;
        }
        return getLiveStatusShowArgs(device, args);
    }

    // LSP
    public String getLiveStatusRouterMplsLsp(String device) {
        String args = "router mpls lsp";
        if (props.isMockLiveShowCommands()) {
            return LiveStatusMockData.LSP_MOCK_DATA;
        }
        return getLiveStatusShowArgs(device, args);
    }

    /**
     * Formats live status query arguments and executes and live status query
     *
     * @param device the device for the query
     * @param args   the live status arguments / argument string
     * @return the result as returned by NSO as a string
     */
    public String getLiveStatusShowArgs(String device, String args) {
        if (device == null) {
            log.error("No device provided");
            return null;
        }
        if (args == null) {
            log.error("No args provided");
            return null;
        }
        LiveStatusRequest request = new LiveStatusRequest();
        request.setArgs(args);
        request.setDevice(device);
        return getLiveStatusShow( request);
    }

    /**
     * Executes a live status query via the NSO REST API
     *
     * @param liveStatusRequest the request parameters
     * @return the result as returned by NSO as a string
     */
    public String getLiveStatusShow(LiveStatusRequest liveStatusRequest) {
        if (startupProperties.getStandalone()) {
            log.info("standalone mode - skipping southbound");
            return "standalone live status";
        }

        String path = "restconf/data/esnet-status:esnet-status/nokia-show";
        String restPath = props.getUri() + path;

        StringBuilder errorStr = new StringBuilder();
        errorStr.append("esnet-status error\n");
        final HttpEntity<LiveStatusRequest> requestEntity = new HttpEntity<>(liveStatusRequest);
        // first, try to get a LiveStatusOutput
        ResponseEntity<Object> responseEntity = null;
        try {
            responseEntity = restTemplate.postForEntity(restPath, requestEntity, Object.class);
            if (responseEntity.getStatusCode() != HttpStatus.OK) {
                if (responseEntity.getStatusCode() == HttpStatus.BAD_REQUEST) {
                    throw new RestClientException("Bad request reported by server. Processing error message:\n" + responseEntity.getBody());
                } else {
                    throw new Exception("URL " + restPath + ". Unexpected response code: " + responseEntity.getStatusCode() + ", body:\n" + responseEntity.getBody().toString());
                }
            }
            if (responseEntity.getBody() != null) {
                // Attempt to deserialize as LiveStatusOutput
                // or
                // Attempt to deserialize as IetfRestconfErrorResponse
                try {

                    ObjectMapper mapper = new ObjectMapper();
                    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                    LinkedHashMap<String, String> body = ((LinkedHashMap<String, String>) responseEntity.getBody());

                    String json = mapper.writeValueAsString(body);

                    if (body.containsKey("esnet-status:output")) {
                        LiveStatusOutput liveOutput = mapper.readValue(json, LiveStatusOutput.class);

                        return liveOutput.getOutput();
                    } else {
                        // Cannot figure out what this is.
                        throw new Exception("Unknown body content received. Cannot deserialize as LiveStatusOutput or IetfRestconfErrorResponse:\n" + json);
                    }

                } catch (Exception e) {
                    // Unknown exception
                    log.error("NsoProxy.getLiveStatusShow() - Error while attempting to process response for LiveStatusOutput:\n" + e.getMessage());
                }

            } else {
                errorStr.append("null response body\n");
            }
        } catch (RestClientException re) {
            try {
                if (responseEntity != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                    LinkedHashMap<String, String> body = ((LinkedHashMap<String, String>) responseEntity.getBody());

                    // protect against nulls
                    if (body == null) {
                        body = new LinkedHashMap<>();
                    }

                    String json = mapper.writeValueAsString(body);

                    if (body.containsKey("ietf-restconf:errors")) {
                        IetfRestconfErrorResponse ietfError = mapper.readValue(json, IetfRestconfErrorResponse.class);
                        for (IetfRestconfErrorResponse.IetfError error : ietfError.getErrors().getErrorList()) {
                            errorStr.append(error.getErrorMessage()).append("\n");
                        }
                    } else {
                        throw new Exception("NsoProxy.getLiveStatusShow() - Error while attempting to process response for IetfRestconfErrorResponse:\n" + json);
                    }
                }
            } catch (Exception e) {
                log.error(e.getLocalizedMessage(), e);
            }
        } catch (Exception e) {
            // Not something we can deserialize.
            log.error(e.getLocalizedMessage(), e);
        }

        return errorStr.toString();
    }



    public NsoVplsResponse getVpls() throws Exception {
        FromNsoServiceConfig serviceConfig = getNsoServiceConfig(NsoService.VPLS);
        return new ObjectMapper().readValue(serviceConfig.getConfig(), NsoVplsResponse.class);
    }

    public NsoVplsResponse getVpls(String path) throws Exception {
        FromNsoServiceConfig serviceConfig = getNsoServiceConfig(NsoService.VPLS, path);
        return new ObjectMapper().readValue(serviceConfig.getConfig(), NsoVplsResponse.class);
    }


    public NsoLspResponse getLsps() throws Exception {
        FromNsoServiceConfig serviceConfig = getNsoServiceConfig(NsoService.LSP);
        return new ObjectMapper().readValue(serviceConfig.getConfig(), NsoLspResponse.class);
    }

    public NsoLspResponse getLsps(String path) throws Exception {
        FromNsoServiceConfig serviceConfig = getNsoServiceConfig(NsoService.LSP, path);
        return new ObjectMapper().readValue(serviceConfig.getConfig(), NsoLspResponse.class);
    }

    public String getNsoServiceConfigRestPath(NsoService service) throws Exception {
        String path = switch (service) {
            case VPLS -> "/esnet-vpls:vpls";
            case LSP -> "/esnet-lsp:lsp";
            default -> null;
        };
        if (path == null) {
            throw new Exception("Could not determine service path type. Please use VPLS or LSP.");
        };
        String req = "restconf/data/tailf-ncs:services%s".formatted(path);

        return props.getUri() + req;
    }

    public FromNsoServiceConfig getNsoServiceConfig(NsoService service) throws Exception {
        String restPath = getNsoServiceConfigRestPath(service);
        return getNsoServiceConfig(service, restPath);
    };
    public FromNsoServiceConfig getNsoServiceConfig(NsoService service, String path) throws Exception {
        log.info("get service config START %s ".formatted(service));

        FromNsoServiceConfig result = null;
        try {
            ResponseEntity<String> response;

            response = restTemplate.getForEntity(path, String.class);

//            DevelUtils.dumpDebug("get-nso-service", response);

            if (response.getStatusCode().is2xxSuccessful()) {
                result = new FromNsoServiceConfig();
                String body = response.getBody();
                if (body != null) {
                    result.setConfig(body);
                } else {
                    if (service == NsoService.VPLS) {
                        result.setConfig("{\"esnet-vpls:vpls\": []}");
                    } else if (service == NsoService.LSP) {
                        result.setConfig("{\"esnet-lsp:lsp\": []}");
                    } else {
                        throw new Exception("Unknown service " + service);
                    }
                }
                result.setSuccessful(true);

                log.info("%s: get service COMPLETE ".formatted(service.toString()));
            } else {
                log.error("%s: get config FAILED (response status was not HTTP 200 range.) ".formatted(service.toString()));
                throw new Exception("%s: get config FAILED (response status was not HTTP 200 range.) ".formatted(service.toString()));
            }

        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw e;
        }
        return result;
    }

}
