
package net.es.nsi.lib.soap.gen.nsi_2_0.services.types;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;


/**
 * 
 *                 This type definition is used to model pathfinding inclusions/
 *                 exclusions in a point-to-point service request.
 *                 
 *                 Inclusions provide pathfinders with a specific set of resources
 *                 to use in path computation.  Different from an ERO in that an
 *                 ERO provides a specific path through the network, while
 *                 inclusions specifies the starting set of resources to be used in
 *                 pathfinding (not all of the resources need be used).
 *                 
 *                 Exclusions provide a mechanism allowing an RA to specify a set of
 *                 resources that must be excluded when computing a path.
 *                 
 *                 If an inclusion(s) is present it is used to build the initial
 *                 routing graph, otherwise the complete set of resources are used.
 *                 
 *                 If an exclusion(s) is present then the specified exclusion(s) are
 *                 pruned from the graph.
 *                 
 *                 Any ERO is applied during pathfinding using the resulting graph.
 *                 
 *                 Elements:
 *                 
 *                 lt - less than contitional element.
 *                 lte - less than equal to conditional element.
 *                 gt - greater than conditional element.
 *                 gte - greater than equal to conditional eement.
 *                 eq - equal conditional element.
 *                 
 *                 Attributes:
 *                 type - A string representing the name of the parameter.     
 *             
 * 
 * <p>Java class for ClusionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ClusionType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;choice&gt;
 *           &lt;element name="lt" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *           &lt;element name="lte" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *         &lt;/choice&gt;
 *         &lt;choice&gt;
 *           &lt;element name="gt" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *           &lt;element name="gte" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/&gt;
 *         &lt;/choice&gt;
 *         &lt;element name="eq" type="{http://www.w3.org/2001/XMLSchema}anyType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="type" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ClusionType", propOrder = {
    "lt",
    "lte",
    "gt",
    "gte",
    "eq"
})
public class ClusionType {

    protected Object lt;
    protected Object lte;
    protected Object gt;
    protected Object gte;
    protected List<Object> eq;
    @XmlAttribute(name = "type", required = true)
    protected String type;

    /**
     * Gets the value of the lt property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getLt() {
        return lt;
    }

    /**
     * Sets the value of the lt property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setLt(Object value) {
        this.lt = value;
    }

    /**
     * Gets the value of the lte property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getLte() {
        return lte;
    }

    /**
     * Sets the value of the lte property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setLte(Object value) {
        this.lte = value;
    }

    /**
     * Gets the value of the gt property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getGt() {
        return gt;
    }

    /**
     * Sets the value of the gt property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setGt(Object value) {
        this.gt = value;
    }

    /**
     * Gets the value of the gte property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getGte() {
        return gte;
    }

    /**
     * Sets the value of the gte property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setGte(Object value) {
        this.gte = value;
    }

    /**
     * Gets the value of the eq property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a <CODE>set</CODE> method for the eq property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEq().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * 
     * 
     */
    public List<Object> getEq() {
        if (eq == null) {
            eq = new ArrayList<Object>();
        }
        return this.eq;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

}
