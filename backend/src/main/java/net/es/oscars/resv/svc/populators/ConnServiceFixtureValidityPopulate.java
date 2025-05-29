package net.es.oscars.resv.svc.populators;

import lombok.Getter;
import lombok.Setter;
import net.es.oscars.web.simple.Fixture;
import net.es.oscars.web.simple.Validity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ConnServiceFixtureValidityPopulate implements Populator {
    List<Fixture> sourceList;
    Map<String, Validity> urnInBwValid;
    Map<String, Validity> urnEgBwValid;

    Map<String, Errors> allErrors = new HashMap<>() ;
    boolean isValid = false;

    public ConnServiceFixtureValidityPopulate(
        List<Fixture> sourceList,
        Map<String, Validity> urnInBwValid,
        Map<String, Validity> urnEgBwValid
    ) {
        this.sourceList = sourceList;
        this.urnInBwValid = urnInBwValid;
        this.urnEgBwValid = urnEgBwValid;
    }
    /**
     *
     */
    @Override
    public void populate() {
        allErrors.clear();
        setValid(false);

        for (Fixture f : sourceList) {
            Validity inBwValid = urnInBwValid.get(f.getPort());
            boolean isBwValid = inBwValid.isValid();

            Errors errors = new BeanPropertyBindingResult(f, "fixture");

            if (!isBwValid) {
                String errorMessage = f.getValidity().getMessage() + inBwValid.getMessage();
                f.getValidity().setMessage(errorMessage);
                f.getValidity().setValid(false);

                errors.rejectValue("port", null, f.getPort() + " (Validity of urnInBw) is not valid");
            }

            Validity egBwValid = urnEgBwValid.get(f.getPort());
            boolean isEgValid = egBwValid.isValid();

            if (!isEgValid) {
                String errorMessage = f.getValidity().getMessage() + egBwValid.getMessage();
                f.getValidity().setValid(false);
                f.getValidity().setMessage(errorMessage);

                errors.rejectValue("port", null, f.getPort() + " (Validity of urnEgBw) is not valid");
            }

            if (errors.hasErrors()) {
                allErrors.put(f.getPort(), errors);
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
