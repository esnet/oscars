package net.es.oscars.sb.nso.db;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import net.es.oscars.sb.nso.ent.NsoVirtInterface;

@Repository
public interface NsoVirtInterfaceDAO extends CrudRepository<NsoVirtInterface, Long> {
        Set<NsoVirtInterface> findByConnectionId(String connectionId);
}
