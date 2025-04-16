package net.es.oscars.nsi.ent;

import lombok.*;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NsiRequesterNSA {
    @Id
    @GeneratedValue
    private Long id;

    @NonNull
    private String nsaId;


    @NonNull
    private String callbackUrl;

    public boolean callbacksEnabled() {
        return callbackUrl.startsWith("https") || callbackUrl.startsWith("http");
    }
}
