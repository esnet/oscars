package net.es.oscars.topo.ent;

import lombok.*;
import net.es.oscars.topo.beans.TopoUrn;
import net.es.oscars.topo.enums.Layer;
import org.jgrapht.graph.DefaultWeightedEdge;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Point {
    @Id
    @GeneratedValue
    private Long id;

    private String device;
    private String port;
    private String addr;
    private String ifce;

    public String getPortUrn() {
        return this.port;
    }

    public String getUrn() {
        if (this.ifce == null || this.ifce.equals("")) {
            return this.getPortUrn();
        }
        return ifce;
    }

    public boolean same(Point b) {
        Map<String, Boolean> matches = new HashMap<>();
        if (this.device != null) {
            matches.put("device", this.device.equals(b.getDevice()));
        } else {
            matches.put("device", false);
            if (b.getDevice() == null) {
                matches.put("device", true);
            }
        }
        if (this.port != null) {
            matches.put("port", this.port.equals(b.getPort()));
        } else {
            matches.put("port", false);
            if (b.getPort() == null) {
                matches.put("port", true);
            }
        }
        if (this.addr != null) {
            matches.put("addr", this.addr.equals(b.getAddr()));
        } else {
            matches.put("addr", false);
            if (b.getAddr() == null) {
                matches.put("addr", true);
            }
        }
        if (this.ifce != null) {
            matches.put("ifce", this.ifce.equals(b.getIfce()));
        } else {
            matches.put("ifce", false);
            if (b.getIfce() == null) {
                matches.put("ifce", true);
            }
        }
        boolean allMatch = true;
        for (Boolean v: matches.values()) {
            if (!v) {
                allMatch = false;
            }
        }

        return allMatch;
    }



    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null ) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Point other = (Point) obj;
        return id != null && id.equals(other.getId());
    }

}
