package net.es.oscars.sb.nso.resv;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.resv.enums.DeploymentIntent;
import net.es.oscars.resv.enums.DeploymentState;
import net.es.oscars.sb.nso.db.*;
import net.es.oscars.sb.nso.ent.NsoQosSapPolicyId;
import net.es.oscars.sb.nso.ent.NsoSdpId;
import net.es.oscars.sb.nso.ent.NsoSdpVcId;
import net.es.oscars.sb.nso.ent.NsoVcId;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.ent.*;
import net.es.oscars.resv.enums.CommandParamIntent;
import net.es.oscars.resv.enums.Phase;
import net.es.oscars.topo.enums.CommandParamType;
import net.es.topo.common.dto.nso.enums.NsoVplsSdpPrecedence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static net.es.topo.common.devel.DevelUtils.dumpDebug;

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
    @Autowired
    private NsoSdpVcIdDAO nsoSdpVcIdDAO;


    @Transactional
    public void importPssToNso() {
        // PSS to NSO resource mappings
        // ALU_SVC_ID           =>  nsoVcId
        // VC_ID                =>  nsoSdpId
        // ALU_QOS_POLICY_ID    =>  nsoQosSapPolicyId

        for (Connection c : cr.findByPhase(Phase.RESERVED)) {
            // We will use the existence of the nsoVcId to decide whether we need (and are able)
            // to migrate PSS resources to NSO ones

            if (nsoVcIdDAO.findNsoVcIdByConnectionId(c.getConnectionId()).isEmpty()) {
                log.info("migrating legacy connection "+c.getConnectionId());

                // this is a legacy connection that has not been migrated yet
                c.setDeploymentState(DeploymentState.DEPLOYED);
                c.setDeploymentIntent(DeploymentIntent.SHOULD_BE_DEPLOYED);
                cr.save(c);

                // we haven't saved an nsoVcId yet.
                Long scheduleId = c.getReserved().getSchedule().getId();
                Components cmp = c.getReserved().getCmp();
                Set<Integer> aluSvcIds = new HashSet<>();
                List<NsoSdpId> nsoSdpIds = new ArrayList<>();
                List<NsoQosSapPolicyId> nsoQosSapPolicyIds = new ArrayList<>();
                List<NsoSdpVcId> nsoSdpVcIds = new ArrayList<>();

                // step 1: collect all
                for (VlanJunction j : cmp.getJunctions()) {
                    // first, find the SVC ID then all the SDP ids

                    Map<String, Map<NsoVplsSdpPrecedence, Integer>> sdpByTarget = new HashMap<>();
                    for (CommandParam cp : j.getCommandParams()) {
                        // we MUST have an ALU_SVC_ID, and we MUST have exactly one of them
                        if (cp.getParamType().equals(CommandParamType.ALU_SVC_ID)) {
                            aluSvcIds.add(cp.getResource());

                            } else if (cp.getParamType().equals(CommandParamType.ALU_SDP_ID)) {
                            // collect SDP ids and match them to target & precedence
                            NsoVplsSdpPrecedence precedence;;
                            if (cp.getIntent().equals(CommandParamIntent.PRIMARY)) {
                                precedence  = NsoVplsSdpPrecedence.PRIMARY;
                            } else {
                                precedence  = NsoVplsSdpPrecedence.SECONDARY;
                            }

                            if (!sdpByTarget.containsKey(cp.getTarget())) {
                                sdpByTarget.put(cp.getTarget(), new HashMap<>());
                            }
                            sdpByTarget.get(cp.getTarget()).put(precedence, cp.getResource());

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
                    dumpDebug("SDPs by Target "+c.getConnectionId()+" "+j.getDeviceUrn(), sdpByTarget);

                    // loop over these again and match vc-ids to sdp-ids
                    // unfortunately we do not actually have target info since OSCARS 1.0 did not save that
                    // we will assume there's only one target
                    for (CommandParam cp : j.getCommandParams()) {
                        if (cp.getParamType().equals(CommandParamType.VC_ID)) {
                            NsoVplsSdpPrecedence precedence;
                            if (cp.getIntent().equals(CommandParamIntent.PRIMARY)) {
                                precedence = NsoVplsSdpPrecedence.PRIMARY;
                            } else {
                                precedence = NsoVplsSdpPrecedence.SECONDARY;
                            }
                            if (sdpByTarget.keySet().size() > 1) {
                                log.error("unable to migrate multipoint SDP ids ");
                                dumpDebug(c.getConnectionId(), sdpByTarget);
                            } else if (sdpByTarget.keySet().isEmpty()) {
                                log.info("no SDPs to migrate");
                            } else {
                                String target = sdpByTarget.keySet().iterator().next();
                                Integer sdpId = sdpByTarget.get(target).get(precedence);
                                nsoSdpVcIds.add(NsoSdpVcId.builder()
                                        .vcId(cp.getResource())
                                        .scheduleId(scheduleId)
                                        .device(j.getDeviceUrn())
                                        .connectionId(c.getConnectionId())
                                        .sdpId(sdpId)
                                        .build());

                            }
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
                        log.info(c.getConnectionId()+" ALU service id: "+pssAluSvcId);
                    }
                    nsoSdpIdDAO.saveAll(nsoSdpIds);
                    nsoQosSapPolicyIdDAO.saveAll(nsoQosSapPolicyIds);
                    nsoSdpVcIdDAO.saveAll(nsoSdpVcIds);
                    dumpDebug("migrate SDPs "+c.getConnectionId(), nsoSdpIds);
                    dumpDebug("migrate QoS SAP "+c.getConnectionId(), nsoQosSapPolicyIds);
                    dumpDebug("migrate SDP VC ids "+c.getConnectionId(), nsoSdpVcIds);
                } else {
                    log.error("multiple ALU svc-ids for "+c.getConnectionId()+"; skipping it.");
                }
            }
        }

    }
}
