package net.es.oscars.web.rest;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.StartupException;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class MiscController {
    private final BuildProperties buildProperties;

    public MiscController(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }


    @ExceptionHandler(StartupException.class)
    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    public void handleStartup() {
        log.warn("Still in startup");
    }

    @RequestMapping(value = "/api/version", method = RequestMethod.GET)
    public String getVersion() {
        return "%s (%s)".formatted(buildProperties.getVersion(), buildProperties.getTime().toString());
    }

    @RequestMapping(value = "/api/ping", method = RequestMethod.GET)
    public String ping() {
        return "pong";
    }

    @RequestMapping(value = "/protected/ping", method = RequestMethod.GET)
    public String loggedInPing() {
        return "pong";
    }



}