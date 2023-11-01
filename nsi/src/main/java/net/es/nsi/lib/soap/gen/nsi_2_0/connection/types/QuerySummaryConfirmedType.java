
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
 *                 This is the type definition for the querySummaryConfirmed message
 *                 (both synchronous and asynchronous versions). An NSA sends this
 *                 positive querySummaryRequest response to the NSA that issued the
 *                 original request message.  There can be zero or more results
 *                 returned in this confirmed message depending on the number of
 *                 matching reservation results.
 * 
 *                 Elements:
 * 
 *                 reservation - Resulting summary set of connection reservations
 *                 matching the query criteria.
 * 
 *                 If there were no matches to the query then no reservation
 *                 elements will be present.
 *                 
 *                 lastModified - Includes the update time of the most recently
 *                 created/modified/updated reservation on the system. The lastModified
 *                 element is included even if the request did not include an
 *                 ifModifiedSince element, and if the response does not contain any
 *                 reservation results.  This lastModified value can be used in the next
 *                 query for this filter.  The lastModified element will only be absent
 *                 if the NSA does not support the ifModifiedSince capability.
 *             
 * 
 * <p>Java class for QuerySummaryConfirmedType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="QuerySummaryConfirmedType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="reservation" type="{http://schemas.ogf.org/nsi/2013/12/connection/types}QuerySummaryResultType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="lastModified" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuerySummaryConfirmedType", propOrder = {
    "reservation",
    "lastModified"
})
public class QuerySummaryConfirmedType {

    protected List<QuerySummaryResultType> reservation;
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar lastModified;

    /**
     * Gets the value of the reservation property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the reservation property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getReservation().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link QuerySummaryResultType }
     * 
     * 
     */
    public List<QuerySummaryResultType> getReservation() {
        if (reservation == null) {
            reservation = new ArrayList<QuerySummaryResultType>();
        }
        return this.reservation;
    }

    /**
     * Gets the value of the lastModified property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getLastModified() {
        return lastModified;
    }

    /**
     * Sets the value of the lastModified property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setLastModified(XMLGregorianCalendar value) {
        this.lastModified = value;
    }

}
