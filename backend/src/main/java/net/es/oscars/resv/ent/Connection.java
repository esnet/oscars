package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.resv.enums.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Connection {
    @JsonCreator
    public Connection(@JsonProperty("connectionId") @NonNull String connectionId,
                      @JsonProperty("phase") @NonNull Phase phase,
                      @JsonProperty("mode") @NonNull BuildMode mode,
                      @JsonProperty("state") @NonNull State state,
                      @JsonProperty("deployment-state") @NonNull DeploymentState deploymentState,
                      @JsonProperty("deployment-intent") @NonNull DeploymentIntent deploymentIntent,
                      @JsonProperty("username") @NonNull String username,
                      @JsonProperty("description") @NonNull String description,
                      @JsonProperty("serviceId") String serviceId,
                      @JsonProperty("reserved") Reserved reserved,
                      @JsonProperty("tags") List<Tag> tags,
                      @JsonProperty("held") Held held,
                      @JsonProperty("archived") Archived archived,
                      @JsonProperty("connection_mtu") @NonNull Integer connection_mtu,
                      @JsonProperty("last_modified") @NonNull Integer last_modified) {
        this.connectionId = connectionId;
        this.phase = phase;
        this.mode = mode;
        this.state = state;
        this.deploymentState = deploymentState;
        this.deploymentIntent = deploymentIntent;
        this.username = username;
        this.description = description;
        this.serviceId = serviceId;
        this.reserved = reserved;
        this.tags = tags;
        this.held = held;
        this.archived = archived;
        this.connection_mtu = connection_mtu;
        this.last_modified = last_modified;
    }


    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @JsonProperty("bandwidth")
    private int getBandwidth() {
        Components cmp;
        switch (this.phase) {
            case HELD -> {
                cmp = this.held.getCmp();
            }
            case RESERVED -> {
                cmp = this.reserved.getCmp();

            }
            case ARCHIVED -> {
                cmp = this.archived.getCmp();
            }
            default -> { return 0; }
        }
        // return the maximum of any of the fixture or pipe bandwidths
        int result = 0;
        for (VlanFixture fixture: cmp.getFixtures()) {
            if (fixture.getEgressBandwidth() > result) {
                result = fixture.getEgressBandwidth();
            }
            if (fixture.getIngressBandwidth() > result) {
                result = fixture.getIngressBandwidth();
            }
        }
        for (VlanPipe pipe: cmp.getPipes()) {
            if (pipe.getAzBandwidth() > result) {
                result = pipe.getAzBandwidth();
            }
            if (pipe.getZaBandwidth() > result) {
                result = pipe.getZaBandwidth();
            }
        }
        return result;

    }

    @NonNull
    private String connectionId;

    @NonNull
    private Phase phase;

    @NonNull
    private BuildMode mode;

    @NonNull
    private DeploymentState deploymentState;

    @NonNull
    private DeploymentIntent deploymentIntent;

    @NonNull
    private State state;

    @NonNull
    private String description;

    @NonNull
    private String username;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String serviceId;

    @OneToMany(cascade = CascadeType.ALL)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ToString.Exclude
    private List<Tag> tags;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Reserved reserved;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Held held;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Archived archived;

    @NonNull
    private Integer connection_mtu;

    @NonNull
    private Integer last_modified;

    @Transient
    public ConnectionSouthbound southbound;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Connection that = (Connection) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
