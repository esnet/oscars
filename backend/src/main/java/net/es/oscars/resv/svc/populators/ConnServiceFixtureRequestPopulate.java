package net.es.oscars.resv.svc.populators;

import lombok.Getter;
import lombok.Setter;
import net.es.oscars.web.simple.Fixture;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.*;

@Getter
@Setter
public class ConnServiceFixtureRequestPopulate implements Populator {

    Map<String, Errors> allErrors = new HashMap<>() ;
    List<Fixture> sourceList;

    Map<String, ImmutablePair<Integer, Integer>> inBwMap;
    Map<String, Set<Integer>> inVlanMap;

    boolean isValid = true;

    public ConnServiceFixtureRequestPopulate(
        List<Fixture> sourceList,
        Map<String, ImmutablePair<Integer, Integer>> inBwMap,
        Map<String, Set<Integer>> inVlanMap
    ) {
        this.sourceList = sourceList;
        this.inBwMap = inBwMap;
        this.inVlanMap = inVlanMap;
    }
    /**
     *
     */
    @Override
    public void populate() {
        setValid(false);
        allErrors.clear();

        for (Fixture fixture : sourceList) {
            Integer inMbps = fixture.getInMbps();
            Integer outMbps = fixture.getOutMbps();
            Errors errors = new BeanPropertyBindingResult(fixture, "fixture");

            // In/Out Mbps
            if (fixture.getMbps() != null) {
                inMbps = fixture.getMbps();
                outMbps = fixture.getMbps();
            }

            // BW Map
            if (inBwMap.containsKey(fixture.getPort())) {
                ImmutablePair<Integer, Integer> prevBw = inBwMap.get(fixture.getPort());
                inMbps += prevBw.getLeft();
                outMbps += prevBw.getRight();
                ImmutablePair<Integer, Integer> newBw = new ImmutablePair<>(inMbps, outMbps);
                inBwMap.put(fixture.getPort(), newBw);
            } else {
                inBwMap.put(fixture.getPort(), new ImmutablePair<>(inMbps, outMbps));
            }

            // Collect VLANs
            Set<Integer> vlans = new HashSet<>();
            if (inVlanMap.containsKey(fixture.getPort())) {
                vlans = inVlanMap.get(fixture.getPort());
            }

            if (!vlans.contains(fixture.getVlan())) {
                vlans.add(fixture.getVlan());
            } else {
                errors.rejectValue("vlan", null, fixture.getVlan() + " requested twice on " + fixture.getPort());
                // No longer valid!
                setValid(false);
            }

            inVlanMap.put(fixture.getPort(), vlans);
            if (errors.hasErrors()) {
                // Keep track of validation errors
                allErrors.put(fixture.getVlan() + ":" + fixture.getPort(), errors);
            }
        }

        if (!hasErrors()) {
            setValid(true);
        }
    }

    public boolean hasErrors() {
        return !allErrors.isEmpty();
    }
}
