package net.es.oscars.v12.model.intent;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import net.es.oscars.v12.model.resource.OverlayResource;

import java.util.List;


@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class OverlayIntent extends Intent {

    @JsonProperty("satisfied-by")
    @OneToOne
    OverlayResource satisfiedBy;


    @OneToMany
    @ToString.Exclude
    List<TunnelIntent> tunnels;


}
