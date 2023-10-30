
package net.es.nsi.lib.soap.gen.nsi_2_0.connection.types;

import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * 
 *                 Type definition for the querySummary message providing a mechanism
 *                 for a Requester NSA to query a Provider NSA for a set of connection
 *                 service reservation instances between the RA-PA pair. This message
 *                 can also be used as a status polling mechanism.
 *                 
 *                 Elements compose a filter for specifying the reservations to return
 *                 in response to the query operation. Supports the querying of reservations
 *                 based on connectionId or globalReservationId. Filter items specified
 *                 are OR'ed to build the match criteria. If no criteria are specified
 *                 then all reservations associated with the requesting NSA are returned.
 * 
 *                 Elements:
 * 
 *                 connectionId - Return reservations containing this connectionId.
 * 
 *                 globalReservationId - Return reservations containing this
 *                 globalReservationId.
 *                 
 *                 ifModifiedSince - If an NSA receives a querySummary or querySummarySync
 *                 message containing this element, then the NSA only returns those
 *                 reservations matching the filter elements (connectionId,
 *                 globalReservationId) if the reservation has been created, modified, or
 *                 has undergone a change since the specified ifModifiedSince time.  This
 *                 includes user initiated actions such as provision and release, as well
 *                 as state changes caused by events such as dataPlaneStateChange
 *                 notifications (in dataPlaneStatus).
 *             
 * 
 * <p>Java class for QueryType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="QueryType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;choice&gt;
 *           &lt;element name="connectionId" type="{http://schemas.ogf.org/nsi/2013/12/framework/types}ConnectionIdType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *           &lt;element name="globalReservationId" type="{http://schemas.ogf.org/nsi/2013/12/connection/types}GlobalReservationIdType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *           &lt;element name="ifModifiedSince" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/&gt;
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
@XmlType(name = "QueryType", propOrder = {
    "connectionId",
    "globalReservationId",
    "ifModifiedSince"
})
public class QueryType {

    protected List<String> connectionId;
    @XmlSchemaType(name = "anyURI")
    protected List<String> globalReservationId;
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar ifModifiedSince;

    /**
     * Gets the value of the connectionId property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the connectionId property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getConnectionId().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getConnectionId() {
        if (connectionId == null) {
            connectionId = new ArrayList<String>();
        }
        return this.connectionId;
    }

    /**
     * Gets the value of the globalReservationId property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the globalReservationId property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGlobalReservationId().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getGlobalReservationId() {
        if (globalReservationId == null) {
            globalReservationId = new ArrayList<String>();
        }
        return this.globalReservationId;
    }

    /**
     * Gets the value of the ifModifiedSince property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getIfModifiedSince() {
        return ifModifiedSince;
    }

    /**
     * Sets the value of the ifModifiedSince property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setIfModifiedSince(XMLGregorianCalendar value) {
        this.ifModifiedSince = value;
    }

}
