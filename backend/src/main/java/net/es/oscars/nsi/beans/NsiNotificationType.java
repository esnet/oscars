package net.es.oscars.nsi.beans;

public enum NsiNotificationType {
    ERROR_EVENT ("errorEvent"),
    RESERVE_TIMEOUT("reserveTimeout"),
    DATAPLANE_STATE_CHANGE ("dataPlaneStateChange"),
    MESSAGE_DELIVERY_TIMEOUT("messageDeliveryTimeout");




    private String code;
    NsiNotificationType(String code) {
        this.code = code;
    }
    public String toString() {
        return code;
    }
}
