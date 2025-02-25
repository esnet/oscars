package net.es.oscars.esdb;


import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.spring.web.v3_1.SpringWebTelemetry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.EsdbProperties;
import net.es.topo.common.dto.esdb.EsdbVlan;
import net.es.topo.common.dto.esdb.EsdbVlanPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
    public ESDBProxy(EsdbProperties props, OpenTelemetry openTelemetry) {
        this.esdbProperties = props;
        this.openTelemetry = openTelemetry;
        SpringWebTelemetry telemetry = SpringWebTelemetry.create(openTelemetry);

        this.restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new HeaderRequestInterceptor("Authorization", "Token "+props.getApiKey()));
        restTemplate.getInterceptors().add(new HeaderRequestInterceptor("Accept", MediaType.APPLICATION_JSON_VALUE));
        restTemplate.getInterceptors().add(new HeaderRequestInterceptor("Content-Type", MediaType.APPLICATION_JSON_VALUE));
        restTemplate.getInterceptors().add(telemetry.newInterceptor());
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

        String result = restTemplate.postForObject(restPath, payload, String.class);
        log.info("create VLAN result:\n" + result);
    }
    public void deleteVlan(Integer vlanPkId) {
        String restPath = esdbProperties.getUri()+"vlan/"+vlanPkId+"/";
        log.info("delete rest path: "+restPath);
        restTemplate.delete(restPath);
    }

    @Data
    static class WrappedEsdbVlans {
        private List<EsdbVlan> results;
    }
}
