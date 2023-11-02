package net.es.oscars.web.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinimalConnEntry {
    private List<MinimalConnEndpoint> endpoints;
    private Map<String, List<Integer>> sdps;
    private String southbound;
    private Set<List<String>> eros;
    private Integer start;
    private Integer end;
    private String description;
}
