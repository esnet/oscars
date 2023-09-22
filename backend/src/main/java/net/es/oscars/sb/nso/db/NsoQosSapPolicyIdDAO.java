package net.es.oscars.sb.nso.db;

import net.es.oscars.sb.nso.ent.NsoQosSapPolicyId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface NsoQosSapPolicyIdDAO extends CrudRepository<NsoQosSapPolicyId, Long> {
    List<NsoQosSapPolicyId> findNsoQosSapPolicyIdByScheduleId(Long scheduleId);
    List<NsoQosSapPolicyId> findNsoQosSapPolicyIdByConnectionId(String connectionId);
    Optional<NsoQosSapPolicyId> findNsoQosSapPolicyIdByFixtureId(Long fixtureId);

}

