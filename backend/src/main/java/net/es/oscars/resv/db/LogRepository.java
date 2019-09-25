package net.es.oscars.resv.db;

import net.es.oscars.resv.ent.EventLog;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LogRepository extends CrudRepository<EventLog, Long> {

    List<EventLog> findAll();
    Optional<EventLog> findByConnectionId(String connectionId);

}