
package net.es.nsi.lib.soap.gen.nsi_2_0.connection.types;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * 
 *                 Type definition for the QueryNotification message providing a
 *                 mechanism for a Requester NSA to query a Provider NSA for a
 *                 set of notifications against a specific connectionId.
 *                 
 *                 Elements compose a filter for specifying the notifications to
 *                 return in response to the query operation.  The filter query
 *                 provides an inclusive range of notification identifiers based
 *                 on connectionId.
 *                 
 *                 Elements:
 *                 
 *                 connectionId - Notifications for this connectionId.
 *                 
 *                 startNotificationId - The start of the range of notificationIds
 *                 to return.  If not present then the query should start from
 *                 oldest notificationId available.
 *                 
 *                 endNotificationId - The end of the range of notificationIds
 *                 to return.  If not present then the query should end with
 *                 the newest notificationId available.
 *             
 * 
 * <p>Java class for QueryNotificationType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="QueryNotificationType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="connectionId" type="{http://schemas.ogf.org/nsi/2013/12/framework/types}ConnectionIdType"/&gt;
 *         &lt;element name="startNotificationId" type="{http://schemas.ogf.org/nsi/2013/12/connection/types}NotificationIdType" minOccurs="0"/&gt;
 *         &lt;element name="endNotificationId" type="{http://schemas.ogf.org/nsi/2013/12/connection/types}NotificationIdType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QueryNotificationType", propOrder = {
    "connectionId",
    "startNotificationId",
    "endNotificationId"
})
public class QueryNotificationType {

    @XmlElement(required = true)
    protected String connectionId;
    protected Long startNotificationId;
    protected Long endNotificationId;

    /**
     * Gets the value of the connectionId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * Sets the value of the connectionId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConnectionId(String value) {
        this.connectionId = value;
    }

    /**
     * Gets the value of the startNotificationId property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getStartNotificationId() {
        return startNotificationId;
    }

    /**
     * Sets the value of the startNotificationId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setStartNotificationId(Long value) {
        this.startNotificationId = value;
    }

    /**
     * Gets the value of the endNotificationId property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getEndNotificationId() {
        return endNotificationId;
    }

    /**
     * Sets the value of the endNotificationId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setEndNotificationId(Long value) {
        this.endNotificationId = value;
    }

}
