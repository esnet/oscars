package net.es.oscars.v12.api;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Builder
public class IntentVerifyResult {
    Boolean satisfiable;
}
