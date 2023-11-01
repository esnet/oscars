
package net.es.nsi.lib.soap.gen.nsi_2_0.services.types;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * 
 *                 A simple ordered list type of Service Termination Point (STP). List
 *                 order is determined by the integer order attribute in the orderedSTP
 *                 element.
 *                 
 *                 Elements:
 *                 
 *                 orderedSTP - A list of STP ordered 0..n by their integer order attribute.
 *             
 * 
 * <p>Java class for StpListType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="StpListType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="orderedSTP" type="{http://schemas.ogf.org/nsi/2013/12/services/types}OrderedStpType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StpListType", propOrder = {
    "orderedSTP"
})
public class StpListType {

    protected List<OrderedStpType> orderedSTP;

    /**
     * Gets the value of the orderedSTP property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the orderedSTP property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOrderedSTP().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link OrderedStpType }
     * 
     * 
     */
    public List<OrderedStpType> getOrderedSTP() {
        if (orderedSTP == null) {
            orderedSTP = new ArrayList<OrderedStpType>();
        }
        return this.orderedSTP;
    }

}
