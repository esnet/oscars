package net.es.oscars.sb.nso;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import net.es.topo.common.dto.nso.*;

import net.es.topo.common.dto.nso.enums.NsoCheckSyncState;
import net.es.topo.common.dto.nso.enums.NsoPlatform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import net.es.topo.common.dto.nso.enums.NsoService;
import net.es.oscars.sb.nso.dto.NsoLspResponse;
import net.es.oscars.sb.nso.dto.NsoVplsResponse;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;

@Slf4j
@Component
public class NsoProxy {

    public static final String APPLICATION_YANG_DATA_JSON = "application/yang-data+json";
    public static final String APPLICATION_YANG_PATCH_JSON = "application/yang-patch+json";
    public static final String RESTCONF_DATA = "restconf/data";

    private final NsoProperties props;
    private final StartupProperties startupProperties;

    @Value("classpath:device-list-query.xml")
    private Resource deviceListQueryResource;

    @Getter
    @Setter
    static NsoResponseErrorHandler restErrorHandler = new NsoResponseErrorHandler();

    @Getter
    @Setter
    static NsoResponseErrorHandler patchErrorHandler = new NsoResponseErrorHandler();

    @Setter
    private RestTemplate restTemplate;
    private RestTemplate xmlRestTemplate;

    private RestClient patchClient;
    private RestClient restClient;
    private JsonMapper skipEmptyObjectMapper;

    final OpenTelemetry openTelemetry;


    @Autowired
    public NsoProxy(NsoProperties props, StartupProperties startupProperties, RestTemplateBuilder builder, OpenTelemetry openTelemetry) {

        this.props = props;
        this.startupProperties = startupProperties;
        this.openTelemetry = openTelemetry;
        try {
            // this object mapper makes sure we don't send any empty / null values
            skipEmptyObjectMapper = JsonMapper.builder()
                    .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_EMPTY))
                    .build();


            SpringWebTelemetry telemetry = SpringWebTelemetry.create(openTelemetry);

            patchClient = RestClient.builder()
                    .requestFactory(new HttpComponentsClientHttpRequestFactory())

                    .configureMessageConverters(client -> {
                        client.registerDefaults().withJsonConverter(new JacksonJsonHttpMessageConverter(skipEmptyObjectMapper));
                    })
                    .defaultHeaders(headers -> {
                        headers.add(HttpHeaders.ACCEPT, APPLICATION_YANG_DATA_JSON);
                        headers.add(HttpHeaders.CONTENT_TYPE, APPLICATION_YANG_PATCH_JSON);
                    })
                    .requestInterceptors(interceptors -> {
                        interceptors.add(new BasicAuthenticationInterceptor(props.getUsername(), props.getPassword()));
                        interceptors.add(telemetry.createInterceptor());
                    })
                    .build();

            restClient = RestClient.builder()
                    .requestFactory(new HttpComponentsClientHttpRequestFactory())
                    .configureMessageConverters(client -> {
                        client.registerDefaults().withJsonConverter(new JacksonJsonHttpMessageConverter(skipEmptyObjectMapper));
                    })
                    .defaultHeaders(headers -> {
                        headers.add(HttpHeaders.ACCEPT, APPLICATION_YANG_DATA_JSON);
                        headers.add(HttpHeaders.CONTENT_TYPE, APPLICATION_YANG_DATA_JSON);
                    })
                    .requestInterceptors(interceptors -> {
                        interceptors.add(new BasicAuthenticationInterceptor(props.getUsername(), props.getPassword()));
                        interceptors.add(telemetry.createInterceptor());
                    })
                    .build();

