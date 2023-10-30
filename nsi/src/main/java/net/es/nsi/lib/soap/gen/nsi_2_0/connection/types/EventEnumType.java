
package net.es.nsi.lib.soap.gen.nsi_2_0.connection.types;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for EventEnumType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>
 * &lt;simpleType name="EventEnumType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="activateFailed"/&gt;
 *     &lt;enumeration value="deactivateFailed"/&gt;
 *     &lt;enumeration value="dataplaneError"/&gt;
 *     &lt;enumeration value="forcedEnd"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "EventEnumType")
@XmlEnum
public enum EventEnumType {

    @XmlEnumValue("activateFailed")
    ACTIVATE_FAILED("activateFailed"),
    @XmlEnumValue("deactivateFailed")
    DEACTIVATE_FAILED("deactivateFailed"),
    @XmlEnumValue("dataplaneError")
    DATAPLANE_ERROR("dataplaneError"),
    @XmlEnumValue("forcedEnd")
    FORCED_END("forcedEnd");
    private final String value;

    EventEnumType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static EventEnumType fromValue(String v) {
        for (EventEnumType c: EventEnumType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
