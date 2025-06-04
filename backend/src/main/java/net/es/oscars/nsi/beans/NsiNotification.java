package net.es.oscars.nsi.beans;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.*;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class NsiNotification {
    @Id
    @GeneratedValue
    private Long id;
    protected String connectionId;
    protected Long notificationId;
    protected NsiNotificationType type;
    @Lob
    protected String xml;
}
