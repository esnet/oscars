package net.es.oscars.sb.nso.db;

import net.es.oscars.sb.nso.ent.NsoSdpVcId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface NsoSdpVcIdDAO extends CrudRepository<NsoSdpVcId, Long> {
    List<NsoSdpVcId> findNsoSdpVcIdByScheduleId(Long scheduleId);
    List<NsoSdpVcId> findNsoSdpVcIdByConnectionId(String connectionId);

}
