
package net.es.nsi.lib.soap.gen.nsi_2_0.connection.types;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * 
 *                 The reserveTimeout message is an autonomous message issued from
 *                 a PA to an RA when a timeout on an existing reserve request
 *                 occurs, and the PA has freed any uncommitted resources
 *                 associated with the reservation. This type of event is
 *                 originated from an uPA managing network resources associated
 *                 with the reservation, and propagated up the request tree to the
 *                 originating uRA.  An aggregator NSA (performing both a PA and
 *                 RA role) will map the received connectionId into a context
 *                 understood by its direct parent RA in the request tree, then
 *                 propagate the event upwards.  The originating connectionId and
 *                 uPA are provided in separate elements to maintain the original
 *                 context generating the timeout.  The timeoutValue and
 *                 timeStamp are populated by the originating uPA and propagated
 *                 up the tree untouched by intermediate NSA.
 *                 
 *                 Elements:
 *                 
 *                 connectionId - The Provider NSA assigned connectionId that this
 *                 notification is against.
 *                 
 *                 notificationId - A notification identifier that is unique in the
 *                 context of a connectionId.  This is a linearly increasing
 *                 identifier that can be used for ordering notifications in the
 *                 context of the connectionId.
 *                 
 *                 timeStamp - Time the event was generated on the originating NSA.
 *                 
 *                 timeoutValue - The timeout value in seconds that expired this
 *                 reservation.
 *                 
 *                 originatingConnectionId - The connectionId that triggered the
 *                 reserve timeout.
 *                 
 *                 originatingNSA - The NSA originating the timeout event.
 *             
 * 
 * <p>Java class for ReserveTimeoutRequestType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ReserveTimeoutRequestType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://schemas.ogf.org/nsi/2013/12/connection/types}NotificationBaseType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="timeoutValue" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="originatingConnectionId" type="{http://schemas.ogf.org/nsi/2013/12/framework/types}ConnectionIdType"/&gt;
 *         &lt;element name="originatingNSA" type="{http://schemas.ogf.org/nsi/2013/12/framework/types}NsaIdType"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReserveTimeoutRequestType", propOrder = {
    "timeoutValue",
    "originatingConnectionId",
    "originatingNSA"
})
public class ReserveTimeoutRequestType
    extends NotificationBaseType
{

    protected int timeoutValue;
    @XmlElement(required = true)
    protected String originatingConnectionId;
    @XmlElement(required = true)
    @XmlSchemaType(name = "anyURI")
    protected String originatingNSA;

    /**
     * Gets the value of the timeoutValue property.
     * 
     */
    public int getTimeoutValue() {
        return timeoutValue;
    }

    /**
     * Sets the value of the timeoutValue property.
     * 
     */
    public void setTimeoutValue(int value) {
        this.timeoutValue = value;
    }

    /**
     * Gets the value of the originatingConnectionId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOriginatingConnectionId() {
        return originatingConnectionId;
    }

    /**
     * Sets the value of the originatingConnectionId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOriginatingConnectionId(String value) {
        this.originatingConnectionId = value;
    }

    /**
     * Gets the value of the originatingNSA property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOriginatingNSA() {
        return originatingNSA;
    }

    /**
     * Sets the value of the originatingNSA property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOriginatingNSA(String value) {
        this.originatingNSA = value;
    }

}
