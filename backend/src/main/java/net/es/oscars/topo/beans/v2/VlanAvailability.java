package net.es.oscars.topo.beans.v2;

import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.topo.common.model.oscars1.IntRange;
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
//    @JsonGetter("intervals")
//    private String asIntervals() {
//        return IntRange.asIntervalNotation(ranges);
//    }

    @JsonGetter("tuples")
    private List<List<Integer>> asTuples() {
        List<List<Integer>> result = new ArrayList<>();

        ranges.stream()
                .sorted()
                .forEach(range -> result.add(new ArrayList<>() {{
                    add(range.getFloor());
                    add(range.getCeiling());
                }}));

        return result;
    }
}
