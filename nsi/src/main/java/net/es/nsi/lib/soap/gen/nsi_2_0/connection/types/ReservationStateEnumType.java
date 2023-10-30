
package net.es.nsi.lib.soap.gen.nsi_2_0.connection.types;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ReservationStateEnumType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>
 * &lt;simpleType name="ReservationStateEnumType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="ReserveStart"/&gt;
 *     &lt;enumeration value="ReserveChecking"/&gt;
 *     &lt;enumeration value="ReserveFailed"/&gt;
 *     &lt;enumeration value="ReserveAborting"/&gt;
 *     &lt;enumeration value="ReserveHeld"/&gt;
 *     &lt;enumeration value="ReserveCommitting"/&gt;
 *     &lt;enumeration value="ReserveTimeout"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "ReservationStateEnumType")
@XmlEnum
public enum ReservationStateEnumType {

    @XmlEnumValue("ReserveStart")
    RESERVE_START("ReserveStart"),
    @XmlEnumValue("ReserveChecking")
    RESERVE_CHECKING("ReserveChecking"),
    @XmlEnumValue("ReserveFailed")
    RESERVE_FAILED("ReserveFailed"),
    @XmlEnumValue("ReserveAborting")
    RESERVE_ABORTING("ReserveAborting"),
    @XmlEnumValue("ReserveHeld")
    RESERVE_HELD("ReserveHeld"),
    @XmlEnumValue("ReserveCommitting")
    RESERVE_COMMITTING("ReserveCommitting"),
    @XmlEnumValue("ReserveTimeout")
    RESERVE_TIMEOUT("ReserveTimeout");
    private final String value;

    ReservationStateEnumType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ReservationStateEnumType fromValue(String v) {
        for (ReservationStateEnumType c: ReservationStateEnumType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
