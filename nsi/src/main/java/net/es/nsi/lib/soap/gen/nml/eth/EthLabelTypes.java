//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.06.19 at 01:27:41 PM PDT 
//


package net.es.nsi.lib.soap.gen.nml.eth;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for LabelTypes.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="LabelTypes"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="http://schemas.ogf.org/nml/2012/10/ethernet#vid"/&gt;
 *     &lt;enumeration value="http://schemas.ogf.org/nml/2012/10/ethernet#vlan"/&gt;
 *     &lt;enumeration value="http://schemas.ogf.org/nml/2012/10/ethernet#stag"/&gt;
 *     &lt;enumeration value="http://schemas.ogf.org/nml/2012/10/ethernet#ctag"/&gt;
 *     &lt;enumeration value="http://schemas.ogf.org/nml/2012/10/ethernet#btag"/&gt;
 *     &lt;enumeration value="http://schemas.ogf.org/nml/2012/10/ethernet#isid"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "LabelTypes")
@XmlEnum
public enum EthLabelTypes {

    @XmlEnumValue("http://schemas.ogf.org/nml/2012/10/ethernet#vid")
    HTTP_SCHEMAS_OGF_ORG_NML_2012_10_ETHERNET_VID("http://schemas.ogf.org/nml/2012/10/ethernet#vid"),
    @XmlEnumValue("http://schemas.ogf.org/nml/2012/10/ethernet#vlan")
    HTTP_SCHEMAS_OGF_ORG_NML_2012_10_ETHERNET_VLAN("http://schemas.ogf.org/nml/2012/10/ethernet#vlan"),
    @XmlEnumValue("http://schemas.ogf.org/nml/2012/10/ethernet#stag")
    HTTP_SCHEMAS_OGF_ORG_NML_2012_10_ETHERNET_STAG("http://schemas.ogf.org/nml/2012/10/ethernet#stag"),
    @XmlEnumValue("http://schemas.ogf.org/nml/2012/10/ethernet#ctag")
    HTTP_SCHEMAS_OGF_ORG_NML_2012_10_ETHERNET_CTAG("http://schemas.ogf.org/nml/2012/10/ethernet#ctag"),
    @XmlEnumValue("http://schemas.ogf.org/nml/2012/10/ethernet#btag")
    HTTP_SCHEMAS_OGF_ORG_NML_2012_10_ETHERNET_BTAG("http://schemas.ogf.org/nml/2012/10/ethernet#btag"),
    @XmlEnumValue("http://schemas.ogf.org/nml/2012/10/ethernet#isid")
    HTTP_SCHEMAS_OGF_ORG_NML_2012_10_ETHERNET_ISID("http://schemas.ogf.org/nml/2012/10/ethernet#isid");
    private final String value;

    EthLabelTypes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static EthLabelTypes fromValue(String v) {
        for (EthLabelTypes c: EthLabelTypes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
