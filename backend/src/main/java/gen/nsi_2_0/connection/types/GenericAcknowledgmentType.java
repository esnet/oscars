
package gen.nsi_2_0.connection.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 Common acknowledgment message type.
 *                 
 *                 Elements:
 *                 We have moved the correlationId to the header so this is
 *                 now an empty response.
 *                 
 *                 Notes on acknowledgment:
 *                 Depending on NSA implementation and thread timing an
 *                 acknowledgment to a request operation may be returned
 *                 after the confirm/fail for the request has been returned
 *                 to the Requesting NSA.
 *                 
 *                 For protocol robustness, Requesting NSA should be
 *                 able to accept confirm/fail before acknowledgment.
 *             
 * 
 * <p>Java class for GenericAcknowledgmentType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="GenericAcknowledgmentType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GenericAcknowledgmentType")
public class GenericAcknowledgmentType {


}
