package net.es.oscars.resv.svc.populators;

import org.springframework.validation.Errors;

import java.util.Map;

public interface Populator {
    void populate();
    boolean hasErrors();
}
