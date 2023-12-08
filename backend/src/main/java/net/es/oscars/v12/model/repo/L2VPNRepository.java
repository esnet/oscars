package net.es.oscars.v12.model.repo;

import net.es.oscars.v12.model.L2VPN;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface L2VPNRepository extends JpaRepository<L2VPN, Long> {

    Optional<L2VPN> findByConnectionId(String connectionId);


}