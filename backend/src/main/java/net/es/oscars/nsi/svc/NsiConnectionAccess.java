package net.es.oscars.nsi.svc;

import net.es.oscars.app.exc.NsiException;
import net.es.oscars.nsi.beans.NsiErrors;
import net.es.oscars.nsi.ent.NsiMapping;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.Connection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class NsiConnectionAccess {

    private final ConnectionRepository connRepo;

    public NsiConnectionAccess(ConnectionRepository connRepo) {
        this.connRepo = connRepo;
    }

    // methods moved here to avoid transactional self-invocation

    @Transactional
    public Connection getOscarsConnection(NsiMapping mapping) throws NsiException {
        // log.debug("getting oscars connection for "+mapping.getOscarsConnectionId());
        Optional<Connection> c = connRepo.findByConnectionId(mapping.getOscarsConnectionId());
        if (c.isEmpty()) {
            throw new NsiException("OSCARS connection not found", NsiErrors.NO_SCH_ERROR);
        } else {
            return c.get();
        }
    }

    @Transactional
    public Optional<Connection> getMaybeOscarsConnection(NsiMapping mapping) {
        // log.debug("getting oscars connection for "+mapping.getOscarsConnectionId());
        return connRepo.findByConnectionId(mapping.getOscarsConnectionId());
    }

}
