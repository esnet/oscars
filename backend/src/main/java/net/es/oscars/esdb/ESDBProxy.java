package net.es.oscars.esdb;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.spring.web.v3_1.SpringWebTelemetry;
import jakarta.validation.constraints.Null;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.EsdbProperties;
import net.es.oscars.app.util.HeaderRequestInterceptor;
import net.es.topo.common.dto.esdb.EsdbVlan;
import net.es.topo.common.dto.esdb.EsdbVlanPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    public List<EsdbVlan> getAllEsdbVlans() {
        String vlanUrl = esdbProperties.getUri()+"vlan/?limit=0";

        WrappedEsdbVlans wrapped = restTemplate.getForObject(vlanUrl, WrappedEsdbVlans.class);
        if (wrapped == null) {
            return new ArrayList<>();
        }
        return wrapped.results;
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
     * @param searchQuery The searchQuery (string) parameter for vlanList.
     * @param sortProperty The sortProperty (string) parameter for vlanList.
     * @param first The first (int) parameter for vlanList.
     * @param skip The skip (int) parameter for vlanList.
     * @param uuids The uuids (List of UUID strings) parameter for vlanList.
     * @return Returns a list of EsdbVlan objects from the GraphQL response.
     */
    public List<EsdbVlan> gqlVlanList(
        @Null String searchQuery,
        @Null String sortProperty,
        @Null Integer first,
        @Null Integer skip,
        @Null List<String> uuids
    ) {
        List<EsdbVlan> results = new ArrayList<>();

        String gqlVlanListQuery = "vlanList";
        List<String> params = new ArrayList<>();

        if (searchQuery != null && !searchQuery.isEmpty()) {
            params.add("search: \"%s\"".formatted(searchQuery));
        }
        if (sortProperty != null && !sortProperty.isEmpty()) {
            params.add("sortProperty: \"%s\"".formatted(sortProperty));
        }
        if (first != null) {
            params.add("first: %d".formatted(first));
        }
        if (skip != null) {
            params.add("skip: %d".formatted(skip));
        }
        if (uuids != null && !uuids.isEmpty()) {
            params.add("uuids: [\"%s\"]".formatted( String.join("\",\"", uuids ) ));
        }

        if (!params.isEmpty()) {
            gqlVlanListQuery += "(" + String.join(",", params) + ")";
        }
        // GraphQL request document
        String graphqlDocument =
        """
        {
            %s {
                count: totalRecords
                list {
                    id,
                    url,
                    vlan_id,
                    description,
                    bridgeId,
                    equipment,
                    equipment_interface
                }
            }
        }
        """.formatted(
            gqlVlanListQuery
        );

        RestClient restClient = RestClient.create(
            esdbProperties.getGraphQlUri() + "vlan/"
        );

        HttpSyncGraphQlClient graphQlClient = HttpSyncGraphQlClient.builder(restClient)
            .header("Authorization", "Token " + esdbProperties.getApiKey())
            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();

        // Should return a List<EsdbVlan> in the "list" property
        // Example response payload:
        // {
        //  "data": {
        //    "vlanList": {
        //      "count": 1646,
        //      "list": [
        //        {
        //          "id": "10943",
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
        //          "vlanId": 4073,
        //          "description": null,
        //          "equipment": {
        //            "id": "2568"
        //          },
        //          "equipmentInterface": {
        //            "id": "27686"
        //          }
        //        }
        //    }
        //  }
        // }
        ClientGraphQlResponse response = graphQlClient
            .document(graphqlDocument)
            .executeSync();

        WrappedEsdbVlans wrappedEsdbVlans = response.field("vlanList").toEntity(WrappedEsdbVlans.class);
        if (wrappedEsdbVlans != null) {
            log.info("ESDBProxy.gqlVlanList() called. Total records count: {}", wrappedEsdbVlans.count);
            results = wrappedEsdbVlans.results;
        } else {
            log.warn("ESDBProxy.gqlVlanList() called but no ESDBVlans found in ESDB GraphQL response. Returning empty list.");
            results = new ArrayList<>();
        }


        return results;
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

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class WrappedEsdbVlans {
        int count;
        private List<EsdbVlan> results;
    }
}
