package net.es.oscars.nso.resv;

import net.es.oscars.topo.beans.IntRange;

import java.util.HashSet;
import java.util.Set;

public class IntegerSet {

    public static Set<Integer> availableFromRangeStrings(String usedRangeString, String allowedRangeString){
        Set<IntRange> idRanges = IntRange.fromExpression(usedRangeString);
        Set<Integer> usedIds = new HashSet<>();
        idRanges.forEach(r -> usedIds.addAll(r.asSet()));

        return availableFromUsedSetAndAllowedString(usedIds, allowedRangeString);
    }

    public static Set<Integer> availableFromUsedSetAndAllowedString(Set<Integer> usedIds, String allowedRangeString){
        Set<IntRange> idRanges = IntRange.fromExpression(allowedRangeString);
        Set<Integer> allowedIds = new HashSet<>();
        idRanges.forEach(r -> allowedIds.addAll(r.asSet()));
        return availableFromUsedAndAllowedSets(usedIds, allowedIds);
    }



    public static Set<Integer> availableFromUsedAndAllowedSets(Set<Integer> usedIds, Set<Integer> allowedIds) {
        Set<Integer> result = new HashSet<>(allowedIds);
        result.removeAll(usedIds);
        return result;
    }

}
