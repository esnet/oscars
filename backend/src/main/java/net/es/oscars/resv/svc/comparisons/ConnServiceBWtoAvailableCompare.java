package net.es.oscars.resv.svc.comparisons;

import lombok.Getter;
import lombok.Setter;
import net.es.oscars.topo.beans.PortBwVlan;
import net.es.oscars.web.simple.Validity;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ConnServiceBWtoAvailableCompare implements Comparison {

    Map<String, ImmutablePair<Integer, Integer>> inBwMap;
    Map<String, PortBwVlan> availBwVlanMap;

    Map<String, Validity> urnInBwValid;
    Map<String, Validity> urnEgBwValid;

    Map<String, Errors> allErrors = new HashMap<>() ;

    boolean isValid = false;

    public ConnServiceBWtoAvailableCompare(
        Map<String, ImmutablePair<Integer, Integer>> inBwMap,
        Map<String, PortBwVlan> availBwVlanMap,
        Map<String, Validity> urnInBwValid,
        Map<String, Validity> urnEgBwValid)
    {
        this.inBwMap = inBwMap;
        this.availBwVlanMap = availBwVlanMap;
        this.urnInBwValid = urnInBwValid;
        this.urnEgBwValid = urnEgBwValid;
    }
    /**
     *
     */
    @Override
    public void compare() {
        setValid(false);
        allErrors.clear();

        for (String urn : inBwMap.keySet()) {
            PortBwVlan avail = availBwVlanMap.get(urn);
            Errors errorsInBw = new BeanPropertyBindingResult(inBwMap, "inBwMap");
            Errors errorsEgBw = new BeanPropertyBindingResult(inBwMap, "inBwMap");

            if (avail == null) {
                Validity bwValid = Validity.builder()
                    .valid(false)
                    .message(urn + " is not present anymore")
                    .build();
                
                urnInBwValid.put(urn, bwValid);
                urnEgBwValid.put(urn, bwValid);

                errorsInBw.rejectValue("inBwMap", null, urn + " is not present anymore");
            } else {
                // @TODO: Do we actually use this Validity object?
                Validity inBwValid = Validity.builder()
                    .valid(true)
                    .message("")
                    .build();
                ImmutablePair<Integer, Integer> inBw = inBwMap.get(urn);

                if (avail.getIngressBandwidth() < inBw.getLeft()) {
                    String errorMessage = "total port ingress bw exceeds available: " + urn + " " + inBw.getLeft() + "(req) / " + avail.getIngressBandwidth() + " (avail)";
                    String inErr = errorMessage + "\n";

                    errorsInBw.rejectValue("urn", null, errorMessage);

                    inBwValid.setMessage(inErr);
                    inBwValid.setValid(false);
                }

                urnInBwValid.put(urn, inBwValid);

                Validity egBwValid = Validity.builder()
                    .valid(true)
                    .message("")
                    .build();

                if (avail.getEgressBandwidth() < inBw.getRight()) {
                    String errorMessage = "total port egress bw exceeds available: " +
                        urn + " " + inBw.getLeft() + "(req) / " +
                        avail.getIngressBandwidth() + " (avail)";

                    String egErr = errorMessage + "\n";

                    errorsEgBw.rejectValue("urn", null, errorMessage);
                    setValid(false);

                    egBwValid.setMessage(egErr);
                    egBwValid.setValid(false);
                }

                urnEgBwValid.put(urn, egBwValid);
            }

            if (errorsInBw.hasErrors()) {
                allErrors.put(urn + " (inBw.keySet() inBw)", errorsInBw);
            }

            if (errorsEgBw.hasErrors()) {
                allErrors.put(urn + " (inBw.keySet() egBw)", errorsEgBw);
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
