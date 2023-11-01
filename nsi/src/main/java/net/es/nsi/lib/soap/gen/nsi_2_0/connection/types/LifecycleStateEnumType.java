
package net.es.nsi.lib.soap.gen.nsi_2_0.connection.types;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for LifecycleStateEnumType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <pre>
 * &lt;simpleType name="LifecycleStateEnumType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Created"/&gt;
 *     &lt;enumeration value="Failed"/&gt;
 *     &lt;enumeration value="PassedEndTime"/&gt;
 *     &lt;enumeration value="Terminating"/&gt;
 *     &lt;enumeration value="Terminated"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "LifecycleStateEnumType")
@XmlEnum
public enum LifecycleStateEnumType {

    @XmlEnumValue("Created")
    CREATED("Created"),
    @XmlEnumValue("Failed")
    FAILED("Failed"),
    @XmlEnumValue("PassedEndTime")
    PASSED_END_TIME("PassedEndTime"),
    @XmlEnumValue("Terminating")
    TERMINATING("Terminating"),
    @XmlEnumValue("Terminated")
    TERMINATED("Terminated");
    private final String value;

    LifecycleStateEnumType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static LifecycleStateEnumType fromValue(String v) {
        for (LifecycleStateEnumType c: LifecycleStateEnumType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
