package net.es.oscars.nsi.ent;

import lombok.*;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.LifecycleStateEnumType;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ProvisionStateEnumType;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ReservationStateEnumType;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NsiMapping {
    @Id
    @GeneratedValue
    private Long id;

    @NonNull
    private String nsiConnectionId;

    @NonNull
    private String nsiGri;

    @NonNull
    private String oscarsConnectionId;

    @NonNull
    private String nsaId;

    private String src;

    private String dst;

    @NonNull
    private Integer dataplaneVersion;

    private Integer deployedDataplaneVersion;

    @NonNull
    private LifecycleStateEnumType lifecycleState;
    @NonNull
    private ReservationStateEnumType reservationState;
    @NonNull
    private ProvisionStateEnumType provisionState;

    @Builder.Default
    private int notificationId = 1;

}
