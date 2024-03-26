package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;
import net.es.oscars.resv.enums.Phase;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "refId")
public class Schedule {
    @JsonCreator
    public Schedule(@JsonProperty("connectionId") String connectionId,
                    @JsonProperty("beginning") @NonNull Instant beginning,
                    @JsonProperty("ending") @NonNull Instant ending,
                    @JsonProperty("phase") Phase phase,
                    @JsonProperty("refId") String refId) {
        this.connectionId = connectionId;
        this.beginning = beginning;
        this.ending = ending;
        this.phase = phase;
        this.refId = refId;
    }


    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    private Instant beginning;

    @NonNull
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    private Instant ending;

    // these will be populated by the system when designing
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String connectionId;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Phase phase;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String refId;


    public Boolean overlaps(Instant b, Instant e) {
        boolean result = true;
        if (this.getEnding().isBefore(b)) {
            result = false;
        }
        if (this.getBeginning().isAfter(e)) {
            result = false;
        }
        return result;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Schedule schedule = (Schedule) o;
        return getId() != null && Objects.equals(getId(), schedule.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
