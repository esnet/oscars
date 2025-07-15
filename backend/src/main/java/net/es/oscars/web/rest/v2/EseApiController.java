package net.es.oscars.web.rest.v2;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.app.util.UsernameGetter;
import net.es.oscars.resv.db.ConnectionRepository;
import net.es.oscars.resv.enums.ConnectionMode;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.resv.svc.L2VPNService;
import net.es.oscars.resv.svc.ResvService;

import net.es.oscars.topo.pop.ConsistencyException;

import net.es.oscars.model.L2VPN;
import net.es.oscars.web.beans.BandwidthAvailabilityResponse;
import net.es.oscars.web.beans.ConnException;
import net.es.oscars.web.beans.ConnectionFilter;
import net.es.oscars.web.beans.v2.L2VPNList;
import net.es.oscars.web.beans.v2.ValidationResponse;
import net.es.oscars.web.simple.SimpleConnection;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


@RestController
@Slf4j
@Data
public class EseApiController {
    private final Startup startup;
    private L2VPNService l2VPNService;
    private UsernameGetter usernameGetter;

    public EseApiController(Startup startup, L2VPNService l2VPNService, UsernameGetter usernameGetter) {
        this.startup = startup;
        this.l2VPNService = l2VPNService;
        this.usernameGetter = usernameGetter;
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


    @RequestMapping(value = "/api/l2vpn/get/{connectionId}", method = RequestMethod.GET)
    @ResponseBody
    @Transactional
    public L2VPN get(@PathVariable String connectionId) throws StartupException, ConnException {
        startup.startupCheck();
        return l2VPNService.get(connectionId);
    }

    @RequestMapping(value = "/api/l2vpn/list", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public L2VPNList list(@RequestBody ConnectionFilter filter) throws StartupException, ConnException {
        startup.startupCheck();
        return l2VPNService.list(filter);
    }

    @RequestMapping(value = "/api/l2vpn/validate", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public ValidationResponse validateNew(@RequestBody L2VPN l2VPNRequest) throws StartupException {
        startup.startupCheck();

        return l2VPNService.validate(l2VPNRequest, ConnectionMode.NEW);
    }

    @RequestMapping(value = "/api/l2vpn/validate", method = RequestMethod.PUT)
    @ResponseBody
    @Transactional
    public ValidationResponse validateModify(@RequestBody L2VPN l2VPNRequest) throws StartupException {
        startup.startupCheck();
        return l2VPNService.validate(l2VPNRequest, ConnectionMode.MODIFY);
    }


    @RequestMapping(value = "/api/l2vpn/new", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public L2VPN newL2VPN(Authentication authentication, @RequestBody L2VPN l2vpn) throws StartupException, ConnException {
        startup.startupCheck();
        l2vpn.getMeta().setUsername(usernameGetter.username(authentication));

        return l2VPNService.create(l2vpn);
    }

    @RequestMapping(value = "/api/l2vpn/replace", method = RequestMethod.PUT)
    @ResponseBody
    @Transactional
    public L2VPN replaceL2VPN(@RequestBody L2VPN l2vpn) throws StartupException, ConnException {
        startup.startupCheck();

        throw new ConnException("replace not implemented yet");
    }

    @RequestMapping(value = "/api/l2vpn/availability", method = RequestMethod.POST)
    @ResponseBody
    public BandwidthAvailabilityResponse bandwidthAvailability(@RequestBody L2VPN l2vpn)
            throws StartupException, ConnException {
        startup.startupCheck();

        return l2VPNService.bwAvailability(l2vpn);
    }

}
