package net.es.oscars.nsi.svc;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.nsi.db.NsiConnectionEventRepository;
import net.es.oscars.nsi.ent.NsiConnectionEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@Data
public class NsiConnectionEventService {

    private final NsiConnectionEventRepository eventRepo;
    public NsiConnectionEventService(NsiConnectionEventRepository eventRepo) {
        this.eventRepo = eventRepo;
    }

    @Transactional
    public void save(NsiConnectionEvent evt) {
        eventRepo.save(evt);
    }

}
