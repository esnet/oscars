package net.es.oscars.esdb;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.EsdbProperties;
import net.es.topo.common.dto.esdb.EsdbVlan;
import net.es.topo.common.dto.esdb.EsdbVlanPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ESDBProxy {

    private final RestTemplate restTemplate;
    private final EsdbProperties esdbProperties;

    @Autowired
    public ESDBProxy(EsdbProperties props) {
        this.esdbProperties = props;
        this.restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new HeaderRequestInterceptor("Authorization", "Token "+props.getApiKey()));
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
