package net.es.oscars.nsi.svc;


import lombok.extern.slf4j.Slf4j;
import net.es.oscars.nsi.beans.NsiRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
@Slf4j
public class NsiRequestManager {

    // we use a map to track the reserve / modify requests that come in
    // remind that the flow is either
    // A. reserve -> commit from a null version to the defined version
    // B. modify -> commit | rollback, from one version to another version (or back to the original one)
    public Map<String, NsiRequest> inFlightRequests = new HashMap<>();

    public void addInFlightRequest(NsiRequest request) {
        inFlightRequests.put(request.getNsiConnectionId(), request);
    }

    public NsiRequest getInFlightRequest(String nsiConnectionId) {
        return inFlightRequests.get(nsiConnectionId);
    }

    // committing, aborting, timing out or failing a request removes it from the list of in-flight ones
    public void remove(String nsiConnectionId) {
        log.info("removing NsiRequest for " + nsiConnectionId);
        if (!inFlightRequests.containsKey(nsiConnectionId)) {
            log.info("could not find NsiRequest for " + nsiConnectionId);
        }
        inFlightRequests.remove(nsiConnectionId);
    }

    // returns any timed out modifies
    public List<NsiRequest> timedOut() {
        List<NsiRequest> result = new ArrayList<>();
        for (NsiRequest reserve : inFlightRequests.values()) {
            if (reserve.getTimeout().isBefore(Instant.now())) {
                result.add(reserve);
            }
        }
        return result;
    }


}
