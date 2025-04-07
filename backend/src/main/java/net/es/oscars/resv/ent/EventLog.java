package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Transactional
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventLog {
    @JsonCreator
    public EventLog(@JsonProperty("connectionId") @NonNull String connectionId,
                    @JsonProperty("created") @NonNull Instant created,
                    @JsonProperty("archived") @NonNull Instant archived,
                    @JsonProperty("events") @NonNull List<Event> events) {
        this.connectionId = connectionId;
        this.created = created;
        this.archived = archived;
        this.events = events;
    }

    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @NonNull
    private String connectionId;

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    private Instant created;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    private Instant archived;

    @OneToMany(cascade = CascadeType.ALL)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @ToString.Exclude
    private List<Event> events;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        EventLog eventLog = (EventLog) o;
        return getId() != null && Objects.equals(getId(), eventLog.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
