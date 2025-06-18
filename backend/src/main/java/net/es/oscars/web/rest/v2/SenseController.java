package net.es.oscars.web.rest.v2;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.nsi.db.NsiConnectionEventRepository;
import net.es.oscars.nsi.ent.NsiConnectionEvent;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@Slf4j
public class SenseController {
    private final Startup startup;
    private final NsiConnectionEventRepository eventRepo;

    public SenseController(Startup startup, NsiConnectionEventRepository eventRepo) {
        this.startup = startup;
        this.eventRepo = eventRepo;
    }

    @ExceptionHandler(StartupException.class)
    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    public void handleException() {
        log.warn("Still in startup");
    }

    @RequestMapping(value = "/api/sense/events/{nsiConnectionId:.+}", method = RequestMethod.GET)
    @ResponseBody
    @Transactional
    public List<NsiConnectionEvent> getSenseEvents(@PathVariable String nsiConnectionId)
            throws StartupException {
        startup.startupCheck();

        log.info("getSenseEvents nsiConnectionId=" + nsiConnectionId);
        return eventRepo.findByNsiConnectionId(nsiConnectionId);
    }

}
