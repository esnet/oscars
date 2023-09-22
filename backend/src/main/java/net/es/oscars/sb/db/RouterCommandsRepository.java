package net.es.oscars.sb.db;

import net.es.oscars.sb.ent.RouterCommands;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface RouterCommandsRepository extends JpaRepository<RouterCommands, Long> {

    List<RouterCommands> findAll();
    List<RouterCommands> findByConnectionId(String connectionId);
    List<RouterCommands> findByConnectionIdAndDeviceUrn(String connectionId, String deviceUrn);


}