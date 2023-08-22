package net.es.oscars.nso.rest;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.NsoException;
import net.es.oscars.app.props.NsoProperties;
import net.es.topo.common.devel.DevelUtils;
import net.es.topo.common.dto.nso.*;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
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
        log.info("NSO server URL: " + props.getUri());
    }

    public void deleteServices(String connectionId, NsoAdapter.NsoOscarsDismantle dismantle) throws NsoException {

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
                .patchId("delete VPLS and LSP for"+connectionId)
                .edit(edits)
                .build();

        String path = "/restconf/data/";
        String restPath = props.getUri() + path;

        YangPatchWrapper wrapped = YangPatchWrapper.builder().patch(deletePatch).build();
        DevelUtils.dumpDebug("yang patch delete", wrapped);

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
                    throw new NsoException(errorRef+"Unable to delete. NSO response parse error.");
                }
                log.error(errorRef+"Unable to delete. NSO error(s): " + errorStr);
                throw new NsoException(errorRef+"Unable to delete. NSO error(s): " + errorStr);
            }
        } catch (RestClientException ex) {
            log.error(errorRef+"REST error %s".formatted(ex.getMessage()));
            throw new NsoException(errorRef+" REST Error: %s".formatted(ex.getMessage()));
        }
    }

    public void postToServices(Object wrapper) throws NsoException {
        String path = "/restconf/data/tailf-ncs:services";
        String restPath = props.getUri() + path;
        UUID errorUuid = UUID.randomUUID();
        String errorRef = "Error reference: [" + errorUuid + "]\n";

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
                    throw new NsoException("Unable to commit. Unable to parse error. Raw: " + response.getBody());
                }
                log.error(errorRef+"Unable to commit. NSO error(s): " + errorStr);
                throw new NsoException("Unable to commit. NSO error(s): " + errorStr);
            }
        } catch (RestClientException ex) {
            log.error(errorRef+"REST error %s".formatted(ex.getMessage()));
            throw new NsoException(ex.getMessage());
        }
    }

}
