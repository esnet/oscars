package net.es.oscars.esdb;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.dto.esdb.gql.GraphqlEsdbEquipment;
import net.es.oscars.dto.esdb.gql.GraphqlEsdbEquipmentInterface;
import net.es.oscars.dto.esdb.gql.GraphqlEsdbInterfaceBandwidth;
import net.es.oscars.dto.esdb.gql.GraphqlEsdbNsiPeering;
import net.es.topo.common.dto.esdb.*;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class EsdbEquipProxy {

    private final ESDBProxy esdbProxy;

    public EsdbEquipProxy(ESDBProxy esdbProxy) {
        this.esdbProxy = esdbProxy;
    }


    public List<EsdbEquip> getEsdbEquip() throws RestClientException {
        log.info("fetching esdb equipment");
        List<EsdbEquip> results = new ArrayList<>();
        try {
            results = gqlEsdbEquipmentList();
        } catch (Exception ex) {
            throw new RestClientException(ex.getMessage(), ex);
        }
        return results;
    }

    public List<EsdbEquip> gqlEsdbEquipmentList() throws Exception {
        // Only retrieve equipment with the role of "Core Router" (UUID "95ed0ea2-cdea-42ba-90c2-94ed749653c3")
        // with equipment state of "Production" (UUID "fbd63047-7bfd-412b-858d-05fbd4837939")
        List<String> roleUUIDs = new ArrayList<>();
        List<String> stateUUIDs = new ArrayList<>();
        roleUUIDs.add("95ed0ea2-cdea-42ba-90c2-94ed749653c3");
        stateUUIDs.add("fbd63047-7bfd-412b-858d-05fbd4837939");
        return gqlEsdbEquipmentList(null, null, null, null, null, roleUUIDs, stateUUIDs);
    }


    public List<EsdbEquip> gqlEsdbEquipmentList(
            @Nullable String search,
            @Nullable String sortProperty,
            @Nullable Integer first,
            @Nullable Integer skip,
            @Nullable List<String> uuids,
            @Nullable List<String> roles,
            @Nullable List<String> states
    ) throws Exception {
        HttpSyncGraphQlClient graphQlClient = esdbProxy.createGraphqlClient();
        List<EsdbEquip> result = new ArrayList<>();
        Map<String, Object> args =  esdbProxy.buildArgList(search, sortProperty, first, skip, uuids, roles, states);
        // See topo-discovery/oscars/src/main/resources/graphql-documents/equipmentList.graphql
        // Our GraphQL client can autoload by document name from the graphql-documents/ directory.
        GraphQlClient.RequestSpec requestSpec = graphQlClient
                .documentName("equipmentList");

        if (!args.isEmpty()) {
            requestSpec.variables(args);
        }

        ClientGraphQlResponse response = requestSpec.executeSync();
        List<GraphqlEsdbEquipment> list = response
                .field("equipmentList.list")
                .toEntityList(GraphqlEsdbEquipment.class);

        if(!list.isEmpty()) {
            log.info("ESDBProxy.gqlEsdbEquipmentList() called. List of equipment has a size of {}", list.size());
            for (GraphqlEsdbEquipment entry : list) {

                List<EsdbEquip.EsdbEquipmentIfceInline> ifces = new ArrayList<>();

                if (!entry.getInterfaces().isEmpty()) {
                    for (GraphqlEsdbEquipmentInterface ifc : entry.getInterfaces()) {
                        ifces.add(
                                EsdbEquip.EsdbEquipmentIfceInline.builder()
                                        .id(ifc.getId())
                                        .ifce(ifc.getEquipmentInterface())
                                        .ifceBw(
                                                ifc.getInterfaceBandwidth() != null
                                                        ? ifc.getInterfaceBandwidth().getId()
                                                        : 0
                                        )
                                        // DEPRECATED:
                                        // .speedMbps
                                        .oscarsBwPercent(ifc.getOscarsBandwidth())
                                        .speed(
                                                ifc.getInterfaceBandwidth() != null
                                                        ? ifc.getInterfaceBandwidth().getSpeed()
                                                        : 0
                                        )
                                        .tagged(ifc.isTagged())
                                        .build()
                        );
                    }
                }

                EsdbEquip equip = EsdbEquip.builder()
                        .id( entry.getId() )
                        .name( entry.getName() )
                        .orchId(entry.getOrchId() )
                        .role( entry.getRole().getId() )
                        .equipmentState( entry.getEquipmentState().getId() )
                        .network( entry.getNetwork().getId() )
                        .location(
                                entry.getLocation() != null
                                        ? EsdbLocation.builder()
                                        .id( entry.getLocation().getId() )
                                        .shortName( entry.getLocation().getShortName() )
                                        .build()
                                        : null
                        )
                        .model(
                                entry.getModel() != null
                                        ? EsdbEquip.EsdbModelInline.builder()
                                        .id( entry.getModel().getId() )
                                        .name( entry.getModel().getName() )
                                        .manufacturer( entry.getModel().getManufacturer().getShortName() )
                                        .build()
                                        : null
                        )
                        .platform(
                                entry.getPlatform() != null
                                        ? EsdbPlatform.builder()
                                        .id( entry.getPlatform().getId() )
                                        .name( entry.getPlatform().getName() )
                                        .manufacturer( entry.getPlatform().getManufacturer().getShortName() )
                                        .description( entry.getPlatform().getDescription() )
                                        .build()
                                        : null
                        )
                        .ifces(
                                ifces
                        )
                        .build();
                result.add(equip);
            }
        }
        return result;
    }


    public List<GraphqlEsdbNsiPeering> gqlEsdbNsiPeeringList() {
        HttpSyncGraphQlClient graphQlClient = esdbProxy.createGraphqlClient();
        GraphQlClient.RequestSpec requestSpec = graphQlClient.documentName("nsiPeeringList");


        ClientGraphQlResponse response = requestSpec.executeSync();
        List<GraphqlEsdbNsiPeering> result = response
                .field("nsiPeeringList.list")
                .toEntityList(GraphqlEsdbNsiPeering.class);
        log.info("ESDBProxy.gqlEsdbNsiPeeringList() called. List of NSI peering has a size of {}", result.size());

        return result;
    }


    public List<EsdbEqIfceBw> gqlEsdbEquipmentInterfaceBandwidthList() {
        HttpSyncGraphQlClient graphQlClient = esdbProxy.createGraphqlClient();
        List<EsdbEqIfceBw> result = new ArrayList<>();
        GraphQlClient.RequestSpec requestSpec = graphQlClient
                .documentName("interfaceBandwidthList");

        ClientGraphQlResponse response = requestSpec.executeSync();
        List<GraphqlEsdbInterfaceBandwidth> list = response
                .field("interfaceBandwidthList.list")
                .toEntityList(GraphqlEsdbInterfaceBandwidth.class);
        if(!list.isEmpty()) {
            log.info("ESDBProxy.gqlEsdbEquipmentInterfaceBandwidthList() called. List of Interface Bandwidth has a size of {}", list.size());
            for (GraphqlEsdbInterfaceBandwidth entry : list) {
                EsdbEqIfceBw ifceBw = EsdbEqIfceBw.builder()
                        .id(entry.getId())
                        // .name(entry.getName()) // @TODO: No name property in EsdbEqIfceBw?
                        .speed(entry.getSpeed())
                        .build();
                result.add(ifceBw);
            }
        }
        return result;
    }
}
