package net.es.oscars.web.beans.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.oscars.topo.beans.Device;
import net.es.oscars.topo.beans.v2.BackbonePort;
import net.es.oscars.web.beans.Interval;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LspWaypoint {
    private BackbonePort port;
    private Device device;
    private WaypointType waypointType;

    public enum WaypointType {
        PORT, DEVICE
    }
}
