package net.es.oscars.sb.nso.ent;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a virtual circuit ID used for a spoke SDP.
 */
@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NsoSdpVcId {

    @Id
    @GeneratedValue
    private Long id;

    private Integer vcId;

    private String device;
    private Integer sdpId;
    private String connectionId;
    private Long scheduleId;

}
