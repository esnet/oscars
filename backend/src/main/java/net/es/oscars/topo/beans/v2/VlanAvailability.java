package net.es.oscars.topo.beans.v2;

import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.oscars.topo.beans.IntRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VlanAvailability {

    private Set<IntRange> ranges;

    @JsonGetter("expression")
    private String asExpression() {
        return IntRange.asString(ranges);
    }

    @JsonGetter("tuples")
    private List<List<Integer>> asTuples() {
        List<List<Integer>> result = new ArrayList<>();
        for (IntRange range : ranges) {
            List<Integer> tuple = new ArrayList<>();
            tuple.add(range.getFloor());
            tuple.add(range.getCeiling());
            result.add(tuple);
        }
        return result;
    }
}
