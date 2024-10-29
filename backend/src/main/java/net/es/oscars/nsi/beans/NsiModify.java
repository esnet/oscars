package net.es.oscars.nsi.beans;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NsiModify {
    public String nsiConnectionId;
    public Instant timeout;
    public Spec initial;
    private Spec modified;

    @Data
    @Builder
    public static class Spec {
        Instant beginning;
        Instant ending;
        int bandwidth;
        int dataplaneVersion;
    }
}
