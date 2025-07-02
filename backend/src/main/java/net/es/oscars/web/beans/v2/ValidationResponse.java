package net.es.oscars.web.beans.v2;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidationResponse {
    private boolean valid;
    private String message;
}
