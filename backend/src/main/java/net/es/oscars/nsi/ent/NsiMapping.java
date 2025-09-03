package net.es.oscars.nsi.ent;

import lombok.*;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.LifecycleStateEnumType;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ProvisionStateEnumType;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ReservationStateEnumType;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NsiMapping {
    @Id
    @GeneratedValue
    private Long id;

    // NSI connection ID. Generated in OSCARS. Should be unique for that L2VPN instance.
    @NonNull
    private String nsiConnectionId;

    // Part of the NSA protocol. An L2VPN identifier that may be on more than one NSI provider ("Fabric", etc).
    // We keep it, but don't actually use it for any tracking. Not guaranteed to be unique.
    // This must be a UUID.
    @NonNull
    private String nsiGri;

    // OSCARS connection ID
    @NonNull
    private String oscarsConnectionId;

    // Requestor ID
    @NonNull
    private String nsaId;

    // Source for AZ fixtures. These URNs are generated.
    // These get generated and published at https://oscars.es.net/api/topo/nml
    // Example: "urn:ogf:network:es.net:2013::losa-cr6:2_1_c4_1:+?vlan=3871"
    private String src;
    private String dst;

    // Connection can change over the lifetime. Dataplane version increments on each
    // change to the connection. (Goes from reserved to held, modification to bandwidth, etc.)
    @NonNull
    private Integer dataplaneVersion;
    // Deployment provision does not necessarily happen immediately after reserve.
    // The deployed dataplane version may differ from the (reserved) dataplane version.
    private Integer deployedDataplaneVersion;

    @NonNull
    private LifecycleStateEnumType lifecycleState;
    @NonNull
    private ReservationStateEnumType reservationState;
    @NonNull
    private ProvisionStateEnumType provisionState;

    // This tracks notification ID for events, such as dataplane version bumps.
    // Whatever uses notificationId will bump the value with the setter when sending
    // a notification. Used for things like general error events, reserve timeouts, dataplane change callbacks.
    @Builder.Default
    private Integer notificationId = 1;
    public Integer getNotificationId() {
        if (notificationId == null) {
            notificationId = 1;
        }
        return notificationId;
    }

    private Instant lastModified;

}
