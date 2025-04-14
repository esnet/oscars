package net.es.oscars.nsi.svc;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ReserveType;
import net.es.oscars.app.exc.NsiException;
import net.es.oscars.nsi.ent.NsiMapping;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NsiReserve {
    public void validateReserve(ReserveType reserve) throws NsiException {
        return;
    }

    public void foo() {
        NsiMapping mapping;
        if (nsiMappingService.hasNsiMapping(reserve.getConnectionId())) {
            mapping = nsiMappingService.getMapping(reserve.getConnectionId());

            log.info("found existing mapping, triggering a modify");
            // pass in the new version
            nsiService.modify(header.value, reserve, mapping, reserve.getCriteria().getVersion());
        } else {
            mapping = nsiMappingService.newMapping(
                    reserve.getConnectionId(),
                    reserve.getGlobalReservationId(),
                    header.value.getRequesterNSA(),
                    reserve.getCriteria().getVersion()
            );

            log.info("no existing mapping, triggering a reserve");
            nsiService.reserve(header.value, reserve, mapping);
        }

        log.info("returning reserve ack");
    }
}
