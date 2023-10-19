package net.es.oscars.sb.nso.resv;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.NsoProperties;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.ent.Schedule;
import net.es.oscars.resv.ent.VlanPipe;
import net.es.oscars.sb.MiscHelper;
import net.es.oscars.sb.nso.db.NsoSdpIdDAO;
import net.es.oscars.sb.nso.db.NsoSdpVcIdDAO;
import net.es.oscars.sb.nso.ent.NsoSdpId;
import net.es.oscars.sb.nso.ent.NsoSdpVcId;
import net.es.oscars.topo.beans.IntRange;
import net.es.topo.common.dto.nso.enums.NsoVplsSdpPrecedence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class NsoSdpVcIdService {
    @Autowired
    private NsoProperties nsoProperties;

    @Autowired
    private NsoSdpVcIdDAO nsoSdpVcIdDAO;

    @Autowired
    private NsoSdpIdDAO nsoSdpIdDAO;

    public void findAndReserveNsoSdpVcIds(Connection conn, List<Schedule> schedules) throws NsoResvException {
        // we use the same VC-id on the spoke-sdps at the end of each pipe.
        // this is similar to the SDP id but this time we NEED to match the vc-id values

        // we COULD use the sdp-id values as the vc-ids but these are conceptually
        // different resources. and, it is possible that we will want to use mesh-sdps
        // at some point; if we do, the sdp-id logic will diverge from the vc-id logic

        // 1. we collect the set of available vc-ids on each involved device
        // 2. we iterate over the VlanPipes of the connection
        // 2.1 if the VlanPipe has `protect` set we will reserve two consecutive vc-ids for it; otherwise just one
        // 2.2. we inspect the two sets of available vc-ids for the VlanPipe's A and Z and locate an unused vc-id
        // 2.3 we keep that around; when we get to the next VlanPipe these shall be considered in-use
        // 3. if we get through all the VlanPipes without any problems, we can save all the NsoSdpVcIds

        Map<String, Set<Integer>> usedSdpVcIdsByDevice = this.collectUsedSdpVcIdsKnowingSchedule(schedules);

        Set<Integer> allowedSdpVcIds = IntRange.singleSetFromExpr(nsoProperties.getVcIdRange());

        Set<String> devices = new HashSet<>();
        for (VlanPipe pipe : conn.getReserved().getCmp().getPipes()) {
            devices.add(pipe.getA().getDeviceUrn());
            devices.add(pipe.getZ().getDeviceUrn());
        }

        Map<String, Set<Integer>> availableSdpVcIds = MiscHelper
                .generateAvailableDeviceScopedIdentifiers(allowedSdpVcIds, usedSdpVcIdsByDevice, devices);

        List<NsoSdpId> nsoSdpIds = nsoSdpIdDAO.findNsoSdpIdByConnectionId(conn.getConnectionId());

        Set<NsoSdpVcId> nsoSdpVcIds = new HashSet<>();
        for (VlanPipe pipe : conn.getReserved().getCmp().getPipes()) {

            boolean needTwoIds = pipe.getProtect();
            Set<Integer> availableSdpVcIdsOnA = availableSdpVcIds.get(pipe.getA().getDeviceUrn());
            Set<Integer> availableSdpVcIdsOnZ = availableSdpVcIds.get(pipe.getZ().getDeviceUrn());
            Map<NsoVplsSdpPrecedence, Integer> sdpVcIdsByPrecedence = MiscHelper
                    .getUnusedIntResourceFromTwoSets(availableSdpVcIdsOnA, availableSdpVcIdsOnZ, needTwoIds);

            for (NsoVplsSdpPrecedence precedence : sdpVcIdsByPrecedence.keySet()) {
                Integer sdpVcId = sdpVcIdsByPrecedence.get(precedence);
                availableSdpVcIds.get(pipe.getA().getDeviceUrn()).remove(sdpVcId);
                availableSdpVcIds.get(pipe.getZ().getDeviceUrn()).remove(sdpVcId);
                Integer sdpId = null;

                // we should have reserved an sdp id for pipe / precedence combination
                for (NsoSdpId nsoSdpId : nsoSdpIds) {
                    if (nsoSdpId.getDevice().equals(pipe.getA().getDeviceUrn()) &&
                        nsoSdpId.getTarget().equals(pipe.getZ().getDeviceUrn()) &&
                        nsoSdpId.getPrecedence().equals(precedence.toString())) {
                        sdpId = nsoSdpId.getSdpId();
                    }
                }
                if (sdpId == null) {
                    throw new NsoResvException("unable to locate reserved sdp id");
                }

                nsoSdpVcIds.add(NsoSdpVcId.builder()
                        .connectionId(conn.getConnectionId())
                        .scheduleId(conn.getReserved().getSchedule().getId())
                        .device(pipe.getA().getDeviceUrn())
                        .sdpId(sdpId)
                        .vcId(sdpVcId)
                        .build());
                nsoSdpVcIds.add(NsoSdpVcId.builder()
                        .connectionId(conn.getConnectionId())
                        .scheduleId(conn.getReserved().getSchedule().getId())
                        .device(pipe.getZ().getDeviceUrn())
                        .sdpId(sdpId)
                        .vcId(sdpVcId)
                        .build());
            }
        }
        this.nsoSdpVcIdDAO.saveAll(nsoSdpVcIds);
    }

    public Map<String, Set<Integer>> collectUsedSdpVcIdsKnowingSchedule(List<Schedule> schedules) {
        Map<String, Set<Integer>> usedSdpVcIds = new HashMap<>();
        schedules.forEach(s -> {
            nsoSdpVcIdDAO.findNsoSdpVcIdByScheduleId(s.getId()).forEach(nsoSdpVcId -> {
                if (!usedSdpVcIds.containsKey(nsoSdpVcId.getDevice())) {
                    usedSdpVcIds.put(nsoSdpVcId.getDevice(), new HashSet<>());
                }
                usedSdpVcIds.get(nsoSdpVcId.getDevice()).add(nsoSdpVcId.getVcId());
            });
        });
        return usedSdpVcIds;
    }




    @Transactional
    public void release(Connection conn) throws NsoResvException {
        log.info("releasing all NSO SDP VC id resources for " + conn.getConnectionId());
        List<NsoSdpVcId> sdpVcIds = nsoSdpVcIdDAO.findNsoSdpVcIdByConnectionId(conn.getConnectionId());
        nsoSdpVcIdDAO.deleteAll(sdpVcIds);
    }

}
