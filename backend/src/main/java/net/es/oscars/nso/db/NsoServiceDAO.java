package net.es.oscars.nso.db;

import net.es.oscars.nso.ent.NsoService;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface NsoServiceDAO extends CrudRepository<NsoService, UUID> {
    Optional<NsoService> findTop1ByConnectionIdOrderByUpdatedDesc(String connectionId);
}
