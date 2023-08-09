package net.es.oscars.web.rest;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.app.util.GitRepositoryState;
import net.es.oscars.app.util.GitRepositoryStatePopulator;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;




@RestController
@Slf4j
public class MiscController {
    @Autowired
    private GitRepositoryStatePopulator gitRepositoryStatePopulator;


    @ExceptionHandler(StartupException.class)
    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    public void handleStartup(StartupException ex) {
        log.warn("Still in startup");
    }

    @RequestMapping(value = "/api/version", method = RequestMethod.GET)
    public String getVersion() {
        GitRepositoryState gitRepositoryState = this.gitRepositoryStatePopulator.getGitRepositoryState();

        return gitRepositoryState.getBuildVersion();
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