
package gen.nsi_2_0.connection.types;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 This is the type definition for the queryRecursiveConfirmed
 *                 message.  An NSA sends this positive queryRecursiveRequest
 *                 response to the NSA that issued the original request message.
 *                 
 *                 Elements:
 *                 
 *                 reservation - Resulting recursive set of connection reservations
 *                 matching the query criteria.
 *                 
 *                 If there were no matches to the query then no reservation
 *                 elements will be present.
 *             
 * 
 * <p>Java class for QueryRecursiveConfirmedType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="QueryRecursiveConfirmedType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="reservation" type="{http://schemas.ogf.org/nsi/2013/12/connection/types}QueryRecursiveResultType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QueryRecursiveConfirmedType", propOrder = {
    "reservation"
})
public class QueryRecursiveConfirmedType {

    protected List<QueryRecursiveResultType> reservation;

    /**
     * Gets the value of the reservation property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
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
     * {@link QueryRecursiveResultType }
     * 
     * 
     */
    public List<QueryRecursiveResultType> getReservation() {
        if (reservation == null) {
            reservation = new ArrayList<QueryRecursiveResultType>();
        }
        return this.reservation;
    }

}
