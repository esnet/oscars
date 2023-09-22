package net.es.oscars.sb.nso.ent;

import jakarta.persistence.*;
import lombok.*;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.NsoVPLS;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode
@Entity
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NsoService {
    @Id
    @GeneratedValue
    private UUID id;

    private Instant updated;
    private String connectionId;

    @JdbcTypeCode(SqlTypes.JSON)
    private NsoVPLS vpls;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<NsoLSP> lsp;


}
