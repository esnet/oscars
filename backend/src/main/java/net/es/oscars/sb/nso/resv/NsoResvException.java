package net.es.oscars.sb.nso.resv;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class NsoResvException extends Exception {

    public NsoResvException(String msg) {
        super(msg);
    }
    public NsoResvException() {super(); }

}
