package net.es.oscars.web.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.oscars.model.Interval;

import java.util.List;
import java.util.Set;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PceRequest {
    private Interval interval;
    private String a;
    private String z;
    private Integer azBw;
    private Integer zaBw;
    private List<String> include;
    private Set<String> exclude;


}
