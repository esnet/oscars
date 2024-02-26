package net.es.oscars.web.beans;

import lombok.*;
import net.es.oscars.resv.ent.Connection;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModifyResponse {
    @NonNull
    protected Boolean success;
    protected String explanation;
    protected Connection connection;


}
