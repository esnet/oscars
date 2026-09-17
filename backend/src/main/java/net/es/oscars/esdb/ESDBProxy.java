package net.es.oscars.esdb;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.spring.web.v3_1.SpringWebTelemetry;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.EsdbProperties;
import net.es.oscars.dto.esdb.gql.GraphqlEsdbBandwidthUtilization;
import net.es.oscars.dto.esdb.gql.GraphqlEsdbOrganization;
import net.es.oscars.dto.esdb.gql.GraphqlEsdbOrganizationType;
import net.es.topo.common.dto.esdb.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;

import static tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Slf4j
@Component
public class ESDBProxy {

    @Getter
    private final RestClient restClient;

    @Getter
    @Setter
    private EsdbProperties esdbProperties;
    final OpenTelemetry openTelemetry;

    @Autowired
    public ESDBProxy(EsdbProperties props, OpenTelemetry openTelemetry) {
        this.esdbProperties = props;
        this.openTelemetry = openTelemetry;
        SpringWebTelemetry telemetry = SpringWebTelemetry.create(openTelemetry);

        JsonMapper mapper = JsonMapper.builder().disable(FAIL_ON_UNKNOWN_PROPERTIES).build();
;
        restClient = RestClient.builder()
                .requestFactory(new HttpComponentsClientHttpRequestFactory())
                .configureMessageConverters(client -> {
                    client.registerDefaults().withJsonConverter(new JacksonJsonHttpMessageConverter(mapper));
                })
                .defaultHeaders(headers -> {
                    headers.add(HttpHeaders.AUTHORIZATION, "Token "+props.getApiKey());
                    headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                })
                .requestInterceptors(interceptors -> {
                    interceptors.add(telemetry.createInterceptor());
                })
                .build();
    }



    /**
     * Get ESDB organizations from ESDB using GraphQL filtered by org type uuid.
     * @return Returns a list of GraphqlEsdbOrganization objects from the GraphQL response.
     */
    public List<GraphqlEsdbOrganization> gqlOrganizationList(String orgTypes) {

        HttpSyncGraphQlClient graphQlClient = createGraphqlClient();
        // See oscars/backend/src/main/resources/graphql-documents/orgList.graphql
        // Our GraphQL client can autoload by document name from the graphql-documents/ directory.
        GraphQlClient.RequestSpec requestSpec = graphQlClient.documentName("orgList");

        Map<String, Object> params = new HashMap<>();
        if (orgTypes != null && !orgTypes.isEmpty()) {
            params.put("orgTypes", orgTypes);
        }
        if (!params.isEmpty()) {
            requestSpec.variables(params);
        }

        ClientGraphQlResponse response = requestSpec.executeSync();

        return response
                .field("organizationList.list")
                .toEntityList(GraphqlEsdbOrganization.class);
    }

    /**
     * Get ESDB organizations from ESDB using GraphQL filtered by org type uuid.
     * @return Returns a list of GraphqlEsdbOrganization objects from the GraphQL response.
     */
    public List<GraphqlEsdbOrganizationType> gqlOrganizationTypeList() {

        HttpSyncGraphQlClient graphQlClient = createGraphqlClient();
        // See oscars/backend/src/main/resources/graphql-documents/orgTypeList.graphql
        // Our GraphQL client can autoload by document name from the graphql-documents/ directory.
        GraphQlClient.RequestSpec requestSpec = graphQlClient.documentName("orgTypeList");


        ClientGraphQlResponse response = requestSpec.executeSync();

        return response
                .field("organizationTypeList.list")
                .toEntityList(GraphqlEsdbOrganizationType.class);
    }

