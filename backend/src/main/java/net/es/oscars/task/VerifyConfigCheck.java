package net.es.oscars.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import net.es.oscars.pss.svc.PSSProxy;
import net.es.oscars.pss.svc.PssHealthChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Component
public class VerifyConfigCheck {

    @Autowired
    private Startup startup;

    @Autowired
    private PssHealthChecker checker;

    @Autowired
    private PSSProxy pssProxy;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processingLoop() {
        if (startup.isInStartup() || startup.isInShutdown()) {
//            log.info("application in startup or shutdown; will perform verifications later");
            return;
        }
        ObjectMapper m = new ObjectMapper();
/*
        Set<Device> verified = new HashSet<>();
        for (Device d: checker.getDevicesToVerify()) {
            try {
                DeviceConfigRequest req = checker.verifyDeviceFacts(d);
                log.info("verifying device "+req.getDevice());
                DeviceConfigResponse resp = pssProxy.getConfig(req);
                log.info(resp.getAsJson());
//                log.info(resp.getConfig());
                verified.add(d);
            } catch (VerifyException | PSSException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
        checker.getDevicesToVerify().removeAll(verified);
*/
    }

}