            this.xmlRestTemplate = builder.requestFactory(HttpComponentsClientHttpRequestFactory.class)
                    .connectTimeout(Duration.ofSeconds(5))
                    .readTimeout(Duration.ofSeconds(300))
                    .build();
            xmlRestTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(props.getUsername(), props.getPassword()));
            xmlRestTemplate.getInterceptors().add(new HeaderRequestInterceptor("Accept", "application/yang-data+json"));
            xmlRestTemplate.getInterceptors().add(new HeaderRequestInterceptor("Content-type", "application/yang-data+xml"));
            xmlRestTemplate.getInterceptors().add(telemetry.createInterceptor());

            this.restTemplate = builder
                    .messageConverters(new JacksonJsonHttpMessageConverter(skipEmptyObjectMapper))
                    .build();
            restTemplate.setErrorHandler(restErrorHandler);
            restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(props.getUsername(), props.getPassword()));
            restTemplate.getInterceptors().add(new HeaderRequestInterceptor(HttpHeaders.ACCEPT, APPLICATION_YANG_DATA_JSON));
            restTemplate.getInterceptors().add(new HeaderRequestInterceptor(HttpHeaders.CONTENT_TYPE, APPLICATION_YANG_DATA_JSON));
            restTemplate.getInterceptors().add(telemetry.createInterceptor());

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

        log.info("submitting yang patch");
        logNsoObject(wrapped);

        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("rollback-label", rollbackLabel);
        String params = "";
        try {
            params = encodedParams(paramMap);
        } catch (JsonProcessingException e) {
            throw new NsoCommitException("unable to encode params");
        }


        String restPath = props.getUri() + RESTCONF_DATA + params;

        UUID errorUuid = UUID.randomUUID();
        String errorRef = "Error reference: [" + errorUuid + "]\n";

        try {
            log.info("submitting yang patch to " + restPath);
            ResponseEntity<String> response = patchClient.patch()
                    .uri(restPath)
                    .body(wrapped)
                    .retrieve()
                    .toEntity(String.class);

            if (response.getStatusCode().isError()) {
                log.error("raw error: " + response.getBody() + "\n" + response.getHeaders());
                StringBuilder errorStr = new StringBuilder();

                JsonMapper mapper = new JsonMapper();
                YangPatchErrorResponse errorResponse = mapper.readValue(response.getBody(), YangPatchErrorResponse.class);
                for (YangPatchErrorResponse.YangPatchError errObj : errorResponse.getStatus().getErrors().getErrorList()) {
                    errorStr.append(errObj.getErrorMessage()).append("\n");
                }

                log.error(errorRef + "Unable to YANG patch. NSO error(s): " + errorStr);
                throw new NsoCommitException(errorRef + "Unable to YANG patch. NSO error(s): " + errorStr);
            }
        } catch (RestClientException ex) {
            log.error(errorRef + "YANG PATCH error %s".formatted(ex.getMessage()));
            throw new NsoCommitException(errorRef + " yang patch REST Error: %s".formatted(ex.getMessage()));
        }
    }

    public NsoCheckSyncState checkSync(String device) {
        if (startupProperties.getStandalone()) {
            log.info("standalone mode - returning in-sync for {}", device);
            return NsoCheckSyncState.IN_SYNC;
        }
        String path = RESTCONF_DATA + "/tailf-ncs:devices/device=" + device + "/check-sync";
        String restPath = props.getUri() + path;
        log.info("checking sync " + restPath);
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

        String rollbackLabel = connectionId + "-build";
        String path = "restconf/data/tailf-ncs:services";
        String restPath = props.getUri() + path + "?rollback-label=" + rollbackLabel;
        UUID errorUuid = UUID.randomUUID();
        String errorRef = "Error reference: [" + errorUuid + "]\n";
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        log.info("building services");
        logNsoObject(wrapper);

        try {
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

    public String buildDryRun(NsoServicesWrapper wrapper, String connectionId) throws NsoDryrunException {
        if (startupProperties.getStandalone()) {
            log.info("standalone mode - skipping southbound");
            return "standalone dry run";
        }
        log.info("BUILD dry run for "+connectionId);


        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("dry-run", "cli");
        paramMap.put("commit-queue", "async");
        String params = "";
        try {
            params = encodedParams(paramMap);
        } catch (JsonProcessingException e) {
            throw new NsoDryrunException("unable to encode params");
        }

        String path = RESTCONF_DATA + "/tailf-ncs:services"+params;
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
            throw new NsoDryrunException(ex.getMessage() + " " + errorRef);
        }
    }

    public String dismantleDryRun(NsoAdapter.NsoOscarsDismantle dismantle) throws NsoDryrunException {
        log.info("DISMANTLE dry run for "+dismantle.getConnectionId());
        if (startupProperties.getStandalone()) {
            log.info("standalone mode - skipping southbound");
            return "standalone dry run";
        }

        YangPatchWrapper wrapped = makeDismantleYangPatch(dismantle);
        return this.yangPatchDryRun(wrapped);
    }


    public String redeployDryRun(NsoServicesWrapper wrapper, String connectionId) throws NsoDryrunException {
        log.info("REDEPLOY dry run for "+connectionId);
        if (startupProperties.getStandalone()) {
            log.info("standalone mode - skipping southbound");
            return "standalone dry run";
        }
        YangPatchWrapper wrapped = makeRedeployYangPatch(wrapper, connectionId);
        return this.yangPatchDryRun(wrapped);
    }

    public String yangPatchDryRun(YangPatchWrapper wrapped) throws NsoDryrunException {
        log.info("submitting yang patch dry run");

        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("dry-run", "cli");
        paramMap.put("commit-queue", "async");
        String params = "";
        try {
            params = encodedParams(paramMap);
        } catch (JsonProcessingException e) {
            throw new NsoDryrunException("unable to encode params");
        }

        String path = RESTCONF_DATA + params;
        String restPath = props.getUri() + path;


        try {
            log.info("submitting yang patch to " + restPath);
            logNsoObject(wrapped);
            NsoDryRun response = patchClient.patch()
                    .uri(restPath)
                    .body(wrapped)
                    .retrieve()
                    .body(NsoDryRun.class);

            if (response != null && response.getDryRunResult() != null) {
                NsoProxy.logNsoObject(response);
                if (response.getDryRunResult() != null && response.getDryRunResult().getCli() != null) {
                    if (response.getDryRunResult().getCli().getLocalNode() != null) {
                        return response.getDryRunResult().getCli().getLocalNode().getData();
                    } else {
                        /*
                        an empty dry run looks like this:
                        {
                          "dry-run-result" : {
                            "cli" : { }
                          }
                        }
                         */
                        return "";
                    }
                }
                // if either "dry-run-result" or "dry-run-result/cli" are null, complain

                return "error retrieving dry run text";
            } else {
                return "no dry-run available";
            }
        } catch (RestClientException ex) {
            log.error("YANG PATCH dry run REST error:\n%s".formatted(ex.getMessage()));
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

    public static YangPatchWrapper makeRedeployLspYangPatch(NsoLSP lsp) {
        List<YangPatch.YangEdit> edits = new ArrayList<>();

        String lspKeyArg = '=' + lsp.instanceKey();
        String path = "/tailf-ncs:services/esnet-lsp:lsp" + lspKeyArg;

        List<NsoLSP> lsps = new ArrayList<>();
        lsps.add(lsp);
        YangPatchLspWrapper lspWrapper = YangPatchLspWrapper
                .builder()
                .lsp(lsps)
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
            int i = 0;

            // replace the entire remote VPLS instance
            for (NsoVPLS vpls : wrapper.getVplsInstances()) {

                int vcid = vpls.getVcId();
                List<NsoVPLS> vplses = new ArrayList<>();
                vplses.add(vpls);
                YangPatchVplsWrapper vplsWrapper = YangPatchVplsWrapper.builder()
                        .vpls(vplses)
                        .build();
                String vplsKey = "=" + vcid;
                String path = "/tailf-ncs:services/esnet-vpls:vpls" + vplsKey;

                edits.add(YangPatch.YangEdit.builder()
                        .editId("replace " + i)
                        .operation("replace")
                        .value(vplsWrapper)
                        .target(path)
                        .build());

                i++;
            }
        }
        if (wrapper.getLspInstances() != null) {
            for (NsoLSP lsp : wrapper.getLspInstances()) {
                String lspKeyArg = '=' + lsp.instanceKey();
                String path = "/tailf-ncs:services/esnet-lsp:lsp" + lspKeyArg;
                List<NsoLSP> lsps = new ArrayList<>();
                lsps.add(lsp);

                YangPatchLspWrapper lspWrapper = YangPatchLspWrapper
                        .builder()
                        .lsp(lsps)
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
    public static class YangPatchVplsWrapper {
        @JsonProperty("esnet-vpls:vpls")
        List<NsoVPLS> vpls;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YangPatchLspWrapper {
        @JsonProperty("esnet-lsp:lsp")
        List<NsoLSP> lsp;
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

        if (startupProperties.getStandalone()) {
            log.info("standalone mode - skipping southbound");
            return "standalone live status";
        }
        log.info(device+ " "+args);

        LiveStatusRequest request = new LiveStatusRequest();
        request.setArgs(args);
        request.setDevice(device);

        String path = RESTCONF_DATA + "/esnet-status:esnet-status/nokia-show";
        String restPath = props.getUri() + path;

        StringBuilder errorStr = new StringBuilder();
        errorStr.append("esnet-status error\n");

        final HttpEntity<LiveStatusRequest> requestEntity = new HttpEntity<>(request);

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
                    JsonMapper mapper = JsonMapper.builder()

                            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                            .build();

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
                    JsonMapper jsonMapper = JsonMapper.builder()
                            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                            .build();

                    LinkedHashMap<String, String> body = ((LinkedHashMap<String, String>) responseEntity.getBody());

                    // protect against nulls
                    if (body == null) {
                        body = new LinkedHashMap<>();
                    }

                    String json = jsonMapper.writeValueAsString(body);

                    if (body.containsKey("ietf-restconf:errors")) {
                        IetfRestconfErrorResponse ietfError = jsonMapper.readValue(json, IetfRestconfErrorResponse.class);
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

    public FromNsoServiceConfig getNsoServiceConfig(NsoService service) {
        log.info("get service config START %s ".formatted(service));

        String path = switch (service) {
            case BBL -> "/esnet-bbl:bbl";
            case BRIDGE -> "/esnet-bridge:bridge";
            case PORT -> "/esnet-port:port";
            case HOST -> "/esnet-host:host";
            case SYSTEM -> "/esnet-system:system";
            case L3_INTERFACE -> "/esnet-layer3:l3-interface";
            case L3_CUSTOMER -> "/esnet-layer3:l3-customer";
            case LSP -> "/esnet-lsp:lsp";
            case VPLS ->   "/esnet-vpls:vpls";
            // this does not fetch any of the prefix-lists cos that's mega slow and typically unnecessary
            // we can add that later tho
            // with prefix-lists:    310 sec 97Mb
            // without prefix-lists: 0.6 sec 400Kb
            case L3_PEER -> "?fields=esnet-layer3:l3-peer(asn;long-name;short-name;routing-domain(name;route-table(name;peer-type;device;v4(prefix-limit;filter-prefixes);v6(prefix-limit;filter-prefixes))))";
            case L3_TRANSIT -> "/esnet-layer3:l3-transit";
        };

        FromNsoServiceConfig result = FromNsoServiceConfig.builder()
                .service(service)
                .successful(false)
                .build();
        String req = RESTCONF_DATA+"tailf-ncs:services%s".formatted(path);

        String restPath = props.getUri() + req;
        String response = restTemplate.getForObject(restPath, String.class);
        // DevelUtils.dumpDebug("get-nso-service", response);

        if (response != null) {
            result.setConfig(response);
            result.setSuccessful(true);
            log.debug("%s: get service COMPLETE ".formatted(service.toString()));
        } else {
            log.warn("%s: get config FAILED ".formatted(service.toString()));
        }
        return result;
    }

    // this logs the object using the custom object mapper that the restClient / restTemplate use
    public static void logNsoObject(Object o) {
        JsonMapper jsonMapper = JsonMapper.builder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_EMPTY))
                .build();

        String pretty = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        log.info(pretty);
    }

    private String encodedParams(Map<String, String> params) throws JsonProcessingException {
        if (params == null || params.isEmpty()) {
            return "";
        }
        if (this.props.isEncodedRestconfParams()) {
            String asJson = skipEmptyObjectMapper.writeValueAsString(params);
            String asBase64 = Base64.getEncoder().encodeToString(asJson.getBytes());
            return "?params="+asBase64;

        } else {
            boolean didFirst = false;
            StringBuilder result = new StringBuilder();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!didFirst) {
                    didFirst = true;
                    result = new StringBuilder(String.format("?%s=%s", entry.getKey(), entry.getValue()));
                } else {
                    result.append(String.format("&%s=%s", entry.getKey(), entry.getValue()));
                }
            }
            return result.toString();

        }
    }

    public FromNsoDeviceList getNsoDeviceList() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        String restPath = props.getUri() + "restconf/tailf/query";

        try {
            File file = deviceListQueryResource.getFile();
            String xmlPayload = new String(Files.readAllBytes(file.toPath()));


            ResponseEntity<FromNsoImmediateQueryResult> response = xmlRestTemplate.postForEntity(restPath, xmlPayload, FromNsoImmediateQueryResult.class);
            FromNsoDeviceList fdl = FromNsoDeviceList.mapQueryToDeviceList(response.getBody());
            FromNsoDeviceList result = FromNsoDeviceList.builder()
                    .devices(new ArrayList<>())
                    .build();
            for (FromNsoDevice device: fdl.getDevices()) {
                // workaround for ocd-stack returning "" for platform name
                if (device.getPlatform().getName().equals(NsoPlatform.UNKNOWN) && device.getNed().getNedId().startsWith("alu-sr")) {
                    device.getPlatform().setName(NsoPlatform.NOKIA);
                }
                result.setSuccessful(fdl.getSuccessful());
                result.getDevices().add(device);
            }
            // DevelUtils.dumpDebug("nso-device-list", fdl);
            return result;
        } catch (IOException ex) {
            log.error("Unable to load query file: "+ex.getMessage());
            ex.printStackTrace(pw);
            log.error(sw.toString());

            return FromNsoDeviceList.builder()
                    .devices(new ArrayList<>())
                    .successful(false)
                    .build();
        } catch (HttpClientErrorException ex) {
            log.error("Unable to get device list: "+ex.getMessage());
            ex.printStackTrace(pw);
            log.error(sw.toString());

            return FromNsoDeviceList.builder()
                    .devices(new ArrayList<>())
                    .successful(false)
                    .build();
        }

    }
}
