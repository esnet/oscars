package net.es.oscars.nso.db;

import net.es.oscars.nso.ent.NsoQosSapPolicyId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface NsoQosSapPolicyIdDAO extends CrudRepository<NsoQosSapPolicyId, Long> {
    List<NsoQosSapPolicyId> findNsoQosSapPolicyIdByScheduleId(Long scheduleId);
    List<NsoQosSapPolicyId> findNsoQosSapPolicyIdByConnectionId(String connectionId);

}

