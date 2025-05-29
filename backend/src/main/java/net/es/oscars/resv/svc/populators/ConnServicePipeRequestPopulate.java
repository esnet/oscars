package net.es.oscars.resv.svc.populators;

import lombok.Getter;
import lombok.Setter;
import net.es.oscars.web.simple.Fixture;
import net.es.oscars.web.simple.Pipe;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.validation.Errors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class ConnServicePipeRequestPopulate implements Populator {
    Map<String, Errors> allErrors = new HashMap<>() ;
    List<Pipe> sourceList;

    Map<String, ImmutablePair<Integer, Integer>> inBwMap;

    boolean isValid = true;

    public ConnServicePipeRequestPopulate(
        List<Pipe> sourceList,
        Map<String, ImmutablePair<Integer, Integer>> inBwMap
    ) {
        this.sourceList = sourceList;
        this.inBwMap = inBwMap;
    }
    /**
     *
     */
    @Override
    public void populate() {
        setValid(false);
        allErrors.clear();

        // @TODO: Audit here. What conditions may cause an invalid state?
        for (Pipe p : sourceList) {
            Integer azMbps = p.getAzMbps();
            Integer zaMbps = p.getZaMbps();
            if (p.getMbps() != null) {
                azMbps = p.getMbps();
                zaMbps = p.getMbps();
            }
            int i = 0;
            for (String urn : p.getEro()) {
                // egress for a-z, ingress for z-a
                Integer egr = azMbps;
                Integer ing = zaMbps;
                boolean notDevice = false;
                if (i % 3 == 1) {
                    notDevice = true;
                } else if (i % 3 == 2) {
                    ing = azMbps;
                    egr = zaMbps;
                    notDevice = true;
                }
                if (notDevice) {
                    if (inBwMap.containsKey(urn)) {
                        ImmutablePair<Integer, Integer> prevBw = inBwMap.get(urn);
                        ImmutablePair<Integer, Integer> newBw =
                            ImmutablePair.of(ing + prevBw.getLeft(), egr + prevBw.getRight());
                        inBwMap.put(urn, newBw);
                    } else {
                        ImmutablePair<Integer, Integer> newBw = ImmutablePair.of(ing, egr);
                        inBwMap.put(urn, newBw);
                    }
                }
                i++;
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
