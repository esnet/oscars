package net.es.oscars.sb;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.dto.pss.cmd.GeneratedCommands;
import net.es.oscars.sb.db.RouterCommandsRepository;
import net.es.oscars.sb.ent.RouterCommands;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service
@Transactional
@Slf4j
public class RouterCommandsService {
    private RouterCommandsRepository rcRepo;

    @Autowired
    public RouterCommandsService(RouterCommandsRepository rcRepo) {
        this.rcRepo = rcRepo;
    }


    public GeneratedCommands commands(String connectionId, String deviceUrn) {
        log.info("retrieving commands for " + connectionId + " " + deviceUrn);


        GeneratedCommands gc = GeneratedCommands.builder()
                .generated(new HashMap<>())
                .build();

        List<RouterCommands> rcs = rcRepo.findByConnectionIdAndDeviceUrn(connectionId, deviceUrn);
        for (RouterCommands rc : rcs) {
            gc.getGenerated().put(rc.getType(), rc.getContents());
        }
        return gc;
    }

}