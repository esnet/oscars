package net.es.oscars.topo.beans;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import jakarta.persistence.Embeddable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
@EqualsAndHashCode

@Slf4j
public class IntRange {
    private Integer floor;
    private Integer ceiling;

    public boolean contains(Integer i) {
        return (floor <= i && ceiling >= i);
    }

    public Set<IntRange> subtract(Integer i) throws NoSuchElementException {
        HashSet<IntRange> result = new HashSet<>();
        if (!this.contains(i)) {
            throw new NoSuchElementException("range " + this.toString() + " does not contain " + i);
        }
        // remove last one: return an empty set
        if (this.getFloor().equals(this.getCeiling())) {
            return result;
        }

        // remove ceiling or floor: return a single range
        if (this.getCeiling().equals(i)) {
            IntRange r = IntRange.builder().ceiling(i - 1).floor(this.getFloor()).build();
            result.add(r);
        } else if (this.getFloor().equals(i)) {
            IntRange r = IntRange.builder().ceiling(this.getCeiling()).floor(i + 1).build();
            result.add(r);
        } else {
            // split into two
            IntRange top = IntRange.builder().floor(this.getFloor()).ceiling(i - 1).build();
            IntRange bottom = IntRange.builder().floor(i + 1).ceiling(this.getCeiling()).build();
            result.add(top);
            result.add(bottom);
        }
        return result;
    }

    public Set<Integer> asSet() {
        Set<Integer> result = new HashSet<>();
        for (Integer i = this.floor; i <= this.ceiling; i++) {
            result.add(i);
        }
        return result;
    }


    public static Set<IntRange> fromSet(Set<Integer> ints) {
        Set<IntRange> result = new HashSet<>();
        List<Integer> sortedInts = new ArrayList<>();
        sortedInts.addAll(ints);
        Collections.sort(sortedInts);

        IntRange range = IntRange.builder().floor(-1).ceiling(-1).build();

        for (Integer idx = 0; idx < sortedInts.size(); idx++) {
            Integer number = sortedInts.get(idx);
            if (range.getCeiling() == -1) {
                range.setFloor(number);
                range.setCeiling(number);
            } else {
                if (range.getCeiling() + 1 == number) {
                    range.setCeiling(number);
                } else {
                    result.add(range);
                    range = IntRange.builder().floor(number).ceiling(number).build();
                }
                if (idx == sortedInts.size() - 1) {
                    range.setCeiling(number);
                    result.add(range);
                }
            }
        }

        return result;
    }

    public static Boolean isValidExpression(String text) {
        Pattern re_valid = Pattern.compile(
                "# Validate comma separated integers/integer ranges.\n" +
                        "^             # Anchor to start of string.         \n" +
                        "[0-9]+        # Integer of 1st value (required).   \n" +
                        "(?:           # Range for 1st value (optional).    \n" +
                        "  :           # Colon separates range integer.      \n" +
                        "  [0-9]+      # Range integer of 1st value.        \n" +
                        ")?            # Range for 1st value (optional).    \n" +
                        "(?:           # Zero or more additional values.    \n" +
                        "  ,           # Comma separates additional values. \n" +
                        "  [0-9]+      # Integer of extra value (required). \n" +
                        "  (?:         # Range for extra value (optional).  \n" +
                        "    :         # Colon separates range integer.      \n" +
                        "    [0-9]+    # Range integer of extra value.      \n" +
                        "  )?          # Range for extra value (optional).  \n" +
                        ")*            # Zero or more additional values.    \n" +
                        "$             # Anchor to end of string.           ",
                Pattern.COMMENTS);
        Matcher m = re_valid.matcher(text);
        return m.matches();
    }

