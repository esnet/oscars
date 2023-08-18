package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import jakarta.persistence.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Vlan {

    @JsonCreator
    public Vlan(@JsonProperty("connectionId") String connectionId,
                @JsonProperty("schedule") Schedule schedule,
                @JsonProperty("urn") @NonNull String urn,
                @JsonProperty("vlanId") @NonNull Integer vlanId) {
        this.connectionId = connectionId;
        this.urn = urn;
        this.vlanId = vlanId;
        this.schedule = schedule;
    }

    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @NonNull
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String urn;


    // mandatory; must be set even if empty

    @NonNull
    private Integer vlanId;


    // these will be populated by the system when designing
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String connectionId;

    @ManyToOne
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Schedule schedule;



}
