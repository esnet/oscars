
package gen.nsi_2_0.framework.types;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 Common service exception used for SOAP faults and Failed
 *                 message.
 *                 
 *                 Elements:
 *                 
 *                 nsaId - NSA that generated the service exception.
 *                 
 *                 connectionId - The connectionId associated with the reservation
 *                 impacted by this error.
 *                 
 *                 serviceType - The service type identifying the applicable
 *                 service description in the context of the NSA generating the
 *                 error.
 *                 
 *                 errorId - Error identifier uniquely identifying each known
 *                 fault within the protocol.  Acts as a parent functionality
 *                 classification for service specific errors.
 *                 
 *                 text - User friendly message text describing the error.
 *                 
 *                 variables - An  optional collection of type/value pairs providing
 *                 additional information relating to the error and feedback for
 *                 possible resolution.
 *                 
 *                 childException - Hierarchical list of service exceptions
 *                 capturing failures within the request tree.
 *             
 * 
 * <p>Java class for ServiceExceptionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ServiceExceptionType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="nsaId" type="{http://schemas.ogf.org/nsi/2013/12/framework/types}NsaIdType"/&gt;
 *         &lt;element name="connectionId" type="{http://schemas.ogf.org/nsi/2013/12/framework/types}ConnectionIdType" minOccurs="0"/&gt;
 *         &lt;element name="serviceType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="errorId" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="text" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="variables" type="{http://schemas.ogf.org/nsi/2013/12/framework/types}VariablesType" minOccurs="0"/&gt;
 *         &lt;element name="childException" type="{http://schemas.ogf.org/nsi/2013/12/framework/types}ServiceExceptionType" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ServiceExceptionType", propOrder = {
    "nsaId",
    "connectionId",
    "serviceType",
    "errorId",
    "text",
    "variables",
    "childException"
})
public class ServiceExceptionType {

    @XmlElement(required = true)
    @XmlSchemaType(name = "anyURI")
    protected String nsaId;
    protected String connectionId;
    protected String serviceType;
    @XmlElement(required = true)
    protected String errorId;
    @XmlElement(required = true)
    protected String text;
    protected VariablesType variables;
    protected List<ServiceExceptionType> childException;

    /**
     * Gets the value of the nsaId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNsaId() {
        return nsaId;
    }

    /**
     * Sets the value of the nsaId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNsaId(String value) {
        this.nsaId = value;
    }

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
     * Gets the value of the errorId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getErrorId() {
        return errorId;
    }

    /**
     * Sets the value of the errorId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setErrorId(String value) {
        this.errorId = value;
    }

    /**
     * Gets the value of the text property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the value of the text property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setText(String value) {
        this.text = value;
    }

    /**
     * Gets the value of the variables property.
     * 
     * @return
     *     possible object is
     *     {@link VariablesType }
     *     
     */
    public VariablesType getVariables() {
        return variables;
    }

    /**
     * Sets the value of the variables property.
     * 
     * @param value
     *     allowed object is
     *     {@link VariablesType }
     *     
     */
    public void setVariables(VariablesType value) {
        this.variables = value;
    }

    /**
     * Gets the value of the childException property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the childException property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getChildException().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ServiceExceptionType }
     * 
     * 
     */
    public List<ServiceExceptionType> getChildException() {
        if (childException == null) {
            childException = new ArrayList<ServiceExceptionType>();
        }
        return this.childException;
    }

}
