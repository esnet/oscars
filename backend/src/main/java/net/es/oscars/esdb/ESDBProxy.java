package net.es.oscars.esdb;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.spring.web.v3_1.SpringWebTelemetry;
import jakarta.validation.constraints.Null;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.EsdbProperties;
import net.es.oscars.app.util.HeaderRequestInterceptor;
import net.es.topo.common.devel.DevelUtils;
import net.es.topo.common.dto.esdb.EsdbEquip;
import net.es.topo.common.dto.esdb.EsdbVlan;
import net.es.topo.common.dto.esdb.EsdbVlanPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.graphql.GraphQlResponse;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.ClientResponseField;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.*;

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

    /**
     * Get all ESDB VLANS from ESDB using the /vlan endpoint.
     * Deprecated. Please use ESDBProxy.gqlVlanList() instead.
     * @deprecated
     * @return List of EsdbVlan objects from the REST endpoint response.
     */
    public List<EsdbVlan> getAllEsdbVlans() throws Exception {
//        String vlanUrl = esdbProperties.getUri()+"vlan/?limit=0";
//
//        WrappedEsdbVlans wrapped = restTemplate.getForObject(vlanUrl, WrappedEsdbVlans.class);
//        if (wrapped == null) {
//            return new ArrayList<>();
//        }
//        return wrapped.getResults();
        throw new Exception("Deprecated method.");
    }

    /**
     * Get all ESDB VLANs from ESDB using GraphQL. Targets vlanList.
     * @return Returns a list of EsdbVlan objects from the GraphQL response.
     */
    public List<EsdbVlan> gqlVlanList() {
        return this.gqlVlanList("", null, null, null, null);
    }
    /**
     * Get all ESDB VLANs from ESDB using GraphQL. Targets vlanList.
     * @param searchQuery The searchQuery (string) parameter for vlanList.
     * @return Returns a list of EsdbVlan objects from the GraphQL response.
     */
    public List<EsdbVlan> gqlVlanList(String searchQuery) {
        return this.gqlVlanList(searchQuery, null, null, null, null);
    }
    /**
     * Get all ESDB VLANs from ESDB using GraphQL. Targets vlanList.
     * @param searchQuery The searchQuery (string) parameter for vlanList.
     * @param sortProperty The sortProperty (string) parameter for vlanList.
     * @return Returns a list of EsdbVlan objects from the GraphQL response.
     */
    public List<EsdbVlan> gqlVlanList(String searchQuery, String sortProperty) {
        return this.gqlVlanList(searchQuery, sortProperty, null, null, null);
    }
    /**
     * Get all ESDB VLANs from ESDB using GraphQL. Targets vlanList.
     * @param searchQuery The searchQuery (string) parameter for vlanList.
     * @param sortProperty The sortProperty (string) parameter for vlanList.
     * @param first The first (int) parameter for vlanList.
     * @return Returns a list of EsdbVlan objects from the GraphQL response.
     */
    public List<EsdbVlan> gqlVlanList(String searchQuery, String sortProperty, Integer first) {
        return this.gqlVlanList(searchQuery, sortProperty, first, null, null);
    }
    /**
     * Get all ESDB VLANs from ESDB using GraphQL. Targets vlanList.
     * @param searchQuery The searchQuery (string) parameter for vlanList.
     * @param sortProperty The sortProperty (string) parameter for vlanList.
     * @param first The first (int) parameter for vlanList.
     * @param skip The skip (int) parameter for vlanList.
     * @return Returns a list of EsdbVlan objects from the GraphQL response.
     */
    public List<EsdbVlan> gqlVlanList(String searchQuery, String sortProperty, Integer first, Integer skip) {
        return this.gqlVlanList(searchQuery, sortProperty, first, skip, null);
    }

    /**
     * Get all ESDB VLANs from ESDB using GraphQL. Targets vlanList.
     * @param search The search (string) parameter for vlanList.
     * @param sortProperty The sortProperty (string) parameter for vlanList.
     * @param first The first (int) parameter for vlanList.
     * @param skip The skip (int) parameter for vlanList.
     * @param uuids The uuids (List of UUID strings) parameter for vlanList.
     * @return Returns a list of EsdbVlan objects from the GraphQL response.
     */
    public List<EsdbVlan> gqlVlanList(
        @Null String search,
        @Null String sortProperty,
        @Null Integer first,
        @Null Integer skip,
        @Null List<String> uuids
    ) {
        List<EsdbVlan> results;

        Map<String, Object> params = new HashMap<>();

        if (search != null && !search.isEmpty()) {
            params.put("search", search);
        }
        if (sortProperty != null && !sortProperty.isEmpty()) {
            params.put("sortProperty", sortProperty);
        }
        if (first != null) {
            params.put("first", first);
        }
        if (skip != null) {
            params.put("skip", skip);
        }
        if (uuids != null && !uuids.isEmpty()) {
            params.put("uuids", uuids);
        }

        HttpSyncGraphQlClient graphQlClient = createGraphqlClient();

        // Should return a List<EsdbVlan> in the "list" property
        // Example response payload:
        // {
        //  "data": {
        //    "vlanList": {
        //      "count": 1646,
        //      "results": [
        //        {
        //          "id": "10943",
        //          "uuid": "",
        //          "vlanId": 2188,
        //          "description": "OSCARS DFYG (ORNL AzureGov Sec)",
        //          "equipment": {
        //            "id": "1717"
        //          },
        //          "equipmentInterface": {
        //            "id": "14117"
        //          }
        //        },
        //        ...,
        //        {
        //          "id": "2104",
        //          "uuid": "",
        //          "vlanId": 4073,
        //          "description": null,
        //          "equipment": {
        //            "id": "2568"
        //          },
        //          "equipmentInterface": {
        //            "id": "27686"
        //          }
        //        }
        //      ]
        //    }
        //  }
        // }

        // See oscars/backend/src/main/resources/graphql-documents/vlanList.graphql
        // Our GraphQL client can autoload by document name from the graphql-documents/ directory.
        GraphQlClient.RequestSpec requestSpec = graphQlClient
            .documentName("vlanList");

        if (!params.isEmpty()) {
            requestSpec.variables(params);
        }
        ClientGraphQlResponse response = requestSpec.executeSync();

        List<GraphqlEsdbVlan> vlanList = response
            .field("vlanList.results")
            .toEntityList(GraphqlEsdbVlan.class);

        results = new ArrayList<>(vlanList.size());
        if (!vlanList.isEmpty()) {
            log.info("ESDBProxy.gqlVlanList() called. List of VLANs has a size of {}", vlanList.size());
            for (GraphqlEsdbVlan vlan : vlanList) {
                EsdbVlan eV = EsdbVlan.builder()
                    .id(vlan.getId())
                    .vlanId(vlan.getVlanId())
                    .description(vlan.getDescription())
                    .equipment(vlan.getEquipment())
                    .equipmentInterface(vlan.getEquipmentInterface())
                    .build();
                results.add(eV);
            }
        } else {
            log.warn("ESDBProxy.gqlVlanList() called but no ESDBVlans found in ESDB GraphQL response. Returning empty list.");
        }

        return results;
    }

    public HttpSyncGraphQlClient createGraphqlClient() {
        log.debug("creating GraphQlClient with RestClient URL {}", esdbProperties.getGraphqlUri());
        RestClient restClient = RestClient.create(
            esdbProperties.getGraphqlUri()
        );

        return HttpSyncGraphQlClient.builder(restClient)
            .header("Authorization", "Token " + esdbProperties.getApiKey())
            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public void createVlan(EsdbVlanPayload payload) {
        String restPath = esdbProperties.getUri()+"vlan/";
        log.info("create rest path: "+restPath);
        EsdbVlan result = restTemplate.postForObject(restPath, payload, EsdbVlan.class);
        if (result != null) {
            log.info("create ESDB VLAN:\n" + result.getUrl());
        }
    }
    public void deleteVlan(Integer vlanPkId) {
        String restPath = esdbProperties.getUri()+"vlan/"+vlanPkId+"/";
        log.info("delete rest path: "+restPath);
        restTemplate.delete(restPath);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class GraphqlEsdbVlan {
        private String id;
        private String url;
        private String uuid;
        private Integer vlanId;
        private String description;
        @JsonProperty("bridgeId")
        private String bridge_id;
        private GraphqlEsdbEquipment equipment = new GraphqlEsdbEquipment();
        private GraphqlEsdbEquipmentInterface equipmentInterface = new GraphqlEsdbEquipmentInterface();

        public Integer getId() {
            return Integer.parseInt(id);
        }

        public Integer getEquipment() {
            return equipment.getId();
        }

        public Integer getEquipmentInterface() {
            return equipmentInterface.getId();
        }
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class GraphqlEsdbEquipment {
        private String id;
        public Integer getId() {
            return Integer.parseInt(id);
        }
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class GraphqlEsdbEquipmentInterface {
        private String id;
        public Integer getId() {
            return Integer.parseInt(id);
        }
    }
}
