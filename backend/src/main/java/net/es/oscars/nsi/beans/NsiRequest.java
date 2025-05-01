package net.es.oscars.nsi.beans;

import lombok.Builder;
import lombok.Data;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ReserveType;

import java.time.Instant;

@Data
@Builder
public class NsiRequest {
    public String nsiConnectionId;
    public Instant timeout;
    public ReserveType initial;
    private ReserveType incoming;
}
