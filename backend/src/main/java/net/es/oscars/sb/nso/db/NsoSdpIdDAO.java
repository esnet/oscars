package net.es.oscars.sb.nso.db;

import net.es.oscars.sb.nso.ent.NsoSdpId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface NsoSdpIdDAO extends CrudRepository<NsoSdpId, Long> {
    List<NsoSdpId> findNsoSdpIdByScheduleId(Long scheduleId);
    List<NsoSdpId> findNsoSdpIdByConnectionId(String connectionId);
    List<NsoSdpId> findAllByScheduleId(Long scheduleId);
}
