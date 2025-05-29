package net.es.oscars.resv.svc.comparators;

import ch.qos.logback.core.spi.ErrorCodes;
import lombok.Getter;
import lombok.Setter;
import net.es.oscars.topo.beans.PortBwVlan;
import net.es.oscars.web.simple.Fixture;
import net.es.oscars.web.simple.Validity;
import net.es.topo.common.model.oscars1.IntRange;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.*;

@Getter
@Setter
public class ConnServiceVlanToAvailableCompare implements Comparator {


    Map<String, Errors> allErrors = new HashMap<>() ;
    List<Fixture> sourceList;

    Map<String, PortBwVlan> availBwVlanMap;
    Map<String, Set<Integer>> inVlanMap;

    boolean isValid = false;

    public ConnServiceVlanToAvailableCompare(
        List<Fixture> sourceList,
        Map<String, PortBwVlan> availBwVlanMap,
        Map<String, Set<Integer>> inVlanMap
    ) {
        this.sourceList = sourceList;
        this.availBwVlanMap = availBwVlanMap;
        this.inVlanMap = inVlanMap;
    }

    /**
     *
     */
    @Override
    public void compare() {
        setValid(false);
        allErrors.clear();

        for (Fixture f : sourceList) {
            // TODO: Do we actually use these Validity objects anywhere?
            Validity fv = Validity.builder()
                .valid(true)
                .message("")
                .build();

            Errors errors = new BeanPropertyBindingResult(f, "fixture");

            if (availBwVlanMap.containsKey(f.getPort())) {
                PortBwVlan avail = availBwVlanMap.get(f.getPort());
                Set<Integer> vlans = inVlanMap.get(f.getPort());
                if (vlans == null) {
                    vlans = new HashSet<>();
                }

                Set<IntRange> availVlanRanges = avail.getVlanRanges();
                for (Integer vlan : vlans) {
                    boolean atLeastOneContains = false;
                    String errorMessage = "";

                    for (IntRange r : availVlanRanges) {
                        if (r.contains(vlan)) {
                            atLeastOneContains = true;
                        }
                    }
                    if (atLeastOneContains) {
                        setValid(true);
                    } else {
                        errorMessage = f.getPort() + " : vlan " + f.getVlan() + " not available";
                        errors.rejectValue("vlans", null, errorMessage);
                        fv.setMessage(errorMessage);
                        fv.setValid(false);
                    }

//                    log.debug(f.getPort() + " vlan " + vlan + " contained in " + IntRange.asString(availVlanRanges) + " ? " + atLeastOneContains);
                }
            } else {
                fv.setValid(false);
                fv.setMessage(f.getPort() + " not in topology\n");

                errors.rejectValue("port", null, f.getPort() + " not in topology");
                isValid = false;
            }
            if (errors.hasErrors()) {
                allErrors.put(f.getPort() + " (vlans)", errors);
            }
            f.setValidity(fv);
        }

        if (!hasErrors()) {
            setValid(true);
        }
    }

    public boolean hasErrors() {
        return !allErrors.isEmpty();
    }
}
