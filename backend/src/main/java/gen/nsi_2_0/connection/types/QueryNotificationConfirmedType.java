
package gen.nsi_2_0.connection.types;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 A query notification confirmation containing a list of notification
 *                 messages matching the specified query criteria.
 *                 
 *                 Elements:
 *                 
 *                 errorEvent - Error event notification.
 *                 
 *                 reserveTimeout - Reserve timeout notification.
 *                 
 *                 dataPlaneStateChange - A data plane state change notification.
 *                 
 *                 messageDeliveryTimeout - Message delivery timeout notification.
 *             
 * 
 * <p>Java class for QueryNotificationConfirmedType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="QueryNotificationConfirmedType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;choice maxOccurs="unbounded" minOccurs="0"&gt;
 *           &lt;element ref="{http://schemas.ogf.org/nsi/2013/12/connection/types}errorEvent"/&gt;
 *           &lt;element ref="{http://schemas.ogf.org/nsi/2013/12/connection/types}reserveTimeout"/&gt;
 *           &lt;element ref="{http://schemas.ogf.org/nsi/2013/12/connection/types}dataPlaneStateChange"/&gt;
 *           &lt;element ref="{http://schemas.ogf.org/nsi/2013/12/connection/types}messageDeliveryTimeout"/&gt;
 *         &lt;/choice&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QueryNotificationConfirmedType", propOrder = {
    "errorEventOrReserveTimeoutOrDataPlaneStateChange"
})
public class QueryNotificationConfirmedType {

    @XmlElements({
        @XmlElement(name = "errorEvent", namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", type = ErrorEventType.class),
        @XmlElement(name = "reserveTimeout", namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", type = ReserveTimeoutRequestType.class),
        @XmlElement(name = "dataPlaneStateChange", namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", type = DataPlaneStateChangeRequestType.class),
        @XmlElement(name = "messageDeliveryTimeout", namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types", type = MessageDeliveryTimeoutRequestType.class)
    })
    protected List<NotificationBaseType> errorEventOrReserveTimeoutOrDataPlaneStateChange;

    /**
     * Gets the value of the errorEventOrReserveTimeoutOrDataPlaneStateChange property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the errorEventOrReserveTimeoutOrDataPlaneStateChange property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getErrorEventOrReserveTimeoutOrDataPlaneStateChange().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ErrorEventType }
     * {@link ReserveTimeoutRequestType }
     * {@link DataPlaneStateChangeRequestType }
     * {@link MessageDeliveryTimeoutRequestType }
     * 
     * 
     */
    public List<NotificationBaseType> getErrorEventOrReserveTimeoutOrDataPlaneStateChange() {
        if (errorEventOrReserveTimeoutOrDataPlaneStateChange == null) {
            errorEventOrReserveTimeoutOrDataPlaneStateChange = new ArrayList<NotificationBaseType>();
        }
        return this.errorEventOrReserveTimeoutOrDataPlaneStateChange;
    }

}
