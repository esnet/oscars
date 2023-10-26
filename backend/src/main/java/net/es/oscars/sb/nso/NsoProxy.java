package net.es.oscars.sb.nso;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.sb.nso.exc.NsoCommitException;
import net.es.oscars.app.props.NsoProperties;
import net.es.oscars.sb.nso.exc.NsoDryrunException;
import net.es.oscars.sb.nso.rest.NsoDryRun;
import net.es.oscars.sb.nso.rest.NsoHeaderRequestInterceptor;
import net.es.oscars.sb.nso.rest.NsoResponseErrorHandler;
import net.es.oscars.sb.nso.rest.NsoServicesWrapper;
import net.es.topo.common.devel.DevelUtils;
import net.es.topo.common.dto.nso.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class NsoProxy {

    private final NsoProperties props;
    private RestTemplate restTemplate;
    private RestTemplate patchTemplate;

    @Autowired
    public NsoProxy(NsoProperties props) {

        this.props = props;
        try {
            // make sure we don't send empty values
            ObjectMapper customObjectMapper = new ObjectMapper();
            customObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
            converter.setObjectMapper(customObjectMapper);

            this.restTemplate = new RestTemplate();
            restTemplate.setErrorHandler(new NsoResponseErrorHandler());
            restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(props.getUsername(), props.getPassword()));
            restTemplate.getInterceptors().add(new NsoHeaderRequestInterceptor(HttpHeaders.ACCEPT, "application/yang-data+json"));
            restTemplate.getInterceptors().add(new NsoHeaderRequestInterceptor(HttpHeaders.CONTENT_TYPE, "application/yang-data+json"));
            restTemplate.getMessageConverters().add(0, converter);

            // different http client for yang patch
            this.patchTemplate = new RestTemplate();
            patchTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
            patchTemplate.setErrorHandler(new NsoResponseErrorHandler());
            patchTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(props.getUsername(), props.getPassword()));
            patchTemplate.getInterceptors().add(new NsoHeaderRequestInterceptor(HttpHeaders.ACCEPT, "application/yang-data+json"));
            patchTemplate.getInterceptors().add(new NsoHeaderRequestInterceptor(HttpHeaders.CONTENT_TYPE, "application/yang-patch+json"));

        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        log.info("NSO server base URI: " + props.getUri());
    }

    public void deleteServices(NsoAdapter.NsoOscarsDismantle dismantle) throws NsoCommitException {
        YangPatchWrapper wrapped = makeDismantleYangPatch(dismantle);

        String path = "restconf/data/";
        String restPath = props.getUri() + path;

        final HttpEntity<YangPatchWrapper> entity = new HttpEntity<>(wrapped);
        UUID errorUuid = UUID.randomUUID();
        String errorRef = "Error reference: ["+errorUuid+"]\n";

        try {
            ResponseEntity<String> response = patchTemplate.exchange(restPath, HttpMethod.PATCH, entity, String.class);
            if (response.getStatusCode().isError()) {
                log.error("raw error: "+response.getBody());
                StringBuilder errorStr = new StringBuilder();
                try {
                    YangPatchErrorResponse errorResponse = new ObjectMapper().readValue(response.getBody(), YangPatchErrorResponse.class);
                    for (YangPatchErrorResponse.YangPatchError errObj : errorResponse.getStatus().getErrors().getErrorList()) {
                        errorStr.append(errObj.getErrorMessage()).append("\n");
                    }
                } catch (JsonProcessingException ex) {
                    log.error(errorRef+ex.getMessage()+"\n"+response.getBody());
                    throw new NsoCommitException(errorRef+"Unable to delete. NSO response parse error.");
                }
                log.error(errorRef+"Unable to delete. NSO error(s): " + errorStr);
                throw new NsoCommitException(errorRef+"Unable to delete. NSO error(s): " + errorStr);
            }
        } catch (RestClientException ex) {
            log.error(errorRef+"REST error %s".formatted(ex.getMessage()));
            throw new NsoCommitException(errorRef+" REST Error: %s".formatted(ex.getMessage()));
        }
    }

    public void buildServices(NsoServicesWrapper wrapper) throws NsoCommitException {
        String path = "restconf/data/tailf-ncs:services";
        String restPath = props.getUri() + path;
        UUID errorUuid = UUID.randomUUID();
        String errorRef = "Error reference: [" + errorUuid + "]\n";
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        try {
            DevelUtils.dumpDebug("commit", wrapper);
            ResponseEntity<String> response = restTemplate.postForEntity(restPath, wrapper, String.class);

            if (response.getStatusCode().isError()) {
                log.error("raw error: "+response.getBody());
                StringBuilder errorStr = new StringBuilder();
                try {
                    IetfRestconfErrorResponse errorResponse = new ObjectMapper().readValue(response.getBody(), IetfRestconfErrorResponse.class);
                    for (IetfRestconfErrorResponse.IetfError errObj : errorResponse.getErrors().getErrorList()) {
                        errorStr.append(errObj.getErrorMessage()).append("\n");
                    }
                } catch (JsonProcessingException ex) {
                    log.error(errorRef + "Unable to commit. Unable to parse error. Raw: " + response.getBody()+"\n"+ex.getMessage());
                    ex.printStackTrace(pw);
                    log.error(sw.toString());
                    throw new NsoCommitException("Unable to commit. Unable to parse error. Raw: " + response.getBody());
                }
                log.error(errorRef+"Unable to commit. NSO error(s): " + errorStr);
                throw new NsoCommitException("Unable to commit. NSO error(s): " + errorStr);
            }
        } catch (RestClientException ex) {
            log.error(errorRef+"REST error %s".formatted(ex.getMessage()));
            if (ex instanceof RestClientResponseException){
                log.info("Response body:\n" + ((RestClientResponseException) ex).getResponseBodyAsString());
            }
            ex.printStackTrace(pw);
            log.error(sw.toString());
            throw new NsoCommitException(ex.getMessage());
        }
    }
    public String buildDryRun(NsoServicesWrapper wrapper) throws NsoDryrunException {
        String path = "restconf/data/tailf-ncs:services?dry-run=cli&commit-queue=async";
        String restPath = props.getUri() + path;
        UUID errorUuid = UUID.randomUUID();
        String errorRef = "Error reference: [" + errorUuid + "]\n";

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(restPath, wrapper, String.class);
            if (response.getStatusCode().isError()) {
                log.error("raw error: " + response.getBody());
                throw new NsoDryrunException("unable to perform dry run "+ response.getBody());
            } else {
                try {
                    NsoDryRun dryRun = new ObjectMapper().readValue(response.getBody(), NsoDryRun.class);
                    if (dryRun.getDryRunResult() != null) {
                        return dryRun.getDryRunResult().toString();
                    } else {
                        return "Null dry run";
                    }
                } catch (JsonProcessingException ex) {
                    log.error(errorRef + "Unable to perform dry run. Unable to parse error. Raw: " + response.getBody()+"\n"+ex.getMessage());
                    throw new NsoDryrunException(ex.getMessage());
                }
            }
        } catch (RestClientException ex) {
            log.error(errorRef+"REST error %s".formatted(ex.getMessage()));
            throw new NsoDryrunException(ex.getMessage());
        }
    }
    public String dismantleDryRun(NsoAdapter.NsoOscarsDismantle dismantle) throws NsoDryrunException {
        YangPatchWrapper wrapped = makeDismantleYangPatch(dismantle);

        String path = "restconf/data?dry-run=cli&commit-queue=async";
        String restPath = props.getUri() + path;

        final HttpEntity<YangPatchWrapper> entity = new HttpEntity<>(wrapped);
        UUID errorUuid = UUID.randomUUID();
        String errorRef = "Error reference: ["+errorUuid+"]\n";

        try {
            NsoDryRun response = patchTemplate.patchForObject(restPath, entity, NsoDryRun.class);
            if (response != null && response.getDryRunResult() != null) {
                log.info(response.getDryRunResult().getCli().getLocalNode().getData());
                return response.getDryRunResult().getCli().getLocalNode().getData();
            } else {
                return "no dry-run available";
            }
        } catch (RestClientException ex) {
            log.error(errorRef+"REST error %s".formatted(ex.getMessage()));
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
                .patchId("delete VPLS and LSP for"+dismantle.getConnectionId())
                .edit(edits)
                .build();


        return YangPatchWrapper.builder().patch(deletePatch).build();
    }

}
