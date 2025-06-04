package net.es.oscars.nsi.db;

import lombok.NonNull;
import net.es.oscars.nsi.beans.NsiNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface NsiNotificationRepository extends JpaRepository<NsiNotification, Long> {

    @NonNull
    List<NsiNotification> findAll();

    List<NsiNotification> findByConnectionId(String connectionId);

}