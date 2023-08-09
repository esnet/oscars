package net.es.oscars.resv.ent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TagCategory {
    @JsonCreator
    public TagCategory(
            @JsonProperty("category") @NonNull String category,
            @JsonProperty("source") String source) {
        this.category = category;
        this.source = source;
    }

    @Id
    @GeneratedValue
    private Long id;

    @NonNull
    private String category;

    private String source;

}
