package net.es.oscars.nso;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.NsoProperties;
import net.es.oscars.nso.db.NsoVcIdDAO;
import net.es.oscars.nso.ent.NsoQosSapPolicyId;
import net.es.oscars.nso.ent.NsoVcId;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.ent.Schedule;
import net.es.oscars.topo.beans.IntRange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static net.es.oscars.nso.IntegerSet.availableFromUsedSetAndAllowedString;

@Component
@Slf4j
public class NsoVcIdService {
    @Autowired
    private NsoProperties nsoProperties;

    @Autowired
    private NsoVcIdDAO nsoVcIdDAO;


    @Transactional
    public void findAndReserveVcId(Connection conn, List<Schedule> schedules) throws NsoResvException {
        Integer vcId = this.findUnusedVcIdKnowingSchedules(schedules);
        log.info("found unused VC id "+vcId);
        NsoVcId nsoVcId = NsoVcId.builder()
                .vcId(vcId)
                .connectionId(conn.getConnectionId())
                .scheduleId(conn.getReserved().getSchedule().getId())
                .build();
        this.nsoVcIdDAO.save(nsoVcId);
        log.info("reserved VC id "+vcId+" for "+conn.getConnectionId());
    }

    public Integer findUnusedVcIdKnowingSchedules(List<Schedule> schedules) throws NsoResvException {
        Set<Integer> usedVcIds = new HashSet<>();
        schedules.forEach(s -> {
            nsoVcIdDAO.findNsoVcIdByScheduleId(s.getId()).ifPresent(nsoVcId -> {
                usedVcIds.add(nsoVcId.getVcId());
            });
        });

        Set<Integer> availableVcIds = availableFromUsedSetAndAllowedString(usedVcIds, nsoProperties.getVcIdRange());
        if (availableVcIds.isEmpty()) {
            throw new NsoResvException("no VC id available");
        }
        return Collections.min(availableVcIds);

    }



    @Transactional
    public void release(Connection conn) throws NsoResvException {
        log.info("releasing all NSO vc-ids for "+conn.getConnectionId());
        nsoVcIdDAO.findNsoVcIdByConnectionId(conn.getConnectionId()).ifPresent(vcId -> nsoVcIdDAO.delete(vcId));
    }

}
