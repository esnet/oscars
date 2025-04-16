package net.es.oscars.nsi.ent;

import lombok.*;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.apache.commons.validator.routines.UrlValidator;

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
        UrlValidator validator = new UrlValidator();
        return !callbackUrl.isEmpty() && validator.isValid(callbackUrl);
    }
}
