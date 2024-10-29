package net.es.oscars.nsi.svc;


import lombok.extern.slf4j.Slf4j;
import net.es.oscars.nsi.beans.NsiModify;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
@Slf4j
public class NsiRequestManager {
    public Map<String, NsiModify> inFlightModifies = new HashMap<>();

    public void addInFlightModify(NsiModify modify) {
        inFlightModifies.put(modify.getNsiConnectionId(), modify);
    }

    public NsiModify getInFlightModify(String nsiConnectionId) {
        return inFlightModifies.get(nsiConnectionId);
    }

    // committing a modify just removes it from the list of in-flight ones
    public void commit(String nsiConnectionId) {
        inFlightModifies.remove(nsiConnectionId);
    }

    // returns any timed out modifies
    public List<NsiModify> timedOut() {
        List<NsiModify> result = new ArrayList<>();
        for (NsiModify modify : inFlightModifies.values()) {
            if (modify.getTimeout().isBefore(Instant.now())) {
                result.add(modify);
            }
        }
        return result;
    }

    // rolling back a modify also removes it from the list of in-flight ones
    public void rollback(String nsiConnectionId) {
        inFlightModifies.remove(nsiConnectionId);
    }

}
