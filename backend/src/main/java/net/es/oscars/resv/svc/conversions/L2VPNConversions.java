package net.es.oscars.resv.svc.conversions;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.model.*;
import net.es.oscars.model.enums.*;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.resv.svc.ResvService;
import net.es.oscars.topo.TopoService;
import net.es.oscars.topo.beans.Device;
import net.es.oscars.topo.beans.PortBwVlan;
import net.es.oscars.topo.beans.Topology;
import net.es.oscars.topo.beans.v2.EdgePort;
import net.es.oscars.topo.enums.UrnType;
import net.es.oscars.topo.pop.ConsistencyException;
import net.es.oscars.topo.svc.TopologyStore;
import net.es.oscars.web.beans.PceMode;
import net.es.oscars.web.simple.Fixture;
import net.es.oscars.web.simple.Junction;
import net.es.oscars.web.simple.Pipe;
import net.es.oscars.web.simple.SimpleConnection;
import net.es.topo.common.model.oscars2.OscarsTopology;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
@Slf4j
public class L2VPNConversions {
    private final ConnService connSvc;
    private final ResvService resvService;
    private final TopologyStore topologyStore;

    public L2VPNConversions(ConnService connSvc, ResvService resvService, TopologyStore topologyStore) {
        this.connSvc = connSvc;
        this.resvService = resvService;
        this.topologyStore = topologyStore;
    }


    public L2VPN fromConnection(Connection c) {

        L2VPN l2VPN = L2VPN.builder()
                .schedule(getInterval(c))
                .qos(getQos(c))
                .tech(getTech(c))
                .status(getStatus(c))
                .meta(getMeta(c))
                .bundles(getBundles(c))
                .endpoints(getEndpoints(c))
                .build();
        for (Endpoint endpoint : l2VPN.getEndpoints()) {
            endpoint.setL2vpn(l2VPN);
        }
        for (Bundle b : l2VPN.getBundles()) {
            b.setL2vpn(l2VPN);
        }

        return l2VPN;
    }

    public List<Endpoint> getEndpoints(Connection c) {
        List<Endpoint> endpoints = new ArrayList<>();
        Components cmp = getComponents(c);
        Topology topology = null;

        Map<String, PortBwVlan> available = resvService.available(getInterval(c), connSvc.getHeld(), c.getConnectionId());
        Map<String, Map<Integer, Set<String>>> vlanUsageMap = resvService.vlanUsage(getInterval(c), connSvc.getHeld(), c.getConnectionId());


        for (VlanFixture f: cmp.getFixtures()) {
            boolean tagged = f.getVlan().getVlanId() == 0;
            EdgePort ep = null;
            try {
                topology = topologyStore.getCurrentTopology();
                if (topology != null) {
                    for (Device d : topology.getDevices().values()) {
                        for (net.es.oscars.topo.beans.Port p : d.getPorts()) {
                            if (p.getUrn().equals(f.getPortUrn())) {
                                ep = TopoService.mapEdgePort(p, available, vlanUsageMap);
                            }
                        }
                    }
                }
            } catch (ConsistencyException e) {
                log.error("can't get edge ports, exception: ", e);
            }
            Endpoint endpoint = Endpoint.builder()
                    .vlan(f.getVlan().getVlanId())
                    .port(f.getPortUrn())
                    .device(f.getJunction().getDeviceUrn())
                    .tagged(tagged)
                    .edgePort(ep)
                    .build();

            endpoints.add(endpoint);
        }

        return endpoints;
    }


