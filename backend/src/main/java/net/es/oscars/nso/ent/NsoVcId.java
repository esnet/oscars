package net.es.oscars.nso.ent;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a virtual circuit ID. It is reserved globally on the whole network.
 *
 * It is also used as the Nokia service-id.
 */
@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NsoVcId {

    @Id
    @GeneratedValue
    private Long id;
    private Integer vcId;
    private String connectionId;
    private Long scheduleId;

}