    /**
     * Get all ESDB bandwidth utilizations from ESDB using GraphQL. Targets vlanList.
     * @param search The search (string) parameter for vlanList.
     * @param sortProperty The sortProperty (string) parameter for vlanList.
     * @param first The first (int) parameter for vlanList.
     * @param skip The skip (int) parameter for vlanList.
     * @return Returns a list of EsdbVlan objects from the GraphQL response.
     */
    public List<EsdbBwUtil> gqlBwUtil(
            @Null String search,
            @Null String sortProperty,
            @Null Integer first,
            @Null Integer skip
    ) {
        List<EsdbBwUtil> results;

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

        HttpSyncGraphQlClient graphQlClient = createGraphqlClient();

        // Should return a List<EsdbBwUtil> in the "list" property
        // Example response payload:
        // {
        //  "data": {
        //    "bandwidthUtilizationList": {
        //      "count": 1646,
        //      "results": [
        //        {
        //          "id": "10943",
        //          "uuid": "",
        //          "system": "oscars",
        //          "remote_system_id": "OSCARS asdf-zyzz",
        //          "equipmentInterface": {
        //            "id": "14117"
        //          }
        //        },
        //        ...,
        //      ]
        //    }
        //  }
        // }

        // See oscars/backend/src/main/resources/graphql-documents/vlanList.graphql
        // Our GraphQL client can autoload by document name from the graphql-documents/ directory.
        GraphQlClient.RequestSpec requestSpec = graphQlClient
                .documentName("bandwidthUtilizationList");

        if (!params.isEmpty()) {
            requestSpec.variables(params);
        }
        ClientGraphQlResponse response = requestSpec.executeSync();

        List<GraphqlEsdbBandwidthUtilization> bwUtilList = response
                .field("bandwidthUtilizationList.results")
                .toEntityList(GraphqlEsdbBandwidthUtilization.class);

        results = new ArrayList<>(bwUtilList.size());
        if (!bwUtilList.isEmpty()) {
            log.info("ESDBProxy.gqlBwUtil() called. List of bwUtils has a size of {}", bwUtilList.size());
            for (GraphqlEsdbBandwidthUtilization bwUtil : bwUtilList) {
                EsdbBwUtil ebw = EsdbBwUtil.builder()
                        .id(bwUtil.getId())
                        .bandwidth(bwUtil.getBandwidth())
                        .equipmentInterface(bwUtil.getEquipmentInterface().getId())
                        .system(bwUtil.getSystem())
                        .remoteSystemId(bwUtil.getRemoteSystemId())
                        .build();
                results.add(ebw);
            }
        } else {
            log.warn("ESDBProxy.gqlBwUtil() called but none found in ESDB GraphQL response. Returning empty list.");
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


    public void createBandwidthUtilization(EsdbBwUtilPayload payload) {
        String restPath = esdbProperties.getUri()+"bandwidth_utilization/";
        restClient.post()
                .uri(restPath)
                .body(payload)
                .retrieve()
                .toEntity(EsdbBwUtil.class);
    }
    public void deleteBandwidthUtilization(Integer bwutilPkId) {
        String restPath = esdbProperties.getUri()+"bandwidth_utilization/"+bwutilPkId+"/";
        restClient.delete()
                .uri(restPath)
                .retrieve()
                .toBodilessEntity();
    }



    public Map<String, Object> buildArgList(
            @Nullable String search,
            @Nullable String sortProperty,
            @Nullable Integer first,
            @Nullable Integer skip,
            @Nullable List<String> uuids,
            @Nullable List<String> roles,
            @Nullable List<String> states
    ) {
        Map<String, Object> args = new HashMap<>();
        if (search != null && !search.isEmpty()) {
            args.put("search", search);
        }
        if (sortProperty != null && !sortProperty.isEmpty()) {
            args.put("sortProperty", sortProperty);
        }
        if (first != null) {
            args.put("first", first);
        }
        if (skip != null) {
            args.put("skip", skip);
        }
        if (uuids != null && !uuids.isEmpty()) {
            args.put("uuids", uuids);
        }

        if (roles != null && !roles.isEmpty()) {
            args.put("roles", roles);
        }

        if (states != null && !states.isEmpty()) {
            args.put("states", states);
        }

        return args;
    }

}
