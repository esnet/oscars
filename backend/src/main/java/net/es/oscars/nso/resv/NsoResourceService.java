package net.es.oscars.nso.resv;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import net.es.oscars.resv.db.ScheduleRepository;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.ent.Schedule;
import net.es.oscars.web.beans.Interval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    private ScheduleRepository scheduleRepo;


    public void reserve(Connection conn) throws NsoResvException {
        log.info("starting NSO resource reservation");
        Schedule sched = conn.getReserved().getSchedule();
        Interval interval = Interval.builder()
                .beginning(sched.getBeginning())
                .ending(sched.getEnding())
                .build();
        List<Schedule> overlappingSchedules = scheduleRepo.findOverlapping(interval.getBeginning(), interval.getEnding());
        nsoVcIdService.findAndReserveVcId(conn, overlappingSchedules);
        nsoQosSapPolicyIdService.findAndReserveQosSapPolicyIds(conn, overlappingSchedules);
        nsoSdpIdService.findAndReserveNsoSdpIds(conn, overlappingSchedules);

    }


    @Transactional
    public void release(Connection conn) throws NsoResvException {
        log.info("releasing all NSO resources for "+conn.getConnectionId());
        nsoVcIdService.release(conn);
        nsoQosSapPolicyIdService.release(conn);

    }

}
