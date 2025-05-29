package net.es.oscars.resv.svc.populators;

import lombok.Getter;
import lombok.Setter;
import net.es.oscars.web.simple.Fixture;
import net.es.oscars.web.simple.Pipe;
import net.es.oscars.web.simple.Validity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ConnServicePipeAndEroValidityPopulate implements Populator {
    Map<String, Errors> allErrors = new HashMap<>();

    List<Pipe> sourceList;
    Map<String, Validity> urnInBwValid;

    boolean isValid = false;

    public ConnServicePipeAndEroValidityPopulate(
        List<Pipe> sourceList,
        Map<String, Validity> urnInBwValid
    ) {
        this.sourceList = sourceList;
        this.urnInBwValid = urnInBwValid;
    }

    public void populate() {
        allErrors.clear();
        setValid(false);

        for (Pipe p : sourceList) {
            Validity pv = Validity.builder().valid(true).message("").build();
            Map<String, Validity> eroValidity = new HashMap<>();


            int i = 0;
            for (String urn : p.getEro()) {
                Errors errors = new BeanPropertyBindingResult(p, "pipe");
                boolean notDevice = false;
                if (i % 3 == 1) {
                    notDevice = true;
                } else if (i % 3 == 2) {
                    notDevice = true;
                }
                if (notDevice) {
                    Validity hopV = Validity.builder()
                        .message("")
                        .valid(true)
                        .build();

                    Validity inBwValid = urnInBwValid.get(urn);
                    if (!inBwValid.isValid()) {
                        hopV.setMessage(inBwValid.getMessage());
                        hopV.setValid(false);
                        pv.setMessage(pv.getMessage() + inBwValid.getMessage());
                        pv.setValid(false);

                        errors.rejectValue("urn", null, urn + " (Validity of inBwValid) is not valid");
                    }

                    Validity egBwValid = urnInBwValid.get(urn);
                    if (!egBwValid.isValid()) {
                        hopV.setMessage(hopV.getMessage() + inBwValid.getMessage());
                        hopV.setValid(false);
                        pv.setMessage(pv.getMessage() + inBwValid.getMessage());
                        pv.setValid(false);

                        errors.rejectValue("urn", null, urn + " (Validity of egBwValid) is not valid");
                    }
                    eroValidity.put(urn, hopV);
                }

                if (errors.hasErrors()) {
                    allErrors.put(urn, errors);
                }

                i++;
            }
            p.setValidity(pv);
            p.setEroValidity(eroValidity);

        }

        if (!hasErrors()) {
            setValid(true);
        }
    }

    public boolean hasErrors() { return !allErrors.isEmpty(); }
}
