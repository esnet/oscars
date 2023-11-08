package net.es.oscars.pce;

import net.es.oscars.app.exc.PCEException;
import net.es.oscars.pce.beans.PathConstraint;
import net.es.oscars.resv.ent.VlanPipe;
import net.es.oscars.web.beans.PceResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Engine {
    PceResponse calculatePaths(VlanPipe requestPipe,
                                      Map<String, Integer> availIngressBw,
                                      Map<String, Integer> availEgressBw,
                                      PathConstraint constraint) throws PCEException;
}
