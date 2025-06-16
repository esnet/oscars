package net.es.oscars.web.rest.v2;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.resv.svc.L2VPNService;
import net.es.oscars.resv.svc.ResvService;

import net.es.oscars.topo.pop.ConsistencyException;

import net.es.oscars.model.L2VPN;
import net.es.oscars.web.beans.BandwidthAvailabilityResponse;
import net.es.oscars.web.beans.ConnException;
import net.es.oscars.web.beans.v2.ValidationResponse;
import net.es.oscars.web.simple.SimpleConnection;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


@RestController
@Slf4j
public class EseApiController {
    private final Startup startup;
    private final L2VPNService l2VPNService;

    public EseApiController(Startup startup, L2VPNService l2VPNService) {
        this.startup = startup;
        this.l2VPNService = l2VPNService;
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


    @RequestMapping(value = "/api/l2vpn/validate", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public ValidationResponse validate(@RequestBody L2VPN l2VPNRequest)
            throws ConsistencyException, StartupException {
        startup.startupCheck();
        return null;
    }

    @RequestMapping(value = "/api/l2vpn/new", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public L2VPN newL2VPN(@RequestBody L2VPN l2VPNRequest)
            throws ConsistencyException, StartupException {
        startup.startupCheck();

        return null;
    }

    @RequestMapping(value = "/api/l2vpn/modify", method = RequestMethod.PUT)
    @ResponseBody
    @Transactional
    public L2VPN updateL2VPN(@RequestBody L2VPN l2VPNRequest)
            throws ConsistencyException, StartupException {
        startup.startupCheck();

        return null;
    }

    @RequestMapping(value = "/api/conn/availability", method = RequestMethod.POST)
    @ResponseBody
    public BandwidthAvailabilityResponse bandwidthAvailability(@RequestBody L2VPN l2VPNRequest)
            throws StartupException, ConnException {
        startup.startupCheck();

        return l2VPNService.bwAvailability(l2VPNRequest);
    }

}
