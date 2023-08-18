package net.es.oscars.resv.db;

import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.enums.DeploymentIntent;
import net.es.oscars.resv.enums.DeploymentState;
import net.es.oscars.resv.enums.Phase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    List<Connection> findAll();
    Optional<Connection> findByConnectionId(String connectionId);
    List<Connection> findByDeploymentIntentAndDeploymentState(DeploymentIntent intent, DeploymentState state);

    List<Connection> findByPhase(Phase phase);
    List<Connection> findByPhaseIn(List<Phase> phases);

}