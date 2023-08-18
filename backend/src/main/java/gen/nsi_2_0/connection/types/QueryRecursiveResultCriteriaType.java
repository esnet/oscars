
package gen.nsi_2_0.connection.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import org.w3c.dom.Element;


/**
 * 
 *                 Type definition for the query recursive result containing
 *                 versioned reservation information and associated child
 *                 connection identifiers.
 *                 
 *                 Elements:
 *                 
 *                 schedule - Time parameters specifying the life of the service.
 *                 
 *                 serviceType - The specific service type of this reservation. 
 *                 This service type string maps into the list of supported service
 *                 definitions defined by the network providers.  In turn, the
 *                 service type specifies the specific service elements carried in
 *                 an instance of this type (through the ANY definition) that
 *                 are associated with the requested service.
 *                 
 *                 children - If this connection reservation is aggregating child
 *                 connections then this element contains detailed information
 *                 about the child connection segment.  The level of detail
 *                 include is left up to the individual NSA and their
 *                 authorization policies.
 * 
 *                 any - Provides a flexible mechanism allowing additional elements
 * 
 *                 to be provided such as the service specific attributes specified
 * 
 *                 by serviceType.  Additional use of this element field is beyond
 * 
 *                 the current scope of this NSI specification, but may be used in
 * 
 *                 the future to extend the existing protocol without requiring a
 * 
 *                 schema change.
 * 
 * 
 *                 Attributes:
 *                 
 *                 version - Version of the reservation instance.
 *                 
 *                 anyAttribute - Provides a flexible mechanism allowing additional
 * 
 *                 attributes to be provided as needed.  Use of this attribute field
 * 
 *                 is beyond the current scope of this NSI specification, but may be
 *                 used in the future to extend the existing protocol without
 * 
 *                 requiring a schema change.
 *             
 * 
 * <p>Java class for QueryRecursiveResultCriteriaType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="QueryRecursiveResultCriteriaType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="schedule" type="{http://schemas.ogf.org/nsi/2013/12/connection/types}ScheduleType"/&gt;
 *         &lt;element name="serviceType" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="children" type="{http://schemas.ogf.org/nsi/2013/12/connection/types}ChildRecursiveListType" minOccurs="0"/&gt;
 *         &lt;any processContents='lax' namespace='##other' maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="version" use="required" type="{http://www.w3.org/2001/XMLSchema}int" /&gt;
 *       &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QueryRecursiveResultCriteriaType", propOrder = {
    "schedule",
    "serviceType",
    "children",
    "any"
})
public class QueryRecursiveResultCriteriaType {

    @XmlElement(required = true)
    protected ScheduleType schedule;
    @XmlElement(required = true)
    protected String serviceType;
    protected ChildRecursiveListType children;
    @XmlAnyElement(lax = true)
    protected List<Object> any;
    @XmlAttribute(name = "version", required = true)
    protected int version;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    /**
     * Gets the value of the schedule property.
     * 
     * @return
     *     possible object is
     *     {@link ScheduleType }
     *     
     */
    public ScheduleType getSchedule() {
        return schedule;
    }

    /**
     * Sets the value of the schedule property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScheduleType }
     *     
     */
    public void setSchedule(ScheduleType value) {
        this.schedule = value;
    }

    /**
     * Gets the value of the serviceType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getServiceType() {
        return serviceType;
    }

    /**
     * Sets the value of the serviceType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setServiceType(String value) {
        this.serviceType = value;
    }

    /**
     * Gets the value of the children property.
     * 
     * @return
     *     possible object is
     *     {@link ChildRecursiveListType }
     *     
     */
    public ChildRecursiveListType getChildren() {
        return children;
    }

    /**
     * Sets the value of the children property.
     * 
     * @param value
     *     allowed object is
     *     {@link ChildRecursiveListType }
     *     
     */
    public void setChildren(ChildRecursiveListType value) {
        this.children = value;
    }

    /**
     * Gets the value of the any property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the any property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAny().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * {@link Element }
     * 
     * 
     */
    public List<Object> getAny() {
        if (any == null) {
            any = new ArrayList<Object>();
        }
        return this.any;
    }

    /**
     * Gets the value of the version property.
     * 
     */
    public int getVersion() {
        return version;
    }

    /**
     * Sets the value of the version property.
     * 
     */
    public void setVersion(int value) {
        this.version = value;
    }

    /**
     * Gets a map that contains attributes that aren't bound to any typed property on this class.
     * 
     * <p>
     * the map is keyed by the name of the attribute and 
     * the value is the string value of the attribute.
     * 
     * the map returned by this method is live, and you can add new attribute
     * by updating the map directly. Because of this design, there's no setter.
     * 
     * 
     * @return
     *     always non-null
     */
    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

}
