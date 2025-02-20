package net.es.oscars.topo.pop;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.spring.web.v3_1.SpringWebTelemetry;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.StartupProperties;
import net.es.oscars.app.props.TopoProperties;
import net.es.oscars.app.props.FeaturesProperties;
import net.es.oscars.dto.topo.DeviceModel;
import net.es.oscars.topo.beans.*;
import net.es.topo.common.model.oscars1.IntRange;
import net.es.oscars.topo.enums.DeviceType;
import net.es.oscars.topo.enums.Layer;
import net.es.oscars.topo.svc.ConsistencyService;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.topo.common.model.oscars1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.*;


@Slf4j
@Service
public class TopoPopulator {

    private final StartupProperties startupProperties;
    private final TopoProperties topoProperties;
    private final FeaturesProperties featuresProperties;
    private final TopologyStore topologyStore;
    private final ConsistencyService consistencySvc;
    private final RestTemplate restTemplate;
    final OpenTelemetry openTelemetry;

    @Autowired
    public TopoPopulator(TopologyStore topologyStore,
                         ConsistencyService consistencySvc,
                         TopoProperties topoProperties,
                         StartupProperties startupProperties,
                         RestTemplateBuilder restTemplateBuilder,
                         OpenTelemetry openTelemetry,
                         FeaturesProperties featuresProperties) {
        this.topoProperties = topoProperties;
        this.consistencySvc = consistencySvc;
        this.topologyStore = topologyStore;
        this.openTelemetry = openTelemetry;
        SpringWebTelemetry telemetry = SpringWebTelemetry.create(openTelemetry);

        this.restTemplate = restTemplateBuilder.build();
        this.restTemplate.getInterceptors().add(telemetry.newInterceptor());

        this.startupProperties = startupProperties;
        this.featuresProperties = featuresProperties;
    }


    public void refresh() throws ConsistencyException, TopoException, IOException {
        if (startupProperties.getStandalone()) {
            if (topologyStore.getTopology() != null) {
                log.info("already loaded a standalone topology, not refreshing");
                return;
            }
        }

        log.info("topology refresh");
        try {
            topologyStore.replaceTopology(this.loadTopology(null));
            // check consistency
            consistencySvc.checkConsistency();

        } catch (ResourceAccessException | IOException | TopoException ex) {
            // typically an unknown host error
            log.error(ex.getMessage());
        }
    }

    public Topology loadTopology(String filePath) throws TopoException, ResourceAccessException, IOException {


        OscarsOneTopo oscarsOneTopo;
        ObjectMapper mapper = new ObjectMapper();

        if (filePath != null) {
            var jsonFile = new ClassPathResource(filePath).getFile();
            oscarsOneTopo = mapper.readValue(jsonFile, OscarsOneTopo.class);

        } else if (startupProperties.getStandalone()) {
            log.info("loading standalone topology from config/topology.json");
            var jsonFile = new ClassPathResource("config/topology.json").getFile();
            oscarsOneTopo = mapper.readValue(jsonFile, OscarsOneTopo.class);
        } else {
            oscarsOneTopo = restTemplate.getForObject(topoProperties.getUrl(), OscarsOneTopo.class);
        }

        log.info("loading topology from discovery");
        if (oscarsOneTopo == null) {
            log.warn("null discovery topology");
            return Topology.builder()
                    .adjcies(new ArrayList<>())
                    .ports(new HashMap<>())
                    .devices(new HashMap<>())
                    .build();

        } else if (oscarsOneTopo.getAdjcies() == null) {
            throw new TopoException("null discovery topology adjacencies");
        } else if (oscarsOneTopo.getDevices() == null) {
            throw new TopoException("null discovery topology devices");
        }
        List<Adjcy> adjcies = new ArrayList<>();
        for (OscarsOneAdjcy discAdjcy : oscarsOneTopo.getAdjcies()) {
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

        for (OscarsOneDevice discDevice : oscarsOneTopo.getDevices()) {
            Set<Layer> deviceCaps = new HashSet<>();
            for (OscarsOneCapability os1Cap : discDevice.getCapabilities()) {
                deviceCaps.add(Layer.valueOf(os1Cap.toString()));
            }
            Set<IntRange> devResVlans = new HashSet<>();
            for (net.es.topo.common.model.oscars1.IntRange vlan : discDevice.getReservableVlans()) {
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
                // If this is an 'untagged' port, and features.untagged-ports is disabled, skip.
                // If this is a QINQ port, and features.qinq-ports is disabled, skip.
                if (

                    ( featuresProperties.getUntaggedPorts() == false
                      && discPort.getEthernetEncapsulation() == EthernetEncapsulation.NULL )

                    || ( featuresProperties.getQinqPorts() == false
                      && discPort.getEthernetEncapsulation() == EthernetEncapsulation.QINQ )
                ) {
                    continue;
                }


                Set<Layer> portCaps = new HashSet<>();
                for (OscarsOneCapability os1Cap : discPort.getCapabilities()) {
                    portCaps.add(Layer.valueOf(os1Cap.toString()));
                }
                Set<IntRange> portResVlans = new HashSet<>();
                for (net.es.topo.common.model.oscars1.IntRange vlan : discPort.getReservableVlans()) {
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
