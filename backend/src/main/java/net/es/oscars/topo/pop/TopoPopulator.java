package net.es.oscars.topo.pop;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.TopoProperties;
import net.es.oscars.dto.topo.DeviceModel;
import net.es.oscars.topo.beans.*;
import net.es.oscars.topo.beans.IntRange;
import net.es.oscars.topo.enums.DeviceType;
import net.es.oscars.topo.enums.Layer;
import net.es.oscars.topo.svc.ConsistencyService;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.topo.common.model.oscars1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;


@Slf4j
@Service
public class TopoPopulator {
    private final TopoProperties topoProperties;
    private final TopologyStore topologyStore;
    private final ConsistencyService consistencySvc;
    private final RestTemplate restTemplate;

    @Autowired
    public TopoPopulator(TopologyStore topologyStore,
                         ConsistencyService consistencySvc,
                         TopoProperties topoProperties,
                         RestTemplateBuilder restTemplateBuilder) {
        this.topoProperties = topoProperties;
        this.consistencySvc = consistencySvc;
        this.topologyStore = topologyStore;

        this.restTemplate = restTemplateBuilder.build();

    }


    public void refresh() throws ConsistencyException, TopoException, IOException {
        log.info("topology refresh");
        try {
            Topology incoming = this.loadFromDiscovery();
            topologyStore.replaceTopology(incoming);
            // check consistency
            consistencySvc.checkConsistency();

        } catch (ResourceAccessException ex) {
            // typically an unknown host error
            log.info(ex.getMessage());

        } catch (TopoException ex) {
            log.error(ex.getMessage());
        }
    }

    public Topology loadFromDiscovery() throws TopoException, ResourceAccessException {
        log.info("loading topology from discovery");


        OscarsOneTopo discTopo = restTemplate.getForObject(topoProperties.getUrl(), OscarsOneTopo.class);
        if (discTopo == null) {
            log.warn("null discovery topology");
            return Topology.builder()
                    .adjcies(new ArrayList<>())
                    .ports(new HashMap<>())
                    .devices(new HashMap<>())
                    .build();

        } else if (discTopo.getAdjcies() == null) {
            throw new TopoException("null discovery topology adjacencies");
        } else if (discTopo.getDevices() == null) {
            throw new TopoException("null discovery topology devices");
        }
        List<Adjcy> adjcies = new ArrayList<>();
        for (OscarsOneAdjcy discAdjcy : discTopo.getAdjcies()) {
            Point a = Point.builder()
                    .port(discAdjcy.getA().getPort())
                    .device(discAdjcy.getA().getDevice())
                    .ifce(discAdjcy.getA().getIfce())
                    .addr(discAdjcy.getA().getAddr())
                    .build();
            Point z = Point.builder()
                    .port(discAdjcy.getZ().getPort())
                    .device(discAdjcy.getZ().getDevice())
                    .ifce(discAdjcy.getZ().getIfce())
                    .addr(discAdjcy.getZ().getAddr())
                    .build();
            Map<Layer, Long> metrics = new HashMap<>();
            for (String key : discAdjcy.getMetrics().keySet()) {
                metrics.put(Layer.valueOf(key), discAdjcy.getMetrics().get(key).longValue());
            }
            adjcies.add(Adjcy.builder()
                    .a(a)
                    .z(z)
                    .metrics(metrics)
                    .build());
        }
        Map<String, Device> devices = new HashMap<>();
        Map<String, Port> ports = new HashMap<>();

        for (OscarsOneDevice discDevice : discTopo.getDevices()) {
            Set<Layer> deviceCaps = new HashSet<>();
            for (String capString : discDevice.getCapabilities()) {
                deviceCaps.add(Layer.valueOf(capString));
            }
            Set<IntRange> devResVlans = new HashSet<>();
            for (OscarsOneVlan vlan : discDevice.getReservableVlans()) {
                devResVlans.add(IntRange.builder()
                                .ceiling(vlan.getCeiling())
                                .floor(vlan.getFloor())
                                .build());
            }
            Device d = Device.builder()
                    .ports(new HashSet<>())
                    .capabilities(new HashSet<>())
                    .ipv4Address(discDevice.getIpv4Address())
                    .longitude(Double.valueOf(discDevice.getLongitude()))
                    .latitude(Double.valueOf(discDevice.getLatitude()))
                    .location(discDevice.getLocation())
                    .model(DeviceModel.valueOf(discDevice.getModel().toString()))
                    .locationId(discDevice.getLocationId())
                    .type(DeviceType.valueOf(discDevice.getType()))
                    .urn(discDevice.getUrn())
                    .capabilities(deviceCaps)
                    .esdbEquipmentId(discDevice.getEsdbEquipmentId())
                    .reservableVlans(devResVlans)
                    .build();
            devices.put(d.getUrn(), d);
            for (OscarsOnePort discPort : discDevice.getPorts()) {
                Set<Layer> portCaps = new HashSet<>();
                for (String capString : discDevice.getCapabilities()) {
                    portCaps.add(Layer.valueOf(capString));
                }
                Set<IntRange> portResVlans = new HashSet<>();
                for (OscarsOneVlan vlan : discPort.getReservableVlans()) {
                    portResVlans.add(IntRange.builder()
                            .ceiling(vlan.getCeiling())
                            .floor(vlan.getFloor())
                            .build());
                }
                Set<Layer3Ifce> ifces = new HashSet<>();
                for (OscarsOnePort.OscarsOnePortIfce discIfce : discPort.getIfces()) {
                    ifces.add(Layer3Ifce.builder()
                            .port(discIfce.getPort())
                            .ipv4Address(discIfce.getIpv4Address())
                            .urn(discIfce.getUrn())
                            .build());
                }
                Port port = Port.builder()
                        .capabilities(portCaps)
                        .device(d)
                        .esdbEquipmentInterfaceId(discPort.getEsdbEquipmentInterfaceId())
                        .reservableEgressBw(discPort.getReservableEgressBw())
                        .reservableIngressBw(discPort.getReservableIngressBw())
                        .tags(new ArrayList<>(discPort.getTags()))
                        .ifces(ifces)
                        .urn(discPort.getUrn())
                        .reservableVlans(portResVlans)
                        .build();
                d.getPorts().add(port);
                ports.put(port.getUrn(), port);
            }
        }

        log.info("loaded a topology with "+devices.size()+" devices");

        return Topology.builder()
                .adjcies(adjcies)
                .ports(ports)
                .devices(devices)
                .build();

    }


}