    public List<Bundle> getBundles(Connection c) {
        List<Bundle> bundles = new ArrayList<>();
        Components cmp = getComponents(c);

        for (VlanPipe vlanPipe : cmp.getPipes()) {
            Bundle b = Bundle.builder()
                    .a(vlanPipe.getA().getDeviceUrn())
                    .z(vlanPipe.getZ().getDeviceUrn())
                    .name(vlanPipe.getA().getDeviceUrn()+"---"+vlanPipe.getZ().getDeviceUrn())
                    .build();

            List<LSP> lsps = new ArrayList<>();
            Protection protection = Protection.NONE;
            if (vlanPipe.getProtect()) {
                protection = Protection.LOOSE;
                List<String> path = new ArrayList<>();
                path.add(vlanPipe.getA().getDeviceUrn());
                path.add(vlanPipe.getZ().getDeviceUrn());
                LSP loose = LSP.builder()
                        .path(path)
                        .role(Role.SECONDARY)
                        .bundle(b)
                        .build();
                lsps.add(loose);
            }

            b.setProtection(protection);
            b.setLsps(lsps);
            b.setConstraints(Bundle.Constraints.builder()
                    .exclude(new HashSet<>())
                    .include(eroAsStringList(vlanPipe.getAzERO()))
                    .build());
            bundles.add(b);
        }
        return bundles;
    }

    public List<Bundle.Waypoint> eroAsStringList(List<EroHop> ero) {
        List<Bundle.Waypoint> list = new ArrayList<>();
        try {
            Topology topology = topologyStore.getCurrentTopology();
            ero.forEach(e -> {
                UrnType urnType = UrnType.PORT;
                if (topology.getDevices().containsKey(e.getUrn())) {
                    urnType = UrnType.DEVICE;
                }
                Bundle.Waypoint waypoint = Bundle.Waypoint.builder()
                        .urn(e.getUrn())
                        .type(urnType)
                        .build();
                list.add(waypoint);
            });
            return list;

        } catch (ConsistencyException e) {
            log.error("can't get waypoints, exception: ", e);
            throw new RuntimeException(e);
        }

    }

    public L2VPN.Meta getMeta(Connection c) {
        return L2VPN.Meta.builder()
                .description(c.getDescription())
                .username(c.getUsername())
                .trackingId(c.getServiceId())
                .orchId(null)
                .build();
    }

    public L2VPN.Status getStatus(Connection c) {
        return L2VPN.Status.builder()
                .state(c.getState())
                .phase(c.getPhase())
                .deploymentIntent(c.getDeploymentIntent())
                .deploymentState(c.getDeploymentState())
                .build();
    }

    public L2VPN.Tech getTech(Connection c)  {
        return L2VPN.Tech.builder()
                .flavor(Flavor.VPLS)
                .mtu(c.getConnection_mtu())
                .macsec(false)
                .mode(c.getMode())
                .build();
    }

    public L2VPN.Qos getQos(Connection c) {

        Components cmp = getComponents(c);
        QosMode qosMode = QosMode.GUARANTEED;
        int bandwidth = getBandwidth(cmp);
        if (bandwidth == 0) {
            qosMode = QosMode.BEST_EFFORT;
        }
        QosExcessAction qosExcessAction = getExcessAction(cmp);

        return L2VPN.Qos.builder()
                .bandwidth(bandwidth)
                .excessAction(qosExcessAction)
                .mode(qosMode)
                .build();
    }

    public QosExcessAction getExcessAction(Components cmp) {
        for (VlanFixture f : cmp.getFixtures()) {
            if (f.getStrict()) {
                return QosExcessAction.DROP;
            }
        }
        return QosExcessAction.SCAVENGER;
    }

    public int getBandwidth(Components cmp) {
        int bandwidth = 0;
        for (VlanPipe p : cmp.getPipes()) {
            if (p.getAzBandwidth() > bandwidth) {
                bandwidth = p.getAzBandwidth();
            } else if (p.getZaBandwidth() > bandwidth) {
                bandwidth = p.getZaBandwidth();
            }
        }
        for (VlanFixture f : cmp.getFixtures()) {
            if (f.getIngressBandwidth() > bandwidth) {
                bandwidth = f.getIngressBandwidth();
            } else if (f.getEgressBandwidth() > bandwidth) {
                bandwidth = f.getEgressBandwidth();
            }
        }

        return bandwidth;

    }

