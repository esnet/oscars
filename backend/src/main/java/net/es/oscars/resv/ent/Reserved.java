package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Getter
@Setter
@ToString
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Reserved {
    @JsonCreator
    public Reserved(@JsonProperty("connectionId") @NonNull String connectionId,
                    @JsonProperty("cmp") @NonNull Components cmp,
                    @JsonProperty("schedule") @NonNull Schedule schedule) {
        this.connectionId = connectionId;
        this.cmp = cmp;
        this.schedule = schedule;
    }

    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @NonNull
    private String connectionId;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @NonNull
    private Components cmp;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @NonNull
    private Schedule schedule;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Reserved reserved = (Reserved) o;
        return getId() != null && Objects.equals(getId(), reserved.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
