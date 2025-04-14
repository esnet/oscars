package net.es.oscars.app.exc;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.es.oscars.nsi.beans.NsiErrors;

@Data
@EqualsAndHashCode(callSuper=false)
public class NsiInternalException extends NsiException {
    private NsiErrors error;

    public NsiInternalException(String msg, NsiErrors error) {
        super(msg, error);
    }
    public NsiInternalException() {super(); }

}
