package net.es.oscars.nso.exc;

public class NsoDryrunException extends Exception {
    public NsoDryrunException(String errorMessage) {
        super(errorMessage);
    }
}
