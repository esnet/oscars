
package net.es.nsi.lib.soap.gen.nsi_2_0.connection.types;

import javax.xml.datatype.XMLGregorianCalendar;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlType;


/**
 * 
 *              A type definition modelling the reservation schedule start and end
 *              time parameters.
 * 
 *             Elements:
 * 
 *             startTime - The start time of the reservation.
 * 
 *             A start time of "now" is represented by a nil value in the
 *             startTime element.  For backwards compatibility an absent
 *             startTime element in the initial reserve message also represents
 *             a start time of "now".
 * 
 *             An absent startTime element in a modification operation indicates
 *             there is no change to startTime.  A startTime element with a nil
 *             value within a modify request represents a modification of
 *             startTime to "now".
 * 
 *             If a reserve request has a startTime in the past it should be
 *             considered as a start time of "now".
 * 
 *             endTime - The end time of the reservation.
 * 
 *             An "indefinite" end time is represented by a nil value in the
 *             endTime element.  For backwards compatibility an absent endTime
 *             element in the inital reserve message also represents an
 *             "indefinite" end time.
 * 
 *             An absent endTime element in a modification operation indicates
 *             there is no change to endTime.  An endTime element with a nil value
 *             within a modify request represents a modification of endTime to
 *             "indefinite".
 * 
 *             If a reserve request has a endTime in the past it should be
 *             considered as an invalid reservation request.
 *          
 * 
 * <p>Java class for ScheduleType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ScheduleType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="startTime" type="{http://schemas.ogf.org/nsi/2013/12/framework/types}DateTimeType" minOccurs="0"/&gt;
 *         &lt;element name="endTime" type="{http://schemas.ogf.org/nsi/2013/12/framework/types}DateTimeType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ScheduleType", propOrder = {
    "startTime",
    "endTime"
})
public class ScheduleType {

    @XmlElementRef(name = "startTime", type = JAXBElement.class, required = false)
    protected JAXBElement<XMLGregorianCalendar> startTime;
    @XmlElementRef(name = "endTime", type = JAXBElement.class, required = false)
    protected JAXBElement<XMLGregorianCalendar> endTime;

    /**
     * Gets the value of the startTime property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}
     *     
     */
    public JAXBElement<XMLGregorianCalendar> getStartTime() {
        return startTime;
    }

    /**
     * Sets the value of the startTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}
     *     
     */
    public void setStartTime(JAXBElement<XMLGregorianCalendar> value) {
        this.startTime = value;
    }

    /**
     * Gets the value of the endTime property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}
     *     
     */
    public JAXBElement<XMLGregorianCalendar> getEndTime() {
        return endTime;
    }

    /**
     * Sets the value of the endTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}
     *     
     */
    public void setEndTime(JAXBElement<XMLGregorianCalendar> value) {
        this.endTime = value;
    }

}
