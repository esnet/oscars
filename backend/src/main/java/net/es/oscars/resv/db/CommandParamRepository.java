package net.es.oscars.resv.db;

import net.es.oscars.resv.ent.CommandParam;
import net.es.oscars.resv.ent.Schedule;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommandParamRepository extends CrudRepository<CommandParam, Long> {

    List<CommandParam> findAll();
    List<CommandParam> findByConnectionId(String connectionId);
    List<CommandParam> findBySchedule(Schedule s);

}