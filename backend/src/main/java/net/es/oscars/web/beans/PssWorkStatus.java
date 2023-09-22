package net.es.oscars.web.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.oscars.sb.beans.QueueName;
import net.es.oscars.resv.enums.State;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PssWorkStatus {
    private String connectionId;
    private QueueName work;
    private State next;

    private String explanation;
}
