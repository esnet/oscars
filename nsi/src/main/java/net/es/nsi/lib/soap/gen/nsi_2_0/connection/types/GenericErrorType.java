
package net.es.nsi.lib.soap.gen.nsi_2_0.connection.types;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import net.es.nsi.lib.soap.gen.nsi_2_0.framework.types.ServiceExceptionType;


/**
 * 
 *                 A generic "Error" message type sent in response to a previous
 *                 protocol "Request" message.  An error message is generated when
 *                 an error condition occurs that does not result in a state
 *                 machine transition.  This type is used in response to all
 *                 request types that can return an error.
 *                 
 *                 The correlationId carried in the NSI header will identify the
 *                 original request associated with this error message.
 *                 
 *                 Elements:
 *                 
 *                 serviceException - Specific error condition indicating the
 *                 reason for the failure.
 *             
 * 
 * <p>Java class for GenericErrorType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="GenericErrorType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="serviceException" type="{http://schemas.ogf.org/nsi/2013/12/framework/types}ServiceExceptionType"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GenericErrorType", propOrder = {
    "serviceException"
})
public class GenericErrorType {

    @XmlElement(required = true)
    protected ServiceExceptionType serviceException;

    /**
     * Gets the value of the serviceException property.
     * 
     * @return
     *     possible object is
     *     {@link ServiceExceptionType }
     *     
     */
    public ServiceExceptionType getServiceException() {
        return serviceException;
    }

    /**
     * Sets the value of the serviceException property.
     * 
     * @param value
     *     allowed object is
     *     {@link ServiceExceptionType }
     *     
     */
    public void setServiceException(ServiceExceptionType value) {
        this.serviceException = value;
    }

}
