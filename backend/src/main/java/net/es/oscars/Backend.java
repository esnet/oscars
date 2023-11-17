package net.es.oscars;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.Startup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties
@EnableAsync
@EnableScheduling
@EnableRetry
@Slf4j
public class Backend {
    public static void main(String[] args) {
        ConfigurableApplicationContext app = SpringApplication.run(Backend.class, args);
        Startup startup = (Startup) app.getBean("startup");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            startup.setInStartup(false);
            startup.setInShutdown(true);
        }));

        try {
            startup.onStart();
        } catch (Exception ex) {
            log.error("startup error!", ex);
            System.exit(1);
        }
    }

    @Configuration
    @Profile("test")
    @ComponentScan(lazyInit = true)
    static class LocalConfig {
    }
}
