package net.es.oscars.app.exc;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.es.oscars.nsi.beans.NsiErrors;

@Data
@EqualsAndHashCode(callSuper=false)
public class NsiStateException extends NsiException {
    private NsiErrors error;

    public NsiStateException(String msg, NsiErrors error) {
        super(msg, error);
    }
    public NsiStateException() {super(); }

}
