package net.es.oscars.app.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrettyPrinter {
    public static void prettyLog(Object o) {
        try {
            String pretty = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(o);

            log.info(pretty);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
