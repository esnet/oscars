package net.es.oscars.nso;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.es.oscars.nsi.beans.NsiErrors;

@Data
@EqualsAndHashCode(callSuper=false)
public class NsoResvException extends Exception {

    public NsoResvException(String msg) {
        super(msg);
    }
    public NsoResvException() {super(); }

}
