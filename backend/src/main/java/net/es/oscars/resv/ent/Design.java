package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class Design {
    @JsonCreator
    public Design(@JsonProperty("designId") @NonNull String designId,
                  @JsonProperty("cmp") @NonNull Components cmp,
                  @JsonProperty("description") String description,
                  @JsonProperty("username") String username) {

        this.description = description;
        this.designId = designId;
        this.cmp = cmp;
        this.username = username;
    }


    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @Column(unique = true)
    @NonNull
    private String designId;

    private String description;

    @ManyToOne(cascade = CascadeType.ALL)
    @NonNull
    private Components cmp;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String username;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Design design = (Design) o;
        return getId() != null && Objects.equals(getId(), design.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
