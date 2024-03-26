package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;
import net.es.oscars.resv.enums.EventType;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    @JsonCreator
    public Event(@JsonProperty("connectionId") @NonNull String connectionId,
                 @JsonProperty("occurrence") @NonNull Instant occurrence,
                 @JsonProperty("type") @NonNull EventType type,
                 @JsonProperty("description") @NonNull String description,
                 @JsonProperty("username") @NonNull String username) {
        this.connectionId = connectionId;
        this.occurrence = occurrence;
        this.type = type;
        this.description = description;
        this.username = username;
    }

    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @NonNull
    private String connectionId;

    @NonNull
    private Instant occurrence;

    @NonNull
    private EventType type;

    @NonNull
    private String description;

    private String username;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Event event = (Event) o;
        return getId() != null && Objects.equals(getId(), event.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
