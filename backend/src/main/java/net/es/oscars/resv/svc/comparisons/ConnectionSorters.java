package net.es.oscars.resv.svc.comparisons;

import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.svc.ConnUtils;
import net.es.oscars.web.beans.ConnectionFilter;
import net.es.oscars.web.simple.Fixture;
import net.es.oscars.web.simple.SimpleConnection;
import net.es.oscars.web.simple.SimpleTag;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class ConnectionSorters {
    private final ConnUtils connUtils;

    public ConnectionSorters(ConnUtils connUtils) {
        this.connUtils = connUtils;
    }

    public Comparator<Connection> getComparator(ConnectionFilter.SortProperty sortProperty) {
        switch (sortProperty) {
            case CONNECTION_ID -> {
                return Comparator.comparing(Connection::getConnectionId);
            }
            case USERNAME -> {
                return Comparator.comparing(Connection::getUsername);
            }
            case CONNECTION_MTU -> {
                return Comparator.comparing(Connection::getConnection_mtu);
            }
            case MODE -> {
                return Comparator.comparing(Connection::getMode);
            }
            case STATE -> {
                return Comparator.comparing(Connection::getState);
            }
            case PHASE -> {
                return Comparator.comparing(Connection::getPhase);
            }
            case LAST_MODIFIED -> {
                return Comparator.comparing(Connection::getLast_modified);
            }
            case DESCRIPTION -> {
                return Comparator.comparing(Connection::getDescription);
            }
            case DEPLOYMENT_INTENT -> {
                return Comparator.comparing(Connection::getDeploymentIntent);
            }
            case DEPLOYMENT_STATE -> {
                return Comparator.comparing(Connection::getDeploymentState);
            }
            case SERVICE_ID -> {
                return Comparator.comparing(Connection::getServiceId);
            }
            case PORT -> {
                return new PortComparator(connUtils);
            }
            case TAGS -> {
                return new TagsComparator(connUtils);
            }
            case VLANS -> {
                return new VlanIdComparator(connUtils);
            }
            default -> {
                return null;
            }
        }
    }

    public static class PortComparator implements Comparator<Connection> {
        private final ConnUtils connUtils;

        public PortComparator(ConnUtils connUtils) {
            this.connUtils = connUtils;
        }

        @Override
        public int compare(Connection o1, Connection o2) {
            SimpleConnection c1 = connUtils.fromConnection(o1, true);
            SimpleConnection c2 = connUtils.fromConnection(o2, true);
            boolean c1Empty = (c1.getFixtures() == null || c1.getFixtures().isEmpty());
            boolean c2Empty = (c2.getFixtures() == null || c2.getFixtures().isEmpty());
            if (c1Empty && c2Empty) {
                return 0;
            } else if (c1Empty) {
                return -1;
            }else if (c2Empty) {
                return 1;
            } else {
                String c1Port = c1.getFixtures().getFirst().getPort();
                String c2Port = c2.getFixtures().getFirst().getPort();
                return c1Port.compareTo(c2Port);
            }
        }
    }
    public static class TagsComparator implements Comparator<Connection> {
        private final ConnUtils connUtils;

        public TagsComparator(ConnUtils connUtils) {
            this.connUtils = connUtils;
        }

        @Override
        public int compare(Connection o1, Connection o2) {
            SimpleConnection c1 = connUtils.fromConnection(o1, true);
            SimpleConnection c2 = connUtils.fromConnection(o2, true);
            boolean c1Empty = (c1.getTags() == null || c1.getTags().isEmpty());
            boolean c2Empty = (c2.getTags() == null || c2.getTags().isEmpty());
            if (c1Empty && c2Empty) {
                return 0;
            } else if (c1Empty) {
                return -1;
            }else if (c2Empty) {
                return 1;
            } else {
                c1.getTags().sort(Comparator.comparing(SimpleTag::getContents));
                c2.getTags().sort(Comparator.comparing(SimpleTag::getContents));
                String c1Tag = c1.getTags().getFirst().getContents();
                String c2Tag = c2.getTags().getFirst().getContents();
                return c1Tag.compareTo(c2Tag);
            }
        }
    }

    public static class VlanIdComparator implements Comparator<Connection> {
        private final ConnUtils connUtils;

        public VlanIdComparator(ConnUtils connUtils) {
            this.connUtils = connUtils;
        }

        @Override
        public int compare(Connection o1, Connection o2) {
            SimpleConnection c1 = connUtils.fromConnection(o1, true);
            SimpleConnection c2 = connUtils.fromConnection(o2, true);
            boolean c1Empty = (c1.getFixtures() == null || c1.getFixtures().isEmpty());
            boolean c2Empty = (c2.getFixtures() == null || c2.getFixtures().isEmpty());
            if (c1Empty && c2Empty) {
                return 0;
            } else if (c1Empty) {
                return -1;
            }else if (c2Empty) {
                return 1;
            } else {
                c1.getFixtures().sort(Comparator.comparing(Fixture::getVlan));
                c2.getFixtures().sort(Comparator.comparing(Fixture::getVlan));
                Integer c1Vlan = c1.getFixtures().getFirst().getVlan();
                Integer c2Vlan = c2.getFixtures().getFirst().getVlan();
                return c1Vlan.compareTo(c2Vlan);
            }
        }
    }
}
