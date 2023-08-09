package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.*;

@Entity
@Data
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

}
