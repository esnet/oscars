package net.es.oscars.app.exc;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.es.oscars.nsi.beans.NsiErrors;

@Data
@EqualsAndHashCode(callSuper=false)
public class NsiValidationException extends Exception {
    private NsiErrors error;

    public NsiValidationException(String msg, NsiErrors error) {
        super(msg);
        this.error = error;
    }
    public NsiValidationException() {super(); }

}
