package net.es.oscars.sb.nso.resv;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.NsoProperties;
import net.es.oscars.sb.nso.db.NsoQosSapPolicyIdDAO;
import net.es.oscars.sb.nso.ent.NsoQosSapPolicyId;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.ent.Schedule;
import net.es.oscars.resv.ent.VlanFixture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static net.es.oscars.sb.nso.resv.IntegerSet.availableFromUsedSetAndAllowedString;

@Component
@Slf4j
public class NsoQosSapPolicyIdService {
    @Autowired
    private NsoProperties nsoProperties;

    @Autowired
    private NsoQosSapPolicyIdDAO nsoQosSapPolicyIdDAO;


    @Transactional
    public void migrate(Long newScheduleId, Long oldScheduleId, Map<Long, Long> fixtureIdMap) {
        nsoQosSapPolicyIdDAO.findAllByScheduleId(oldScheduleId).forEach(qspId -> {
            qspId.setScheduleId(newScheduleId);
            // migrate fixture id
            qspId.setFixtureId(fixtureIdMap.get(qspId.getFixtureId()));
            nsoQosSapPolicyIdDAO.save(qspId);
        });
    }

    @Transactional
    public void findAndReserveQosSapPolicyIds(Connection conn, List<Schedule> schedules) throws NsoResvException {
        Map<String, Set<VlanFixture>> byDevice = new HashMap<>();
        conn.getReserved().getCmp().getJunctions().forEach(j -> byDevice.put(j.getDeviceUrn(), new HashSet<>()));
        for (VlanFixture f : conn.getReserved().getCmp().getFixtures()) {
            byDevice.get(f.getJunction().getDeviceUrn()).add(f);
        }

        for (String device : byDevice.keySet()) {
            this.findAndReserveQosSapPolicyIdsForDevice(device, byDevice.get(device), conn, schedules);
        }
    }

    public void findAndReserveQosSapPolicyIdsForDevice(String device, Set<VlanFixture> fixtures,
                                                       Connection conn, List<Schedule> schedules)
            throws NsoResvException {
        Set<Integer> availableSapQosIds = findUnusedQosSapPolicyIdKnowingSchedulesAndDevice(device, schedules);
        if (availableSapQosIds.size() < fixtures.size()) {
            throw new NsoResvException("not enough available sap qos ids");
        }

        for (VlanFixture f : fixtures) {
            Integer sapQosId = Collections.min(availableSapQosIds);
            availableSapQosIds.remove(sapQosId);

            NsoQosSapPolicyId policyId = NsoQosSapPolicyId.builder()
                    .fixtureId(f.getId())
                    .policyId(sapQosId)
                    .device(device)
                    .scheduleId(conn.getReserved().getSchedule().getId())
                    .connectionId(conn.getConnectionId())
                    .build();
            nsoQosSapPolicyIdDAO.save(policyId);
        }

    }

    public Set<Integer> findUnusedQosSapPolicyIdKnowingSchedulesAndDevice(String device, List<Schedule> schedules) {
        Set<Integer> usedSapQosIds = new HashSet<>();
        for (Schedule s : schedules) {
            usedSapQosIds.addAll(nsoQosSapPolicyIdDAO.findNsoQosSapPolicyIdByScheduleId(s.getId())
                    .stream()
                    .filter(nsoQosSapPolicyId -> nsoQosSapPolicyId.getDevice().equals(device))
                    .map(NsoQosSapPolicyId::getPolicyId)
                    .toList());
        }
        return availableFromUsedSetAndAllowedString(usedSapQosIds, nsoProperties.getSapQosIdRange());
    }


    @Transactional
    public void release(Connection conn) throws NsoResvException {
        log.info("releasing all SAP QoS policy id resources for " + conn.getConnectionId());
        List<NsoQosSapPolicyId> sapPolicyIds = nsoQosSapPolicyIdDAO.findNsoQosSapPolicyIdByConnectionId(conn.getConnectionId());
        nsoQosSapPolicyIdDAO.deleteAll(sapPolicyIds);
    }

}
