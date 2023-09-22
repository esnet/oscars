package net.es.oscars.sb.nso.ent;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a Nokia SDP id. It is reserved per device.
 */
@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NsoSdpId {

    @Id
    @GeneratedValue
    private Long id;

    private String device;
    private Integer sdpId;
    private String precedence;
    private String target;

    private String connectionId;

    private Long scheduleId;

}
