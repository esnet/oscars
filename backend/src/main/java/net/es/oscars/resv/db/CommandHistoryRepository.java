package net.es.oscars.resv.db;

import net.es.oscars.sb.ent.RouterCommandHistory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommandHistoryRepository extends CrudRepository<RouterCommandHistory, Long> {

    List<RouterCommandHistory> findAll();
    List<RouterCommandHistory> findByConnectionId(String connectionId);

}