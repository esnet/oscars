package net.es.oscars.app;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.app.props.StartupProperties;
import net.es.oscars.app.syslog.Syslogger;
import net.es.oscars.sb.nso.resv.LegacyPopulator;
import net.es.oscars.security.db.UserPopulator;
import net.es.oscars.topo.beans.TopoException;
import net.es.oscars.topo.pop.ConsistencyException;
import net.es.oscars.topo.pop.TopoPopulator;
import net.es.oscars.topo.pop.UIPopulator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class Startup {

    private List<StartupComponent> components;
    private StartupProperties startupProperties;
    private Syslogger syslogger;

    private TopoPopulator topoPopulator;

    private LegacyPopulator legacyPopulator;

    @Getter
    private boolean inStartup = true;
    @Getter
    private boolean inShutdown = false;

    public void setInStartup(boolean inStartup) {
        this.inStartup = inStartup;
    }

    public void setInShutdown(boolean inShutdown) {
        this.inShutdown = inShutdown;
    }


    @Bean
    public Executor taskExecutor() {
        return new SimpleAsyncTaskExecutor();
    }

    @Autowired
    public Startup(StartupProperties startupProperties,
                   Syslogger syslogger,
                   TopoPopulator topoPopulator,
                   UserPopulator userPopulator,
                   UIPopulator uiPopulator, LegacyPopulator legacyPopulator) {
        this.startupProperties = startupProperties;
        this.topoPopulator = topoPopulator;
        this.syslogger = syslogger;
        this.legacyPopulator = legacyPopulator;

        components = new ArrayList<>();
        components.add(userPopulator);
        components.add(uiPopulator);
    }

    public void onStart() throws IOException, ConsistencyException, TopoException {
        System.out.println(startupProperties.getBanner());

        this.setInStartup(true);
        if (startupProperties.getExit()) {
            log.info("In Shutdown");
            this.setInStartup(false);
            this.setInShutdown(true);
            syslogger.sendSyslog("OSCARS APPLICATION SHUTDOWN COMPLETED");
            System.out.println("Exiting (startup.exit is true)");
            System.exit(0);
        }
        topoPopulator.refresh(false);
        legacyPopulator.importPssToNso();




        try {
            for (StartupComponent sc : this.components) {
                sc.startup();
            }
        } catch (StartupException ex) {
            ex.printStackTrace();
            System.out.println("Exiting..");
            System.exit(1);
        }
        log.info("OSCARS startup successful.");

        syslogger.sendSyslog("OSCARS APPLICATION STARTUP COMPLETED");

        this.setInStartup(false);

    }

}
