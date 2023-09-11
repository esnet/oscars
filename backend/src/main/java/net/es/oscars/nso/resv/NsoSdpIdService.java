package net.es.oscars.nso.resv;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.NsoProperties;
import net.es.oscars.nso.db.NsoSdpIdDAO;
import net.es.oscars.nso.ent.NsoSdpId;
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

    public void findAndReserveNsoSdpIds(Connection conn, List<Schedule> schedules) throws NsoResvException {
        // we want to use the same SDP id on the two devices at the end of each pipe.
        // this is a bit tricky
        // 1. we collect the set of available SDP ids on each involved device
        // 2. we iterate over the VlanPipes of the connection
        // 2.1 if the VlanPipe has `protect` set we will reserve two consecutive SDP ids for it; otherwise just one
        // 2.2. we inspect the two set of available SDP ids for the VlanPipe's A and Z and locate a shared SDP id
        // 2.3 we keep that around; when we get to the next VlanPipe these shall be considered in-use
        // 3. if we get through all the VlanPipes without any problems, we can save all the NsoSdpIds

        Map<String, Set<Integer>> usedSdpIdsByDevice = this.collectUsedSdpIdsKnowingSchedule(schedules);
        Set<Integer> allowedSdpIds = IntRange.singleSetFromExpr(nsoProperties.getSdpIdRange());

        Set<String> devices = new HashSet<>();
        for (VlanPipe pipe : conn.getReserved().getCmp().getPipes()) {
            devices.add(pipe.getA().getDeviceUrn());
            devices.add(pipe.getZ().getDeviceUrn());
        }

        Map<String, Set<Integer>> availableSdpIds = generateAvailableSdpIds(allowedSdpIds, usedSdpIdsByDevice, devices);

        Set<NsoSdpId> nsoSdpIds = new HashSet<>();

        for (VlanPipe pipe : conn.getReserved().getCmp().getPipes()) {
            Set<Integer> availableSdpIdsOnA = availableSdpIds.get(pipe.getA().getDeviceUrn());
            Set<Integer> availableSdpIdsOnZ = availableSdpIds.get(pipe.getZ().getDeviceUrn());
            boolean needTwoSdpIds = pipe.getProtect();
            Map<NsoVplsSdpPrecedence, Integer> sdpIdsByPrecedence = getCommonUnusedSdpIds(availableSdpIdsOnA, availableSdpIdsOnZ, needTwoSdpIds);
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

    public Map<String, Set<Integer>> collectUsedSdpIdsKnowingSchedule(List<Schedule> schedules) {
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

    public static Map<String, Set<Integer>> generateAvailableSdpIds(
            Set<Integer> allowed, Map<String, Set<Integer>> usedSdpIds, Set<String> devices) {

        Map<String, Set<Integer>> availableSdpIds = new HashMap<>();
        for (String device : devices) {
            Set<Integer> availableOnDevice = new HashSet<>(allowed);
            if (usedSdpIds.containsKey(device)) {
                availableOnDevice.removeAll(usedSdpIds.get(device));
            }
            availableSdpIds.put(device, availableOnDevice);
        }

        return availableSdpIds;
    }

    public static Map<NsoVplsSdpPrecedence, Integer> getCommonUnusedSdpIds(
            Set<Integer> availableSdpIdsOnA, Set<Integer> availableSdpIdsOnZ, boolean needTwoSdpIds)
            throws NsoResvException {

        Map<NsoVplsSdpPrecedence, Integer> result = new HashMap<NsoVplsSdpPrecedence, Integer>();
        if (needTwoSdpIds) {
            for (Integer sdpId : availableSdpIdsOnA.stream().sorted().toList()) {
                Integer nextSdpId = sdpId + 1;
                boolean nextIsAlsoAvailable = availableSdpIdsOnA.contains(nextSdpId) && availableSdpIdsOnZ.contains(nextSdpId);
                if (availableSdpIdsOnZ.contains(sdpId) && nextIsAlsoAvailable) {
                    result.put(NsoVplsSdpPrecedence.PRIMARY, sdpId);
                    result.put(NsoVplsSdpPrecedence.SECONDARY, nextSdpId);
                    return result;
                }
            }

        } else {
            for (Integer sdpId : availableSdpIdsOnA.stream().sorted().toList()) {
                if (availableSdpIdsOnZ.contains(sdpId)) {
                    result.put(NsoVplsSdpPrecedence.PRIMARY, sdpId);
                    return result;
                }
            }
        }
        throw new NsoResvException("unable to locate common unused SDP ID(s)");
    }

    @Transactional
    public void release(Connection conn) throws NsoResvException {
        log.info("releasing all NSO SDP id resources for " + conn.getConnectionId());
        List<NsoSdpId> sdpIds = nsoSdpIdDAO.findNsoSdpIdByConnectionId(conn.getConnectionId());
        nsoSdpIdDAO.deleteAll(sdpIds);
    }

}
