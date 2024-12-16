package net.es.oscars.web.rest.v2;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.svc.ResvService;

import net.es.oscars.topo.pop.ConsistencyException;
;
import net.es.oscars.web.beans.v2.L2VPN;
import net.es.oscars.web.beans.v2.ValidationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


@RestController
@Slf4j
public class EseApiController {
    private final Startup startup;
    private final ResvService resvService;
    private final ConnectionRepository connRepo;

    public EseApiController(Startup startup, ResvService resvService, ConnectionRepository connRepo) {
        this.startup = startup;
        this.resvService = resvService;
        this.connRepo = connRepo;
    }

    @ExceptionHandler(ConsistencyException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleException(ConsistencyException ex) {
        log.warn("consistency error " + ex.getMessage());
    }

    @ExceptionHandler(StartupException.class)
    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    public void handleException() {
        log.warn("Still in startup");
    }


    @RequestMapping(value = "/api/ese/l2vpn/validate", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public ValidationResponse validate(@RequestBody L2VPN l2VPNRequest)
            throws ConsistencyException, StartupException {
        startup.startupCheck();
        return null;
    }

    @RequestMapping(value = "/api/ese/l2vpn/new", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public L2VPN newL2VPN(@RequestBody L2VPN l2VPNRequest)
            throws ConsistencyException, StartupException {
        startup.startupCheck();

        return null;
    }

    @RequestMapping(value = "/api/ese/l2vpn/modify", method = RequestMethod.PATCH)
    @ResponseBody
    @Transactional
    public L2VPN updateL2VPN(@RequestBody L2VPN l2VPNRequest)
            throws ConsistencyException, StartupException {
        startup.startupCheck();

        return null;
    }

}
