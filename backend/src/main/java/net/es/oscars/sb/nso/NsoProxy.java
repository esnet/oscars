package net.es.oscars.sb.nso;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.spring.web.v3_1.SpringWebTelemetry;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.StartupProperties;

import net.es.oscars.sb.nso.exc.NsoCommitException;
import net.es.oscars.app.props.NsoProperties;
import net.es.oscars.sb.nso.exc.NsoDryrunException;
import net.es.oscars.sb.nso.rest.*;
import net.es.topo.common.dto.nso.*;

import net.es.topo.common.dto.nso.enums.NsoPlatform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

import net.es.topo.common.dto.nso.enums.NsoService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class NsoProxy {

    public static final String APPLICATION_YANG_DATA_JSON = "application/yang-data+json";
    public static final String APPLICATION_YANG_DATA_XML = "application/yang-data+xml";
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


    private RestClient patchClient;
    private RestClient xmlClient;
    private RestClient restClient;
    private JsonMapper skipEmptyObjectMapper;

    final OpenTelemetry openTelemetry;


    @Autowired
    public NsoProxy(NsoProperties props, StartupProperties startupProperties, OpenTelemetry openTelemetry) {

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

            xmlClient = RestClient.builder()
                    .requestFactory(new HttpComponentsClientHttpRequestFactory())
                    .configureMessageConverters(client -> {
                        client.registerDefaults().withJsonConverter(new JacksonJsonHttpMessageConverter(skipEmptyObjectMapper));
                    })
                    .defaultHeaders(headers -> {
                        headers.add(HttpHeaders.ACCEPT, APPLICATION_YANG_DATA_JSON);
                        headers.add(HttpHeaders.CONTENT_TYPE, APPLICATION_YANG_DATA_XML);
                    })
                    .requestInterceptors(interceptors -> {
                        interceptors.add(new BasicAuthenticationInterceptor(props.getUsername(), props.getPassword()));
                        interceptors.add(telemetry.createInterceptor());
                    })
                    .build();


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
            ResponseEntity<IetfRestconfErrorResponse> response = restClient.post()
                    .uri(restPath)
                    .body(wrapper)
                    .retrieve()
                    .toEntity(IetfRestconfErrorResponse.class);

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
            ResponseEntity<NsoDryRun> dryRunResponse = restClient.post()
                    .uri(restPath)
                    .body(wrapper)
                    .retrieve()
                    .toEntity(NsoDryRun.class);

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
            HttpEntity<NsoDryRun> response = patchClient.patch()
                    .uri(restPath)
                    .body(wrapped)
                    .retrieve()
                    .toEntity(NsoDryRun.class);

            NsoDryRun dryRun = response.getBody();
            if (dryRun != null) {
                NsoProxy.logNsoObject(response);
                if (dryRun.getDryRunResult() != null && dryRun.getDryRunResult().getCli() != null) {
                    if (dryRun.getDryRunResult().getCli().getLocalNode() != null) {
                        return dryRun.getDryRunResult().getCli().getLocalNode().getData();
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
        try {
            HttpEntity<LiveStatusOutput> response = restClient.post()
                    .uri(restPath)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        String resBody = new String(res.getBody().readAllBytes());

                        throw new LiveStatusException(resBody);
                    })
                    .toEntity(LiveStatusOutput.class);
            if (response.getBody() != null) {
                return response.getBody().getOutput();
            }
            errorStr.append("null response from server\n");

        } catch (RestClientException e) {
            log.error("Error while calling esnet-status api", e);
            errorStr.append(e.getMessage());

        } catch (LiveStatusException ex) {
            try {
                JsonMapper jsonMapper = JsonMapper.builder()
                        .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                        .build();

                IetfRestconfErrorResponse ietfError = jsonMapper.readValue(ex.getResponse(), IetfRestconfErrorResponse.class);
                for (IetfRestconfErrorResponse.IetfError error : ietfError.getErrors().getErrorList()) {
                    errorStr.append(error.getErrorMessage()).append("\n");
                }
                log.error(jsonMapper.writeValueAsString(ietfError));
            } catch (JacksonException exc) {
                log.error("error deserializing server error response", exc);
                errorStr.append(exc.getMessage());
            }
        }

        return errorStr.toString();
    }
    public static class LiveStatusException  extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;

        @Getter
        private final String response;
        public LiveStatusException(String response) {
            this.response = response;
        }
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
        try {
            HttpEntity<String> response = restClient.get().uri(restPath).retrieve().toEntity(String.class);
            result.setConfig(response.getBody());
            result.setSuccessful(true);
            log.debug("%s: get service COMPLETE ".formatted(service.toString()));
        } catch (RestClientException ex) {
            log.warn("%s: get service config FAILED ".formatted(service.toString()));
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

            InputStream inputStream = deviceListQueryResource.getInputStream();

            String xmlPayload = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);


            ResponseEntity<FromNsoImmediateQueryResult> response = xmlClient.post()
                            .uri(restPath)
                            .body(xmlPayload)
                            .retrieve()
                            .toEntity(FromNsoImmediateQueryResult.class);

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
