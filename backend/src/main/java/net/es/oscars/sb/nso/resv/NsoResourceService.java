package net.es.oscars.sb.nso.resv;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import net.es.oscars.resv.db.ScheduleRepository;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.ent.Schedule;
import net.es.oscars.model.Interval;
import net.es.oscars.sb.nso.ent.NsoVcId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
@Slf4j
public class NsoResourceService {

    @Autowired
    private NsoVcIdService nsoVcIdService;

    @Autowired
    private NsoQosSapPolicyIdService nsoQosSapPolicyIdService;

    @Autowired
    private NsoSdpIdService nsoSdpIdService;

    @Autowired
    private NsoSdpVcIdService nsoSdpVcIdService;

    @Autowired
    private ScheduleRepository scheduleRepo;

    public void migrate(Long newScheduleId, Long oldScheduleId, Map<Long, Long> fixtureIdMap) {
        nsoVcIdService.migrate(newScheduleId, oldScheduleId);
        nsoSdpIdService.migrate(newScheduleId, oldScheduleId);
        nsoQosSapPolicyIdService.migrate(newScheduleId, oldScheduleId, fixtureIdMap);
        nsoSdpVcIdService.migrate(newScheduleId, oldScheduleId);
    }

    public void reserve(Connection conn) throws NsoResvException {
        log.info("starting NSO resource reservation");
        Schedule connectionSchedule = conn.getReserved().getSchedule();

        // we get all the NsoVcIds; every RESERVED connection should have one.
        List<NsoVcId> allNsoVcIds = nsoVcIdService.allNsoVcIds();


        List<Schedule> overlappingSchedules = new ArrayList<>();
        for (NsoVcId nsoVcId : allNsoVcIds) {
            // from there we can retrieve the schedule objects associated with each NsoVcId
            scheduleRepo.findById(nsoVcId.getScheduleId()).ifPresent(nsoVcIdSchedule -> {
                /*
                 * evaluate each to see if it overlaps with the Schedule from the incoming Connection
                 *
                 * an interval A overlaps with another interval B if the following is true:
                 *     max(A.beginning, B.beginning) <=  min(A.ending, B.ending)
                 */
                Instant maxBeginning = nsoVcIdSchedule.getBeginning();
                if (connectionSchedule.getBeginning().isAfter(maxBeginning)) {
                    maxBeginning = connectionSchedule.getBeginning();
                }
                Instant minEnding = nsoVcIdSchedule.getEnding();
                if (connectionSchedule.getEnding().isBefore(minEnding)) {
                    minEnding = connectionSchedule.getEnding();
                }
                if (maxBeginning.isBefore(minEnding)) {
                    overlappingSchedules.add(nsoVcIdSchedule);
                }
            });
        }

        nsoVcIdService.findAndReserveVcId(conn, overlappingSchedules);
        nsoQosSapPolicyIdService.findAndReserveQosSapPolicyIds(conn, overlappingSchedules);
        nsoSdpIdService.findAndReserveNsoSdpIds(conn, overlappingSchedules);
        nsoSdpVcIdService.findAndReserveNsoSdpVcIds(conn, overlappingSchedules);
        log.info("starting NSO resource reservation");

    }


    @Transactional
    public void release(Connection conn) throws NsoResvException {
        log.info("releasing all NSO resources for "+conn.getConnectionId());
        nsoVcIdService.release(conn);
        nsoQosSapPolicyIdService.release(conn);

    }

}
