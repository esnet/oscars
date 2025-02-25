package net.es.oscars.esdb;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.spring.web.v3_1.SpringWebTelemetry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.EsdbProperties;
import net.es.oscars.app.util.HeaderRequestInterceptor;
import net.es.topo.common.devel.DevelUtils;
import net.es.topo.common.dto.esdb.EsdbVlan;
import net.es.topo.common.dto.esdb.EsdbVlanPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ESDBProxy {

    private final RestTemplate restTemplate;
    private final EsdbProperties esdbProperties;
    final OpenTelemetry openTelemetry;

    @Autowired
    public ESDBProxy(EsdbProperties props, OpenTelemetry openTelemetry, RestTemplateBuilder builder) {
        this.esdbProperties = props;
        this.openTelemetry = openTelemetry;
        SpringWebTelemetry telemetry = SpringWebTelemetry.create(openTelemetry);

        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(mapper);

        this.restTemplate = builder
                .additionalInterceptors(
                        new HeaderRequestInterceptor("Authorization", "Token "+props.getApiKey()),
                        new HeaderRequestInterceptor("Accept", MediaType.APPLICATION_JSON_VALUE),
                        new HeaderRequestInterceptor("Content-Type", MediaType.APPLICATION_JSON_VALUE),
                        telemetry.newInterceptor()
                )
                .messageConverters(converter)
                .build();

    }

    public List<EsdbVlan> getAllEsdbVlans() {
        String vlanUrl = esdbProperties.getUri()+"vlan/?limit=0";

        WrappedEsdbVlans wrapped = restTemplate.getForObject(vlanUrl, WrappedEsdbVlans.class);
        if (wrapped == null) {
            return new ArrayList<>();
        }
        return wrapped.results;
    }

    public void createVlan(EsdbVlanPayload payload) {
        String restPath = esdbProperties.getUri()+"vlan/";
        log.info("create rest path: "+restPath);
        DevelUtils.dumpDebug("payload", payload);

        String result = restTemplate.postForObject(restPath, payload, String.class);
        log.info("create VLAN result:\n" + result);
    }
    public void deleteVlan(Integer vlanPkId) {
        String restPath = esdbProperties.getUri()+"vlan/"+vlanPkId+"/";
        log.info("delete rest path: "+restPath);
        restTemplate.delete(restPath);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class WrappedEsdbVlans {
        int count;
        private List<EsdbVlan> results;
    }
}
