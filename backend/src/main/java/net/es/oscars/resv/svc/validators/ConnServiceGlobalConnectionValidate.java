package net.es.oscars.resv.svc.validators;

import lombok.Getter;
import lombok.Setter;
import net.es.oscars.web.simple.SimpleConnection;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ConnServiceGlobalConnectionValidate implements Validator, ValidatorWithErrors {
    Map<String, Errors> allErrors = new HashMap<>();
    private Integer minMtu;
    private Integer maxMtu;

    private boolean isConnectionIdValid;
    private boolean isConnectionMtuValid;
    private boolean isDescriptionValid;

    public ConnServiceGlobalConnectionValidate(Integer minMtu, Integer maxMtu) {
        this.minMtu = minMtu;
        this.maxMtu = maxMtu;

        this.clear();
    }

    /**
     * @param clazz
     * @return
     */
    @Override
    public boolean supports(Class<?> clazz) {
        return false;
    }

    /**
     * @param target
     * @param errors
     */
    @Override
    public void validate(Object target, Errors errors) {

        this.clear();

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "connectionId", null, "null connection id");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "connection_mtu", null, "MTU must be between " + minMtu + " and " + maxMtu + " (inclusive)");


        SimpleConnection inConn = (SimpleConnection) target;

        isConnectionIdValid = checkConnectionId(inConn, errors);
        isConnectionMtuValid = checkMtu(inConn, errors);
        isDescriptionValid = checkDescription(inConn, errors);

    }

    public boolean checkConnectionId(SimpleConnection inConn, Errors errors) {
        String connectionId = inConn.getConnectionId();
        boolean checkedConnectionId = false;

        // check the connection ID BEGIN
        if (
            connectionId.matches("^[a-zA-Z][a-zA-Z0-9_\\-]+$")
            && connectionId.length() >= 4
            && connectionId.length() <= 12
        ) {
            checkedConnectionId = true;
        } else {
            if (connectionId.length() > 12) {
                errors.rejectValue("connectionId", null, "connection id too long");
            } else if (connectionId.length() < 4) {
                errors.rejectValue("connectionId", null, "connection id too short");
            } else if (!connectionId.matches("^[a-zA-Z][a-zA-Z0-9_\\-]+$")) {
                errors.rejectValue("connectionId", null, "connection id invalid format");
            }
        }
        // check the connection ID END
        if (errors.hasErrors()) {
            allErrors.put("connectionId", errors);
        }
        return checkedConnectionId;
    }

    public boolean checkMtu(SimpleConnection inConn, Errors errors) {
        Integer connectionMtu = inConn.getConnection_mtu();
        boolean checkedMtu = false;

        // check the connection MTU BEGIN
        if (connectionMtu >= minMtu && connectionMtu <= maxMtu) {
            checkedMtu = true;

        } else {
            errors.rejectValue("connection_mtu", null, "MTU must be between " + minMtu + " and " + maxMtu + " (inclusive)");
        }
        // check the connection MTU END
        if (errors.hasErrors()) {
            allErrors.put("connection_mtu", errors);
        }
        return checkedMtu;
    }

    public boolean checkDescription(SimpleConnection inConn, Errors errors) {
        String description = inConn.getDescription();
        boolean checkedDescription = false;

        // check description BEGIN
        if (!description.isEmpty()) {
            checkedDescription = true;
        } else {
            errors.rejectValue("description", null, "null description");
        }
        // check description END
        if (errors.hasErrors()) {
            allErrors.put("connection description", errors);
        }
        return checkedDescription;
    }

    public boolean valid() {
        return isConnectionIdValid && isConnectionMtuValid && isDescriptionValid;
    }

    public void clear() {
        isConnectionIdValid = false;
        isConnectionMtuValid = false;
        isDescriptionValid = false;

        allErrors.clear();
    }

    public boolean hasErrors() {
        return !allErrors.isEmpty();
    }
}
