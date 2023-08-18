package net.es.oscars.nso.db;

import net.es.oscars.nso.ent.NsoSdpId;
import net.es.oscars.nso.ent.NsoVcId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface NsoVcIdDAO extends CrudRepository<NsoVcId, Long> {
    Optional<NsoVcId> findNsoVcIdByScheduleId(Long scheduleId);
    Optional<NsoVcId> findNsoVcIdByConnectionId(String connectionId);

}
