package net.es.oscars.nsi.svc;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ReserveType;
import net.es.oscars.app.exc.NsiException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NsiReserve {
    public void validateReserve(ReserveType reserve) throws NsiException {
        return xxx;
    }


}
