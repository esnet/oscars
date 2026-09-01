package net.es.oscars.sb.nso;

import net.es.topo.common.model.oscars1.IntRange;
import java.util.HashSet;
import java.util.Set;

public class IntegerSet {

    public static Set<Integer> availableFromRangeStrings(String usedRangeString, String allowedRangeString){
        Set<Integer> usedIds = singleSetFromExpr(usedRangeString);
        return availableFromUsedSetAndAllowedString(usedIds, allowedRangeString);
    }

    public static Set<Integer> availableFromUsedSetAndAllowedString(Set<Integer> usedIds, String allowedRangeString){
        Set<Integer> allowedIds = singleSetFromExpr(allowedRangeString);
        return availableFromUsedAndAllowedSets(usedIds, allowedIds);
    }


    public static Set<Integer> singleSetFromExpr(String rangeExpr){
        Set<IntRange> ranges = IntRange.fromExpression(rangeExpr);
        Set<Integer> result = new HashSet<>();
        ranges.forEach(r -> result.addAll(r.asSet()));
        return result;
    }

    public static Set<Integer> availableFromUsedAndAllowedSets(Set<Integer> usedIds, Set<Integer> allowedIds) {
        Set<Integer> result = new HashSet<>(allowedIds);
        result.removeAll(usedIds);
        return result;
    }

}
