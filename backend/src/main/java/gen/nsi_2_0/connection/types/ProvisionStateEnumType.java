
package gen.nsi_2_0.connection.types;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ProvisionStateEnumType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ProvisionStateEnumType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="Released"/&gt;
 *     &lt;enumeration value="Provisioning"/&gt;
 *     &lt;enumeration value="Provisioned"/&gt;
 *     &lt;enumeration value="Releasing"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "ProvisionStateEnumType")
@XmlEnum
public enum ProvisionStateEnumType {

    @XmlEnumValue("Released")
    RELEASED("Released"),
    @XmlEnumValue("Provisioning")
    PROVISIONING("Provisioning"),
    @XmlEnumValue("Provisioned")
    PROVISIONED("Provisioned"),
    @XmlEnumValue("Releasing")
    RELEASING("Releasing");
    private final String value;

    ProvisionStateEnumType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ProvisionStateEnumType fromValue(String v) {
        for (ProvisionStateEnumType c: ProvisionStateEnumType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
