package net.es.oscars.app;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.app.props.StartupProperties;
import net.es.oscars.sb.nso.resv.LegacyPopulator;
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

    private final List<StartupComponent> components;
    private final StartupProperties startupProperties;
    private final TopoPopulator topoPopulator;
    private final LegacyPopulator legacyPopulator;

    @Setter
    @Getter
    private boolean inStartup = true;
    @Setter
    @Getter
    private boolean inShutdown = false;


    @Bean
    public Executor taskExecutor() {
        return new SimpleAsyncTaskExecutor();
    }

    @Autowired
    public Startup(StartupProperties startupProperties,
                   TopoPopulator topoPopulator,
                   UIPopulator uiPopulator, LegacyPopulator legacyPopulator) {
        this.startupProperties = startupProperties;
        this.topoPopulator = topoPopulator;
        this.legacyPopulator = legacyPopulator;

        components = new ArrayList<>();
        components.add(uiPopulator);
    }

    @WithSpan(value="startup")
    public void onStart() throws IOException, ConsistencyException, TopoException {
        System.out.println(startupProperties.getBanner());
        Span currentSpan = Span.current();

        currentSpan.addEvent("starting up");
        currentSpan.setAttribute("isTestAttribute", true);
        log.info("OSCARS starting up");

        this.setInStartup(true);
        if (startupProperties.getExit()) {
            log.info("OSCARS shutting down");
            this.setInStartup(false);
            this.setInShutdown(true);
            System.out.println("Exiting (startup.exit is true)");
            System.exit(0);
        }
        topoPopulator.refresh();
        legacyPopulator.importPssToNso();

        try {
            for (StartupComponent sc : this.components) {
                sc.startup();
            }
        } catch (StartupException ex) {
            log.error(ex.getMessage(), ex);
            System.out.println("Exiting..");
            System.exit(1);
        }

        this.setInStartup(false);

    }

}
