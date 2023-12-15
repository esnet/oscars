package net.es.oscars.sb.nso.db;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import net.es.oscars.sb.nso.ent.NsoVirtInterface;

@Repository
public interface NsoVirtInterfaceDAO extends CrudRepository<NsoVirtInterface, Long> {
        List<NsoVirtInterface> findNsoVirtInterfaceByConnectionId(String connectionId);
}
