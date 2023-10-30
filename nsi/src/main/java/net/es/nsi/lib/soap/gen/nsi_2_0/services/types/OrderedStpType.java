
package net.es.nsi.lib.soap.gen.nsi_2_0.services.types;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * 
 *                 A Service Termination Point (STP) that can be ordered in a list for
 *                 use in PathObject definition.
 *                 
 *                 Attributes:
 *                 
 *                 order - Order attribute is provided only when the STP is part of an
 *                 orderedStpList.
 *                 
 *                 Elements:
 *                 
 *                 stp - The Service Termination Point (STP).
 *             
 * 
 * <p>Java class for OrderedStpType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="OrderedStpType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="stp" type="{http://schemas.ogf.org/nsi/2013/12/services/types}StpIdType"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="order" use="required" type="{http://www.w3.org/2001/XMLSchema}int" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OrderedStpType", propOrder = {
    "stp"
})
public class OrderedStpType {

    @XmlElement(required = true)
    protected String stp;
    @XmlAttribute(name = "order", required = true)
    protected int order;

    /**
     * Gets the value of the stp property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStp() {
        return stp;
    }

    /**
     * Sets the value of the stp property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStp(String value) {
        this.stp = value;
    }

    /**
     * Gets the value of the order property.
     * 
     */
    public int getOrder() {
        return order;
    }

    /**
     * Sets the value of the order property.
     * 
     */
    public void setOrder(int value) {
        this.order = value;
    }

}
