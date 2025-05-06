package net.es.oscars.nsi.svc;

import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.LifecycleStateEnumType;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ProvisionStateEnumType;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ReservationStateEnumType;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.NsiStateException;
import net.es.oscars.nsi.beans.NsiErrors;
import net.es.oscars.nsi.beans.NsiEvent;
import net.es.oscars.nsi.ent.NsiMapping;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class NsiStateEngine {

    public void reserve(NsiEvent event, NsiMapping mapping) throws NsiStateException {
        if (event.equals(NsiEvent.RESV_START)) {
            if (!mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_START)) {
                throw new NsiStateException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setReservationState(ReservationStateEnumType.RESERVE_START);

        } else if (event.equals(NsiEvent.RESV_CHECK)) {
            if (!mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_START)) {
                throw new NsiStateException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
            }

            mapping.setReservationState(ReservationStateEnumType.RESERVE_CHECKING);

        } else if (event.equals(NsiEvent.RESV_FL)) {
            if (!mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_CHECKING)) {
                throw new NsiStateException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
            }

            mapping.setReservationState(ReservationStateEnumType.RESERVE_FAILED);
        } else if (event.equals(NsiEvent.RESV_CF)) {
            if (!mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_CHECKING)) {
                throw new NsiStateException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setReservationState(ReservationStateEnumType.RESERVE_HELD);
        } else {
            throw new NsiStateException("Invalid event " + event, NsiErrors.TRANS_ERROR);
        }

    }


    public void provision(NsiEvent event, NsiMapping mapping) throws NsiStateException {
        // allowed transitions
        // null state -> provisioning via PROV_START
        // provisioned -> provisioning via PROV_START

        // provisioning -> provisioned via PROV_CF

        if (event.equals(NsiEvent.PROV_START)) {
            if (!mapping.getProvisionState().equals(ProvisionStateEnumType.RELEASED) && !mapping.getProvisionState().equals(ProvisionStateEnumType.PROVISIONED)) {
                throw new NsiStateException("Invalid prov state " + mapping.getProvisionState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setProvisionState(ProvisionStateEnumType.PROVISIONING);
        } else if (event.equals(NsiEvent.PROV_CF)) {
            if (!mapping.getProvisionState().equals(ProvisionStateEnumType.PROVISIONING)) {
                throw new NsiStateException("Invalid prov state " + mapping.getProvisionState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setProvisionState(ProvisionStateEnumType.PROVISIONED);

        } else {
            throw new NsiStateException("Invalid event " + event, NsiErrors.TRANS_ERROR);
        }
    }

    public void release(NsiEvent event, NsiMapping mapping) throws NsiStateException {
        if (event.equals(NsiEvent.REL_START)) {
            if (!mapping.getProvisionState().equals(ProvisionStateEnumType.PROVISIONED)) {
                throw new NsiStateException("Invalid prov state " + mapping.getProvisionState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setProvisionState(ProvisionStateEnumType.RELEASING);
        } else if (event.equals(NsiEvent.REL_CF)) {
            if (!mapping.getProvisionState().equals(ProvisionStateEnumType.RELEASING)) {
                throw new NsiStateException("Invalid prov state " + mapping.getProvisionState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setProvisionState(ProvisionStateEnumType.RELEASED);

        } else {
            throw new NsiStateException("Invalid event " + event, NsiErrors.TRANS_ERROR);
        }
    }


    public void resvTimedOut(NsiMapping mapping) throws NsiStateException {
        Set<ReservationStateEnumType> allowedStates = new HashSet<>();
        allowedStates.add(ReservationStateEnumType.RESERVE_HELD);
        allowedStates.add(ReservationStateEnumType.RESERVE_CHECKING);
        allowedStates.add(ReservationStateEnumType.RESERVE_ABORTING);
        allowedStates.add(ReservationStateEnumType.RESERVE_COMMITTING);

        if (!allowedStates.contains(mapping.getReservationState())) {
            throw new NsiStateException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
        }
        mapping.setReservationState(ReservationStateEnumType.RESERVE_TIMEOUT);
    }

    public void termStart(NsiMapping mapping) throws NsiStateException {
        Set<LifecycleStateEnumType> allowedStates = new HashSet<>();
        allowedStates.add(LifecycleStateEnumType.CREATED);
        allowedStates.add(LifecycleStateEnumType.PASSED_END_TIME);
        allowedStates.add(LifecycleStateEnumType.FAILED);

        if (!allowedStates.contains(mapping.getLifecycleState())) {
            throw new NsiStateException("Invalid lifecycle state " + mapping.getLifecycleState(), NsiErrors.TRANS_ERROR);
        }
        mapping.setLifecycleState(LifecycleStateEnumType.TERMINATING);
    }

    public void termConfirm(NsiMapping mapping) throws NsiStateException {
        Set<LifecycleStateEnumType> allowedStates = new HashSet<>();
        allowedStates.add(LifecycleStateEnumType.TERMINATING);

        if (!allowedStates.contains(mapping.getLifecycleState())) {
            throw new NsiStateException("Invalid lifecycle state " + mapping.getLifecycleState(), NsiErrors.TRANS_ERROR);
        }
        mapping.setLifecycleState(LifecycleStateEnumType.TERMINATED);
    }


    public void pastEndTime(NsiMapping mapping) throws NsiStateException {
        Set<LifecycleStateEnumType> allowedStates = new HashSet<>();
        allowedStates.add(LifecycleStateEnumType.CREATED);
        if (!allowedStates.contains(mapping.getLifecycleState())) {
            throw new NsiStateException("Invalid lifecycle state " + mapping.getLifecycleState(), NsiErrors.TRANS_ERROR);
        }
        mapping.setLifecycleState(LifecycleStateEnumType.PASSED_END_TIME);
    }

    public void forcedEnd(NsiMapping mapping) throws NsiStateException {
        Set<LifecycleStateEnumType> allowedStates = new HashSet<>();
        allowedStates.add(LifecycleStateEnumType.CREATED);
        allowedStates.add(LifecycleStateEnumType.FAILED);
        if (!allowedStates.contains(mapping.getLifecycleState())) {
            throw new NsiStateException("Invalid lifecycle state " + mapping.getLifecycleState(), NsiErrors.TRANS_ERROR);
        }
        mapping.setLifecycleState(LifecycleStateEnumType.TERMINATED);
    }

    public void commit(NsiEvent event, NsiMapping mapping) throws NsiStateException {


        if (event.equals(NsiEvent.COMMIT_START)) {
            if (!mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_HELD)) {
                throw new NsiStateException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setReservationState(ReservationStateEnumType.RESERVE_COMMITTING);
        } else if (event.equals(NsiEvent.COMMIT_FL)) {
            if (!mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_COMMITTING)) {
                throw new NsiStateException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setReservationState(ReservationStateEnumType.RESERVE_START);
        } else if (event.equals(NsiEvent.COMMIT_CF)) {
            if (!mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_COMMITTING)) {
                throw new NsiStateException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setReservationState(ReservationStateEnumType.RESERVE_START);
        } else {
            throw new NsiStateException("Invalid event " + event, NsiErrors.TRANS_ERROR);
        }
    }

    public void abort(NsiEvent event, NsiMapping mapping) throws NsiStateException {

        if (event.equals(NsiEvent.ABORT_START)) {
            if (!mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_HELD) && !mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_FAILED)) {
                throw new NsiStateException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setReservationState(ReservationStateEnumType.RESERVE_ABORTING);

        } else if (event.equals(NsiEvent.ABORT_CF)) {
            if (!mapping.getReservationState().equals(ReservationStateEnumType.RESERVE_ABORTING)) {
                throw new NsiStateException("Invalid reservation state " + mapping.getReservationState(), NsiErrors.TRANS_ERROR);
            }
            mapping.setReservationState(ReservationStateEnumType.RESERVE_START);

        } else {
            throw new NsiStateException("Invalid event " + event, NsiErrors.TRANS_ERROR);
        }
    }
}
