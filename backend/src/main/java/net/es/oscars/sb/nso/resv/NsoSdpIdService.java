package net.es.oscars.sb.nso.resv;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.NsoProperties;
import net.es.oscars.sb.MiscHelper;
import net.es.oscars.sb.nso.db.NsoSdpIdDAO;
import net.es.oscars.sb.nso.ent.NsoSdpId;
import net.es.oscars.resv.ent.*;
import net.es.oscars.topo.beans.IntRange;
import net.es.topo.common.dto.nso.enums.NsoVplsSdpPrecedence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class NsoSdpIdService {
    @Autowired
    private NsoProperties nsoProperties;

    @Autowired
    private NsoSdpIdDAO nsoSdpIdDAO;

    @Transactional
    public void findAndReserveNsoSdpIds(Connection conn, List<Schedule> schedules) throws NsoResvException {
        // we want to use the same SDP id on the two devices at the end of each pipe.
        // we don't technically _need_ to match these, but it's nice
        // this is a bit tricky
        // 1. we collect the set of available SDP ids on each involved device
        // 2. we iterate over the VlanPipes of the connection
        // 2.1 if the VlanPipe has `protect` set we will reserve two consecutive SDP ids for it; otherwise just one
        // 2.2. we inspect the two set of available SDP ids for the VlanPipe's A and Z and locate a shared SDP id
        // 2.3 we keep that around; when we get to the next VlanPipe these shall be considered in-use
        // 3. if we get through all the VlanPipes without any problems, we can save all the NsoSdpIds
        Set<Integer> allowedSdpIds = IntRange.singleSetFromExpr(nsoProperties.getSdpIdRange());


        Map<String, Set<Integer>> usedSdpIdsByDevice;
        Set<String> devices = new HashSet<>();
        for (VlanPipe pipe : conn.getReserved().getCmp().getPipes()) {
            devices.add(pipe.getA().getDeviceUrn());
            devices.add(pipe.getZ().getDeviceUrn());
        }

        if (nsoProperties.getSdpIdsGloballyUnique()) {
            // in this case we consider all sdp ids already reserved anywhere to be reserved on our devices
            Set<Integer> usedSdpIds  = this.collectUsedSdpIdsGloballyKnowingSchedule(schedules);
            usedSdpIdsByDevice = new HashMap<>();
            devices.forEach(d -> {
                usedSdpIdsByDevice.put(d, usedSdpIds);
            });

        } else {
            usedSdpIdsByDevice = this.collectUsedSdpIdsByDeviceKnowingSchedule(schedules);
        }

        Map<String, Set<Integer>> availableSdpIds = MiscHelper.generateAvailableDeviceScopedIdentifiers(allowedSdpIds, usedSdpIdsByDevice, devices);
        Set<NsoSdpId> nsoSdpIds = new HashSet<>();

        for (VlanPipe pipe : conn.getReserved().getCmp().getPipes()) {
            Set<Integer> availableSdpIdsOnA = availableSdpIds.get(pipe.getA().getDeviceUrn());
            Set<Integer> availableSdpIdsOnZ = availableSdpIds.get(pipe.getZ().getDeviceUrn());
            boolean needTwoSdpIds = pipe.getProtect();
            Map<NsoVplsSdpPrecedence, Integer> sdpIdsByPrecedence = MiscHelper
                    .getUnusedIntResourceFromTwoSets(availableSdpIdsOnA, availableSdpIdsOnZ, needTwoSdpIds);
            for (NsoVplsSdpPrecedence precedence : sdpIdsByPrecedence.keySet()) {
                Integer sdpId = sdpIdsByPrecedence.get(precedence);
                availableSdpIds.get(pipe.getA().getDeviceUrn()).remove(sdpId);
                availableSdpIds.get(pipe.getZ().getDeviceUrn()).remove(sdpId);
                nsoSdpIds.add(NsoSdpId.builder()
                        .connectionId(conn.getConnectionId())
                        .scheduleId(conn.getReserved().getSchedule().getId())
                        .device(pipe.getA().getDeviceUrn())
                        .target(pipe.getZ().getDeviceUrn())
                        .sdpId(sdpId)
                        .precedence(precedence.toString())
                        .build());
                nsoSdpIds.add(NsoSdpId.builder()
                        .connectionId(conn.getConnectionId())
                        .scheduleId(conn.getReserved().getSchedule().getId())
                        .device(pipe.getZ().getDeviceUrn())
                        .target(pipe.getA().getDeviceUrn())
                        .sdpId(sdpId)
                        .precedence(precedence.toString())
                        .build());
            }
        }
        this.nsoSdpIdDAO.saveAll(nsoSdpIds);
    }

    public Set<Integer> collectUsedSdpIdsGloballyKnowingSchedule(List<Schedule> schedules) {
        Set<Integer> usedSdpIds = new HashSet<>();
        schedules.forEach(s -> {
            nsoSdpIdDAO.findNsoSdpIdByScheduleId(s.getId()).forEach(nsoSdpId -> {
                usedSdpIds.add(nsoSdpId.getSdpId());
            });
        });

        return usedSdpIds;
    }

    public Map<String, Set<Integer>> collectUsedSdpIdsByDeviceKnowingSchedule(List<Schedule> schedules) {
        Map<String, Set<Integer>> usedSdpIds = new HashMap<>();
        schedules.forEach(s -> {
            nsoSdpIdDAO.findNsoSdpIdByScheduleId(s.getId()).forEach(nsoSdpId -> {
                if (!usedSdpIds.containsKey(nsoSdpId.getDevice())) {
                    usedSdpIds.put(nsoSdpId.getDevice(), new HashSet<>());
                }
                usedSdpIds.get(nsoSdpId.getDevice()).add(nsoSdpId.getSdpId());
            });
        });
        return usedSdpIds;
    }

    @Transactional
    public void release(Connection conn) throws NsoResvException {
        log.info("releasing all NSO SDP id resources for " + conn.getConnectionId());
        List<NsoSdpId> sdpIds = nsoSdpIdDAO.findNsoSdpIdByConnectionId(conn.getConnectionId());
        nsoSdpIdDAO.deleteAll(sdpIds);
    }

}
