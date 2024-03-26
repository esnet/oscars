package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
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
public class Held {
    @JsonCreator
    public Held(@JsonProperty("connectionId") @NonNull String connectionId,
                @JsonProperty("cmp") @NonNull Components cmp,
                @JsonProperty("expiration") @NonNull Instant expiration,
                @JsonProperty("schedule") @NonNull Schedule schedule) {
        this.connectionId = connectionId;
        this.cmp = cmp;
        this.expiration = expiration;
        this.schedule = schedule;
    }

    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @NonNull
    @Column
    private String connectionId;

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    private Instant expiration;

    @NonNull
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Components cmp;

    @NonNull
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Schedule schedule;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Held held = (Held) o;
        return getId() != null && Objects.equals(getId(), held.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
