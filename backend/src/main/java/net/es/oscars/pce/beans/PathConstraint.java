package net.es.oscars.pce.beans;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@Builder
public class PathConstraint {
    List<String> ero;
    Set<String> exclude;

}
