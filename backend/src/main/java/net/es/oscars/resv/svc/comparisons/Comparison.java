package net.es.oscars.resv.svc.comparisons;

import org.springframework.validation.Errors;

import java.util.Map;

public interface Comparison {
    void compare();
    boolean hasErrors();
    Map<String, Errors> getAllErrors();
}
