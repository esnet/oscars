package net.es.oscars.sb.nso.resv;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.sb.nso.db.NsoQosSapPolicyIdDAO;
import net.es.oscars.sb.nso.db.NsoSdpIdDAO;
import net.es.oscars.sb.nso.db.NsoVcIdDAO;
import net.es.oscars.sb.nso.ent.NsoQosSapPolicyId;
import net.es.oscars.sb.nso.ent.NsoSdpId;
import net.es.oscars.sb.nso.ent.NsoVcId;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.CommandParamIntent;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.topo.enums.CommandParamType;
import net.es.topo.common.dto.nso.enums.NsoVplsSdpPrecedence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class LegacyPopulator {
    @Autowired
    private ConnectionRepository cr;

    @Autowired
    private NsoQosSapPolicyIdDAO nsoQosSapPolicyIdDAO;

    @Autowired
    private NsoSdpIdDAO nsoSdpIdDAO;

    @Autowired
    private NsoVcIdDAO nsoVcIdDAO;


    public void importPssToNso() {
        // PSS to NSO resource mappings
        // ALU_SVC_ID           =>  nsoVcId
        // VC_ID                =>  nsoSdpId
        // ALU_QOS_POLICY_ID    =>  nsoQosSapPolicyId

        for (Connection c : cr.findByPhase(Phase.RESERVED)) {
            // We will use the existence of the nsoVcId to decide whether we need (and are able)
            // to migrate PSS resources to NSO ones

            if (nsoVcIdDAO.findNsoVcIdByConnectionId(c.getConnectionId()).isEmpty()) {
                // we haven't saved an nsoVcId yet.
                Long scheduleId = c.getReserved().getSchedule().getId();
                Components cmp = c.getReserved().getCmp();
                Set<Integer> aluSvcIds = new HashSet<>();
                List<NsoSdpId> nsoSdpIds = new ArrayList<>();
                List<NsoQosSapPolicyId> nsoQosSapPolicyIds = new ArrayList<>();

                // step 1: collect all
                for (VlanJunction j : cmp.getJunctions()) {
                    for (CommandParam cp : j.getCommandParams()) {
                        // we MUST have an ALU_SVC_ID, and we MUST have exactly one of them
                        if (cp.getParamType().equals(CommandParamType.ALU_SVC_ID)) {
                            aluSvcIds.add(cp.getResource());
                        }

                        // we might have VC_IDs; they get saved as nsoSdpIds
                        if (cp.getParamType().equals(CommandParamType.VC_ID)) {
                            NsoVplsSdpPrecedence precedence;;
                            if (cp.getIntent().equals(CommandParamIntent.PRIMARY)) {
                                precedence  = NsoVplsSdpPrecedence.PRIMARY;
                            } else {
                                precedence  = NsoVplsSdpPrecedence.SECONDARY;
                            }
                            nsoSdpIds.add(NsoSdpId.builder()
                                    .sdpId(cp.getResource())
                                    .scheduleId(scheduleId)
                                    .device(j.getDeviceUrn())
                                    .connectionId(c.getConnectionId())
                                    .precedence(precedence.toString())
                                    .target(cp.getTarget())
                                    .build());
                        }
                    }
                }

                // collect QOS policy ids from fixtures
                for (VlanFixture f : cmp.getFixtures()) {
                    for (CommandParam cp : f.getCommandParams()) {
                        if (cp.getParamType().equals(CommandParamType.ALU_QOS_POLICY_ID)) {
                            nsoQosSapPolicyIds.add(NsoQosSapPolicyId.builder()
                                    .fixtureId(f.getId())
                                    .policyId(cp.getResource())
                                    .device(f.getJunction().getDeviceUrn())
                                    .scheduleId(scheduleId)
                                    .connectionId(c.getConnectionId())
                                    .build());
                        }
                    }
                }

                // step 2: save all collected NSO resources
                if (aluSvcIds.size() == 1) {
                    for (int pssAluSvcId : aluSvcIds) {
                        nsoVcIdDAO.save(NsoVcId.builder()
                                .connectionId(c.getConnectionId())
                                .scheduleId(scheduleId)
                                .vcId(pssAluSvcId)
                                .build());
                    }
                    nsoSdpIdDAO.saveAll(nsoSdpIds);
                    nsoQosSapPolicyIdDAO.saveAll(nsoQosSapPolicyIds);
                } else {
                    log.error("multiple ALU svc-ids for "+c.getConnectionId()+"; skipping it.");
                }
            }
        }

    }
}