    public static Set<IntRange> fromExpression(String text) throws NumberFormatException {

        Set<IntRange> firstPass = new HashSet<>();
        Pattern re_next_val = Pattern.compile(
                "# extract next integers/integer range value.    \n" +
                        "([0-9]+)      # $1: 1st integer (Base).         \n" +
                        "(?:           # Range for value (optional).     \n" +
                        "  :           # Colon separates range integer.   \n" +
                        "  ([0-9]+)    # $2: 2nd integer (Range)         \n" +
                        ")?            # Range for value (optional). \n" +
                        "(?:,|$)       # End on comma or string end.",
                Pattern.COMMENTS);
        Matcher m = re_next_val.matcher(text);

        while (m.find()) {
            Integer floor = Integer.parseInt(m.group(1));
            IntRange ir = IntRange.builder().floor(floor).ceiling(floor).build();
            if (m.group(2) != null) {
                Integer ceiling = Integer.parseInt(m.group(2));
                if (ceiling < floor) {
                    throw new NumberFormatException("ceiling < floor");
                }
                ir.setCeiling(ceiling);
            }
            firstPass.add(ir);
        }

        Set<IntRange> result = new HashSet<>();
        result.addAll(mergeIntRanges(firstPass));
        return result;

    }



    public static String asString(Collection<IntRange> ranges) {
        ranges = mergeIntRanges(ranges);
        List<IntRange> listOfRanges = new ArrayList<>();
        listOfRanges.addAll(ranges);
        listOfRanges.sort(Comparator.comparing(IntRange::getFloor));

        List<String> parts = new ArrayList<>();
        listOfRanges.forEach(r -> {
            if (r.getCeiling().equals(r.getFloor())) {
                parts.add(r.getCeiling() + "");
            } else {
                parts.add(r.getFloor() + ":" + r.getCeiling());

            }
        });
        return StringUtils.join(parts, ',');
    }


    public static List<IntRange> mergeIntRanges(Collection<IntRange> input) {

        // don't mutate the list; copy it first
        List<IntRange> ranges = new ArrayList<>();
        input.forEach(i -> {
            IntRange copy = IntRange.builder().floor(i.getFloor()).ceiling(i.getCeiling()).build();
            ranges.add(copy);
        });

        if (ranges.size() <= 1) {
            return ranges;
        }

        ranges.sort(Comparator.comparing(IntRange::getFloor));

        List<IntRange> result = new ArrayList<>();

        IntRange prev = ranges.get(0);
        for (int i = 1; i < ranges.size(); i++) {
            IntRange curr = ranges.get(i);

            // ceiling +1 can merge with next floor
            if (prev.getCeiling() + 1 >= curr.getFloor()) {

                Integer newCeiling = Math.max(prev.getCeiling(), curr.getCeiling());
                // merged case
                prev.setCeiling(newCeiling);
            } else {
                result.add(prev);
                prev = curr;
            }
        }

        result.add(prev);
        return result;
    }

    public static Set<IntRange> subtractFromSet(Set<IntRange> ranges, Integer i) {
        Set<Integer> all = new HashSet<>();
        for (IntRange range: ranges) {
            all.addAll(range.asSet());
        }
        all.remove(i);
        return fromSet(all);
    }

    public static Integer minFloor(Collection<IntRange> ranges) {
        Integer floor = Integer.MAX_VALUE;
        for (IntRange r: ranges) {
            if (r.getFloor() < floor) {
                floor = r.getFloor();
            }
        }
        return floor;
    }
    public static Integer maxCeil(Collection<IntRange> ranges) {
        Integer ceiling = Integer.MIN_VALUE;
        for (IntRange r: ranges) {
            if (r.getCeiling() > ceiling) {
                ceiling = r.getCeiling();
            }
        }
        return ceiling;
    }

    public static boolean setContains(Set<IntRange> rangeSet, Integer i) {

        for (IntRange range: rangeSet) {
            if (range.contains(i)) {
                return true;
            }
        }
        return false;
    }


    public static Integer leastInAll(Map<String, Set<IntRange>> rangeMapOfSets) {
        Set<IntRange> allRanges = new HashSet<>();
        for (Set<IntRange> rangeSet : rangeMapOfSets.values()) {
            allRanges.addAll(rangeSet);
        }

        Integer floor = minFloor(allRanges);
        Integer ceiling = maxCeil(allRanges);
        for (Integer i = floor; i <= ceiling; i++) {
            boolean containedInAll = true;
            for (Set<IntRange> rangeSet : rangeMapOfSets.values()) {
                if (!setContains(rangeSet, i)) {
                    containedInAll = false;
                }
            }
            if (containedInAll) {
                return i;
            }

        }
        return null;

    }

}
