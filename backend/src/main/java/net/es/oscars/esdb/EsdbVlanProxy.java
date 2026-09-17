package net.es.oscars.esdb;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.dto.esdb.gql.GraphqlEsdbVlan;
import net.es.topo.common.dto.esdb.EsdbVlan;
import net.es.topo.common.dto.esdb.EsdbVlanPayload;
import net.es.topo.common.dto.esdb.EsdbVlanWithDetails;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class EsdbVlanProxy {

    private final ESDBProxy esdbProxy;

    public EsdbVlanProxy(ESDBProxy esdbProxy) {
        this.esdbProxy = esdbProxy;
    }


    /**
     * Get all ESDB VLANs from ESDB using GraphQL. Targets vlanList.
     * @return Returns a list of EsdbVlan objects from the GraphQL response.
     */
    public List<EsdbVlan> gqlVlanList() {
        List<EsdbVlan> results;
        HttpSyncGraphQlClient graphQlClient = esdbProxy.createGraphqlClient();

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
                        .equipment(vlan.getEquipment().getId())
                        .equipmentInterface(vlan.getEquipmentInterface().getId())
                        .build();
                results.add(eV);
            }
        } else {
            log.warn("ESDBProxy.gqlVlanList() called but no ESDBVlans found in ESDB GraphQL response. Returning empty list.");
        }

        return results;
    }

    public List<EsdbVlanWithDetails> gqlEsdbVlanWithDetailsList() {
        HttpSyncGraphQlClient graphQlClient = esdbProxy.createGraphqlClient();
        List<EsdbVlanWithDetails> result = new ArrayList<>();
        Map<String, Object> args = esdbProxy.buildArgList(null, null, null, null, null, null, null);
        GraphQlClient.RequestSpec requestSpec = graphQlClient
                .documentName("vlanWithDetailsList");

        if (!args.isEmpty()) {
            requestSpec.variables(args);
        }

        ClientGraphQlResponse response = requestSpec.executeSync();
        List<GraphqlEsdbVlan> list = response
                .field("vlanList.list")
                .toEntityList(GraphqlEsdbVlan.class);
        if(!list.isEmpty()) {
            log.info("ESDBProxy.gqlEsdbVlanWithDetailsList() called. List of VLANs has a size of {}", list.size());
            for (GraphqlEsdbVlan entry : list) {
                List<EsdbVlanWithDetails.ServiceEdgeInline> serviceEdges = new ArrayList<>();
                List<String> switchPorts = new ArrayList<>();

                EsdbVlanWithDetails vlan = EsdbVlanWithDetails.builder()
                        .id(entry.getId())
                        .vlanId(entry.getVlanId())
                        .description(entry.getDescription())
//                    .bridge_id(entry.getBridgeId())
                        .equipment(
                                EsdbVlanWithDetails.EquipmentInline.builder()
                                        .id(entry.getEquipment().getId())
                                        // .url() // @TODO is there no URL property?
                                        .name(entry.getEquipment().getName())
                                        .build())
                        .equipmentInterface(
                                EsdbVlanWithDetails.EquipIfceInline.builder()
                                        .build()
                        )
                        .serviceEdges(serviceEdges)
                        .switchPorts(switchPorts)
                        .build();
                result.add(vlan);
            }
        }
        return result;
    }


    public void createVlan(EsdbVlanPayload payload) {
        String restPath = esdbProxy.getEsdbProperties().getUri()+"vlan/";
        esdbProxy.getRestClient().post()
                .uri(restPath)
                .body(payload)
                .retrieve()
                .toEntity(EsdbVlan.class);
    }

    public void deleteVlan(Integer vlanPkId) {
        String restPath = esdbProxy.getEsdbProperties().getUri()+"vlan/"+vlanPkId+"/";
        esdbProxy.getRestClient().delete()
                .uri(restPath)
                .retrieve()
                .toBodilessEntity();
    }
}
