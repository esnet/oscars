package net.es.oscars.esdb;

import net.es.oscars.dto.esdb.gql.GraphqlEsdbNsiPeering;
import net.es.topo.common.dto.esdb.EsdbEqIfceBw;
import net.es.topo.common.dto.esdb.EsdbEquip;
import net.es.topo.common.dto.esdb.EsdbVlanWithDetails;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EsdbCache {
    public static final String EQUIP = "equip";
    public static final String VLAN = "vlan";
    public static final String EQ_IFCE_BW = "eq_ifce_bw";
    public static final String NSI_PEERING = "nsi_peering";

    private final EsdbEquipProxy esdbEquipProxy;
    private final EsdbVlanProxy esdbVlanProxy;

    public EsdbCache(EsdbEquipProxy esdbEquipProxy, EsdbVlanProxy esdbVlanProxy) {
        this.esdbEquipProxy = esdbEquipProxy;
        this.esdbVlanProxy = esdbVlanProxy;
    }

    @CacheEvict(value = "esdb-data", key = "#cacheKey")
    public void evictSingleValue(String cacheKey) {}

    @CacheEvict(value = "esdb-data")
    public void evictAllData() {}


    @Cacheable(value = "esdb-data", key = "#root.target.EQUIP")
    public List<EsdbEquip> getEquip() {
        return esdbEquipProxy.getEsdbEquip();
    }

    @Cacheable(value = "esdb-data", key = "#root.target.VLAN")
    public List<EsdbVlanWithDetails> getVlanWithDetails(){
        return esdbVlanProxy.gqlEsdbVlanWithDetailsList();
    }

    @Cacheable(value = "esdb-data", key = "#root.target.NSI_PEERING")
    public List<GraphqlEsdbNsiPeering> getNsiPeering(){
        return esdbEquipProxy.gqlEsdbNsiPeeringList();
    }

    @Cacheable(value = "esdb-data", key = "#root.target.EQ_IFCE_BW")
    public List<EsdbEqIfceBw> getEqIfceBw() {
        return esdbEquipProxy.gqlEsdbEquipmentInterfaceBandwidthList();

    }
}