    public Components getComponents(Connection c) {
        if (c.getPhase().equals(Phase.HELD)) {
            return c.getHeld().getCmp();
        } else if (c.getPhase().equals(Phase.RESERVED)) {
            return c.getReserved().getCmp();
        } else {
            return c.getArchived().getCmp();
        }

    }

    public Interval getInterval(Connection c) {
        Schedule sch;
        if (c.getPhase().equals(Phase.HELD)) {
            sch = c.getHeld().getSchedule();
        } else if (c.getPhase().equals(Phase.RESERVED)) {
            sch = c.getReserved().getSchedule();
        } else {
            sch = c.getArchived().getSchedule();
        }
        return Interval.builder().beginning(sch.getBeginning()).ending(sch.getEnding()).build();
    }



    public SimpleConnection fromL2VPN(L2VPN l2VPNRequest) {
        Integer begin  = Long.valueOf(l2VPNRequest.getSchedule().getBeginning().getEpochSecond()).intValue();
        Integer end  = Long.valueOf(l2VPNRequest.getSchedule().getEnding().getEpochSecond()).intValue();
        Integer heldUntil = Long.valueOf(Instant.now().getEpochSecond() + 60).intValue();

        List<Fixture> fixtures = new ArrayList<>();
        List<Pipe> pipes = new ArrayList<>();
        Set<Junction> junctions = new HashSet<>();

        for (Endpoint endpoint : l2VPNRequest.getEndpoints()) {
            junctions.add(Junction.builder().device(endpoint.getDevice()).build());
            fixtures.add(Fixture.builder()
                    .junction(endpoint.getDevice())
                    .inMbps(l2VPNRequest.getQos().getBandwidth())
                    .outMbps(l2VPNRequest.getQos().getBandwidth())
                    .mbps(l2VPNRequest.getQos().getBandwidth())
                    .port(endpoint.getPort())
                    .vlan(endpoint.getVlan())
                    .strict(l2VPNRequest.getQos().getExcessAction().equals(QosExcessAction.DROP))
                    .build());
        }
        for (Bundle bundle : l2VPNRequest.getBundles()) {
            LSP primary = null;
            for (LSP lsp : bundle.getLsps()) {
                if (lsp.getRole().equals(Role.PRIMARY)) {
                    primary = lsp;
                }
            }
            if (primary != null) {
                List<String> ero = new ArrayList<>();
                if (primary.getPath() != null && !primary.getPath().isEmpty()) {
                    ero = primary.getPath();
                } else if (bundle.getConstraints().getInclude() != null && !bundle.getConstraints().getInclude().isEmpty()) {
                    ero = bundle.getConstraints().includedUrns();
                }

                pipes.add(Pipe.builder()
                        .azMbps(l2VPNRequest.getQos().getBandwidth())
                        .zaMbps(l2VPNRequest.getQos().getBandwidth())
                        .mbps(l2VPNRequest.getQos().getBandwidth())
                        .a(bundle.getA())
                        .z(bundle.getZ())
                        .ero(ero)
                        .exclude(new ArrayList<>(bundle.getConstraints().excludedUrns()))
                        .pceMode(PceMode.BEST)
                        .build());

            }
        }

        return SimpleConnection.builder()
                .username(l2VPNRequest.getMeta().getUsername())
                .connection_mtu(l2VPNRequest.getTech().getMtu())
                .connectionId(l2VPNRequest.getName())
                .mode(l2VPNRequest.getTech().getMode())
                .begin(begin)
                .end(end)
                .description(l2VPNRequest.getMeta().getDescription())
                .phase(l2VPNRequest.getStatus().getPhase())
                .state(l2VPNRequest.getStatus().getState())
                .heldUntil(heldUntil)
                .serviceId(l2VPNRequest.getMeta().getTrackingId())
                .fixtures(fixtures)
                .junctions(new ArrayList<>(junctions))
                .pipes(pipes)
                .build();

    }
}
