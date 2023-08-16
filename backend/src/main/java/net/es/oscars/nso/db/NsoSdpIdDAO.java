package net.es.oscars.nso.db;

import net.es.oscars.nso.ent.NsoSdpId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface NsoSdpIdDAO extends CrudRepository<NsoSdpId, Long> {
    List<NsoSdpId> findNsoSdpIdByScheduleId(Long scheduleId);
    List<NsoSdpId> findNsoSdpIdByConnectionId(String connectionId);

}
