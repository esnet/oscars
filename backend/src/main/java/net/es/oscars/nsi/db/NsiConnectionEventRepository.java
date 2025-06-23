package net.es.oscars.nsi.db;

import net.es.oscars.nsi.ent.NsiConnectionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NsiConnectionEventRepository extends JpaRepository<NsiConnectionEvent, Long> {
    List<NsiConnectionEvent> findByNsiConnectionId(String nsiConnectionId);
}