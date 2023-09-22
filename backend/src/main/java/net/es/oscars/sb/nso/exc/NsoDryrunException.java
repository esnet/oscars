package net.es.oscars.sb.nso.exc;

public class NsoDryrunException extends Exception {
    public NsoDryrunException(String errorMessage) {
        super(errorMessage);
    }
}
