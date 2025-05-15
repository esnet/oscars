package net.es.oscars.web.rest;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.resv.db.LogRepository;
import net.es.oscars.resv.ent.EventLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;


@RestController
@Slf4j
public class EventLogController {

    @Autowired
    private LogRepository logRepo;

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public void handleResourceNotFoundException(NoSuchElementException ex) {
        log.warn("requested an item which did not exist", ex);
    }


    @RequestMapping(value = "/api/log/conn/{connectionId:.+}", method = RequestMethod.GET)
    @ResponseBody
    public EventLog eventLog(@PathVariable String connectionId) {
        if (connectionId == null || connectionId.isEmpty()) {
            return null;
        }

        return logRepo.findByConnectionId(connectionId).orElse(null);
    }


}