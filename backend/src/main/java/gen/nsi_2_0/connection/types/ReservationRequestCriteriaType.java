
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
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import org.w3c.dom.Element;


/**
 * 
 *                 Type definition for a reservation and modification request
 *                 criteria.  Only those values requiring change are specified in
 *                 the modify request.  The version value specified in a
 *                 reservation or modify request must be a positive integer
 *                 larger than the previous version number.  A version value of
 *                 zero is a special number indicating an allocated but not yet
 *                 reserved reservation and cannot be specified by the RA.
 *                 
 *                 Elements:
 *                 
 *                 schedule - Time parameters specifying the life of the service.
 *                 If not present then the service to start immediately and run
 *                 for an infinite time.
 *                 
 *                 serviceType - Specific service type being requested in the
 *                 reservation.  This service type string maps into the list
 *                 of supported service descriptions defined by the network
 *                 providers, and in turn, to the specific service elements
 *                 carried in this element (through the ANY definition) required
 *                 to specify the requested service.  The service type is
 *                 mandatory in the original reserve request, and optional in a
 *                 reserve issued to modify an existing reservation.
 *                 
 *                 any - Provides a flexible mechanism allowing additional elements
 *                 to be provided such as the service specific attributes specified
 *                 by serviceType.  Additional use of this element field is beyond
 *                 the current scope of this NSI specification, but may be used in
 *                 the future to extend the existing protocol without requiring a
 *                 schema change.
 *                 
 *                 Attributes:
 *                 
 *                 version - The version number assigned by the RA to this
 *                 reservation instance.  If not specified in the initial
 *                 reservation request, the new reservation will default to one
 *                 for the first version, however, an initial request can specify
 *                 any positive integer except zero.  Each further reservation request
 *                 on an existing reservation (a modify operation), will be assigned a
 *                 linear increasing number, either specified by the RA, or
 *                 assigned by the PA if not specified.
 *                 
 *                 anyAttribute - Provides a flexible mechanism allowing additional
 *                 attributes to be provided as needed.  Use of this attribute field
 *                 is beyond the current scope of this NSI specification, but may be
 *                 used in the future to extend the existing protocol without
 *                 requiring a schema change.
 *             
 * 
 * <p>Java class for ReservationRequestCriteriaType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ReservationRequestCriteriaType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="schedule" type="{http://schemas.ogf.org/nsi/2013/12/connection/types}ScheduleType" minOccurs="0"/&gt;
 *         &lt;element name="serviceType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;any processContents='lax' namespace='##other' maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="version" type="{http://www.w3.org/2001/XMLSchema}int" /&gt;
 *       &lt;anyAttribute processContents='lax' namespace='##other'/&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReservationRequestCriteriaType", propOrder = {
    "schedule",
    "serviceType",
    "any"
})
public class ReservationRequestCriteriaType {

    protected ScheduleType schedule;
    protected String serviceType;
    @XmlAnyElement(lax = true)
    protected List<Object> any;
    @XmlAttribute(name = "version")
    protected Integer version;
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
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * Sets the value of the version property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setVersion(Integer value) {
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
