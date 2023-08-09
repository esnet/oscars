package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class EroHop {
    @JsonCreator
    public EroHop(@JsonProperty("urn") @NonNull String urn) {
        this.urn = urn;
    }

    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;


    @NonNull
    private String urn;


}
