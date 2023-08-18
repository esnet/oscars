package net.es.oscars.nsi.svc;

import gen.nsi_2_0.connection.types.LifecycleStateEnumType;
import gen.nsi_2_0.connection.types.ProvisionStateEnumType;
import gen.nsi_2_0.connection.types.ReservationStateEnumType;
import lombok.extern.slf4j.Slf4j;
import gen.nsi_2_0.connection.ifce.ServiceException;
import net.es.oscars.app.exc.NsiException;
import net.es.oscars.nsi.beans.NsiErrors;
import net.es.oscars.nsi.beans.NsiEvent;
import net.es.oscars.nsi.db.NsiMappingRepository;
import net.es.oscars.nsi.ent.NsiMapping;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.resv.svc.ConnUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class NsiStateEngine {
    @Autowired
    private NsiMappingRepository nsiRepo;

    @Autowired
    private ConnUtils connUtils;


    public NsiMapping newMapping(String nsiConnectionId, String nsiGri, String nsaId, Integer version) throws ServiceException {
        if (nsiConnectionId == null || nsiConnectionId.equals("")) {
            throw new ServiceException("null nsi connection id");
        }
        if (nsiGri == null) {
            nsiGri = "";
        }
        if (!nsiRepo.findByNsiConnectionId(nsiConnectionId).isEmpty()) {
            throw new ServiceException("previously used nsi connection id! " + nsiConnectionId);
        }
        String oscarsConnectionId = connUtils.genUniqueConnectionId();

        NsiMapping mapping = NsiMapping.builder()
                .nsiConnectionId(nsiConnectionId)
                .nsiGri(nsiGri)
                .oscarsConnectionId(oscarsConnectionId)
                .dataplaneVersion(version)
                .nsaId(nsaId)
                .lifecycleState(LifecycleStateEnumType.CREATED)
                .provisionState(ProvisionStateEnumType.RELEASED)
                .reservationState(ReservationStateEnumType.RESERVE_START)
                .build();
        log.info("added an NSI mapping: "+nsiConnectionId+" --> "+oscarsConnectionId);
        nsiRepo.save(mapping);
        return mapping;
    }

    public void reserve(NsiEvent event, NsiMapping mapping) throws NsiException {


        if (event.equals(NsiEvent.RESV_START)) {
            if (!mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_START)) {
                throw new NsiException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setReservationState(ReservationStateEnumType.RESERVE_CHECKING);

        } else if (event.equals(NsiEvent.RESV_FL)) {
            if (!mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_CHECKING)) {
                throw new NsiException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
            }

            mapping.setReservationState(ReservationStateEnumType.RESERVE_FAILED);
        } else if (event.equals(NsiEvent.RESV_CF)) {
            if (!mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_CHECKING)) {
                throw new NsiException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setReservationState(ReservationStateEnumType.RESERVE_HELD);
        } else {
            throw new NsiException("Invalid event " + event, NsiErrors.TRANS_ERROR);
        }
        nsiRepo.save(mapping);

    }


    public void provision(NsiEvent event, NsiMapping mapping) throws NsiException {
        if (event.equals(NsiEvent.PROV_START)) {
            if (!mapping.getProvisionState().equals(ProvisionStateEnumType.RELEASED)) {
                throw new NsiException("Invalid prov state " + mapping.getProvisionState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setProvisionState(ProvisionStateEnumType.PROVISIONING);
        } else if (event.equals(NsiEvent.PROV_CF)) {
            if (!mapping.getProvisionState().equals(ProvisionStateEnumType.PROVISIONING)) {
                throw new NsiException("Invalid prov state " + mapping.getProvisionState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setProvisionState(ProvisionStateEnumType.PROVISIONED);

        } else {
            throw new NsiException("Invalid event " + event, NsiErrors.TRANS_ERROR);
        }
        nsiRepo.save(mapping);

    }

    public void release(NsiEvent event, NsiMapping mapping) throws NsiException {
        if (event.equals(NsiEvent.REL_START)) {
            if (!mapping.getProvisionState().equals(ProvisionStateEnumType.PROVISIONED)) {
                throw new NsiException("Invalid prov state " + mapping.getProvisionState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setProvisionState(ProvisionStateEnumType.RELEASING);
        } else if (event.equals(NsiEvent.REL_CF)) {
            if (!mapping.getProvisionState().equals(ProvisionStateEnumType.RELEASING)) {
                throw new NsiException("Invalid prov state " + mapping.getProvisionState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setProvisionState(ProvisionStateEnumType.RELEASED);

        } else {
            throw new NsiException("Invalid event " + event, NsiErrors.TRANS_ERROR);
        }
        nsiRepo.save(mapping);

    }


    public void resvTimedOut(NsiMapping mapping) throws NsiException {

        if (!mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_HELD)) {
            throw new NsiException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
        }
        mapping.setReservationState(ReservationStateEnumType.RESERVE_TIMEOUT);
        nsiRepo.save(mapping);
    }

    public void termStart(NsiMapping mapping) throws NsiException {
        Set<LifecycleStateEnumType> allowedStates = new HashSet<>();
        allowedStates.add(LifecycleStateEnumType.CREATED);
        allowedStates.add(LifecycleStateEnumType.PASSED_END_TIME);
        allowedStates.add(LifecycleStateEnumType.FAILED);

        if (!allowedStates.contains(mapping.getLifecycleState())) {
            throw new NsiException("Invalid lifecycle state " + mapping.getLifecycleState(), NsiErrors.TRANS_ERROR);
        }
        mapping.setLifecycleState(LifecycleStateEnumType.TERMINATING);
        nsiRepo.save(mapping);
    }

    public void termConfirm(NsiMapping mapping) throws NsiException {
        Set<LifecycleStateEnumType> allowedStates = new HashSet<>();
        allowedStates.add(LifecycleStateEnumType.TERMINATING);

        if (!allowedStates.contains(mapping.getLifecycleState())) {
            throw new NsiException("Invalid lifecycle state " + mapping.getLifecycleState(), NsiErrors.TRANS_ERROR);
        }
        mapping.setLifecycleState(LifecycleStateEnumType.TERMINATED);

        nsiRepo.save(mapping);

    }


    public void pastEndTime(NsiMapping mapping) throws NsiException {
        Set<LifecycleStateEnumType> allowedStates = new HashSet<>();
        allowedStates.add(LifecycleStateEnumType.CREATED);
        if (!allowedStates.contains(mapping.getLifecycleState())) {
            throw new NsiException("Invalid lifecycle state " + mapping.getLifecycleState(), NsiErrors.TRANS_ERROR);
        }
        mapping.setLifecycleState(LifecycleStateEnumType.PASSED_END_TIME);
        nsiRepo.save(mapping);

    }

    public void forcedEnd(NsiMapping mapping) throws NsiException {
        Set<LifecycleStateEnumType> allowedStates = new HashSet<>();
        allowedStates.add(LifecycleStateEnumType.CREATED);
        if (!allowedStates.contains(mapping.getLifecycleState())) {
            throw new NsiException("Invalid lifecycle state " + mapping.getLifecycleState(), NsiErrors.TRANS_ERROR);
        }
        mapping.setLifecycleState(LifecycleStateEnumType.FAILED);
        nsiRepo.save(mapping);

    }

    public void commit(NsiEvent event, NsiMapping mapping) throws NsiException {


        if (event.equals(NsiEvent.COMMIT_START)) {
            if (!mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_HELD)) {
                throw new NsiException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setReservationState(ReservationStateEnumType.RESERVE_COMMITTING);
        } else if (event.equals(NsiEvent.COMMIT_FL)) {
            if (!mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_COMMITTING)) {
                throw new NsiException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setReservationState(ReservationStateEnumType.RESERVE_START);
        } else if (event.equals(NsiEvent.COMMIT_CF)) {
            if (!mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_COMMITTING)) {
                throw new NsiException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setReservationState(ReservationStateEnumType.RESERVE_START);
        } else {
            throw new NsiException("Invalid event " + event, NsiErrors.TRANS_ERROR);
        }
        nsiRepo.save(mapping);
    }

    public void abort(NsiEvent event, NsiMapping mapping) throws NsiException {

        if (event.equals(NsiEvent.ABORT_START)) {
            if (!mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_HELD)) {
                throw new NsiException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setReservationState(ReservationStateEnumType.RESERVE_ABORTING);

        } else if (event.equals(NsiEvent.ABORT_CF)) {
            if (!mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_ABORTING)) {
                throw new NsiException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setReservationState(ReservationStateEnumType.RESERVE_START);

        } else {
            throw new NsiException("Invalid event " + event, NsiErrors.TRANS_ERROR);
        }
        nsiRepo.save(mapping);

    }
}
