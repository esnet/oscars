package net.es.oscars.nso.ent;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a Nokia pair of qos/sap-ingress and sap-egress policy IDs. It is reserved per device.
 */

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NsoQosSapPolicyId {

    @Id
    @GeneratedValue
    private Long id;

    private String device;
    private Integer policyId;
    // which SAP this will be applied to
    private String sap;
    private String connectionId;
    private Long scheduleId;

}
