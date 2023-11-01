
package net.es.nsi.lib.soap.gen.nsi_2_0.connection.types;

import javax.xml.datatype.XMLGregorianCalendar;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * 
 *                 A QueryResultResponseType type containing a single operation result
 *                 matching the specified query criteria.
 *                 
 *                 Elements:
 *                 
 *                 resultId - A result identifier that is unique in the context of a
 *                 connectionId.  This is a linearly increasing identifier that can
 *                 be used for sequencing results in the order in which they were
 *                 generated in the context of the connectionId.
 *                 
 *                 correlationId - The correlationId corresponding to the operation
 *                 result as would have been returned in the NSI header element when
 *                 this result was returned to the RA.
 *                 
 *                 timeStamp - The time this result was generated.
 *                 
 *                 Choice of one of the following:
 *                 
 *                 reserveConfirmed - Reserve operation confirmation.
 *                 
 *                 reserveFailed - Reserve operation failure.
 *                 
 *                 reserveCommitConfirmed - Reserve commit operation confirmation.
 *                 
 *                 reserveCommitFailed - Reserve commit operation failure.
 *                 
 *                 reserveAbortConfirmed - Reserve abort operation confirmation.
 *                 
 *                 provisionConfirmed - Provision operation confirmation.
 *                 
 *                 releaseConfirmed - Release operation confirmation.
 *                 
 *                 terminateConfirmed - Terminate confirmation.
 *                 
 *                 error - Error response message.
 *             
 * 
 * <p>Java class for QueryResultResponseType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="QueryResultResponseType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="resultId" type="{http://schemas.ogf.org/nsi/2013/12/connection/types}ResultIdType"/&gt;
 *         &lt;element name="correlationId" type="{http://schemas.ogf.org/nsi/2013/12/framework/types}UuidType"/&gt;
 *         &lt;element name="timeStamp" type="{http://schemas.ogf.org/nsi/2013/12/framework/types}DateTimeType"/&gt;
 *         &lt;choice&gt;
 *           &lt;element ref="{http://schemas.ogf.org/nsi/2013/12/connection/types}reserveConfirmed"/&gt;
 *           &lt;element ref="{http://schemas.ogf.org/nsi/2013/12/connection/types}reserveFailed"/&gt;
 *           &lt;element ref="{http://schemas.ogf.org/nsi/2013/12/connection/types}reserveCommitConfirmed"/&gt;
 *           &lt;element ref="{http://schemas.ogf.org/nsi/2013/12/connection/types}reserveCommitFailed"/&gt;
 *           &lt;element ref="{http://schemas.ogf.org/nsi/2013/12/connection/types}reserveAbortConfirmed"/&gt;
 *           &lt;element ref="{http://schemas.ogf.org/nsi/2013/12/connection/types}provisionConfirmed"/&gt;
 *           &lt;element ref="{http://schemas.ogf.org/nsi/2013/12/connection/types}releaseConfirmed"/&gt;
 *           &lt;element ref="{http://schemas.ogf.org/nsi/2013/12/connection/types}terminateConfirmed"/&gt;
 *           &lt;element ref="{http://schemas.ogf.org/nsi/2013/12/connection/types}error"/&gt;
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
@XmlType(name = "QueryResultResponseType", propOrder = {
    "resultId",
    "correlationId",
    "timeStamp",
    "reserveConfirmed",
    "reserveFailed",
    "reserveCommitConfirmed",
    "reserveCommitFailed",
    "reserveAbortConfirmed",
    "provisionConfirmed",
    "releaseConfirmed",
    "terminateConfirmed",
    "error"
})
public class QueryResultResponseType {

    protected long resultId;
    @XmlElement(required = true)
    @XmlSchemaType(name = "anyURI")
    protected String correlationId;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar timeStamp;
    @XmlElement(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
    protected ReserveConfirmedType reserveConfirmed;
    @XmlElement(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
    protected GenericFailedType reserveFailed;
    @XmlElement(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
    protected GenericConfirmedType reserveCommitConfirmed;
    @XmlElement(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
    protected GenericFailedType reserveCommitFailed;
    @XmlElement(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
    protected GenericConfirmedType reserveAbortConfirmed;
    @XmlElement(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
    protected GenericConfirmedType provisionConfirmed;
    @XmlElement(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
    protected GenericConfirmedType releaseConfirmed;
    @XmlElement(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
    protected GenericConfirmedType terminateConfirmed;
    @XmlElement(namespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
    protected GenericErrorType error;

    /**
     * Gets the value of the resultId property.
     * 
     */
    public long getResultId() {
        return resultId;
    }

    /**
     * Sets the value of the resultId property.
     * 
     */
    public void setResultId(long value) {
        this.resultId = value;
    }

    /**
     * Gets the value of the correlationId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Sets the value of the correlationId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCorrelationId(String value) {
        this.correlationId = value;
    }

    /**
     * Gets the value of the timeStamp property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    /**
     * Sets the value of the timeStamp property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setTimeStamp(XMLGregorianCalendar value) {
        this.timeStamp = value;
    }

    /**
     * Gets the value of the reserveConfirmed property.
     * 
     * @return
     *     possible object is
     *     {@link ReserveConfirmedType }
     *     
     */
    public ReserveConfirmedType getReserveConfirmed() {
        return reserveConfirmed;
    }

    /**
     * Sets the value of the reserveConfirmed property.
     * 
     * @param value
     *     allowed object is
     *     {@link ReserveConfirmedType }
     *     
     */
    public void setReserveConfirmed(ReserveConfirmedType value) {
        this.reserveConfirmed = value;
    }

    /**
     * Gets the value of the reserveFailed property.
     * 
     * @return
     *     possible object is
     *     {@link GenericFailedType }
     *     
     */
    public GenericFailedType getReserveFailed() {
        return reserveFailed;
    }

    /**
     * Sets the value of the reserveFailed property.
     * 
     * @param value
     *     allowed object is
     *     {@link GenericFailedType }
     *     
     */
    public void setReserveFailed(GenericFailedType value) {
        this.reserveFailed = value;
    }

    /**
     * Gets the value of the reserveCommitConfirmed property.
     * 
     * @return
     *     possible object is
     *     {@link GenericConfirmedType }
     *     
     */
    public GenericConfirmedType getReserveCommitConfirmed() {
        return reserveCommitConfirmed;
    }

    /**
     * Sets the value of the reserveCommitConfirmed property.
     * 
     * @param value
     *     allowed object is
     *     {@link GenericConfirmedType }
     *     
     */
    public void setReserveCommitConfirmed(GenericConfirmedType value) {
        this.reserveCommitConfirmed = value;
    }

    /**
     * Gets the value of the reserveCommitFailed property.
     * 
     * @return
     *     possible object is
     *     {@link GenericFailedType }
     *     
     */
    public GenericFailedType getReserveCommitFailed() {
        return reserveCommitFailed;
    }

    /**
     * Sets the value of the reserveCommitFailed property.
     * 
     * @param value
     *     allowed object is
     *     {@link GenericFailedType }
     *     
     */
    public void setReserveCommitFailed(GenericFailedType value) {
        this.reserveCommitFailed = value;
    }

    /**
     * Gets the value of the reserveAbortConfirmed property.
     * 
     * @return
     *     possible object is
     *     {@link GenericConfirmedType }
     *     
     */
    public GenericConfirmedType getReserveAbortConfirmed() {
        return reserveAbortConfirmed;
    }

    /**
     * Sets the value of the reserveAbortConfirmed property.
     * 
     * @param value
     *     allowed object is
     *     {@link GenericConfirmedType }
     *     
     */
    public void setReserveAbortConfirmed(GenericConfirmedType value) {
        this.reserveAbortConfirmed = value;
    }

    /**
     * Gets the value of the provisionConfirmed property.
     * 
     * @return
     *     possible object is
     *     {@link GenericConfirmedType }
     *     
     */
    public GenericConfirmedType getProvisionConfirmed() {
        return provisionConfirmed;
    }

    /**
     * Sets the value of the provisionConfirmed property.
     * 
     * @param value
     *     allowed object is
     *     {@link GenericConfirmedType }
     *     
     */
    public void setProvisionConfirmed(GenericConfirmedType value) {
        this.provisionConfirmed = value;
    }

    /**
     * Gets the value of the releaseConfirmed property.
     * 
     * @return
     *     possible object is
     *     {@link GenericConfirmedType }
     *     
     */
    public GenericConfirmedType getReleaseConfirmed() {
        return releaseConfirmed;
    }

    /**
     * Sets the value of the releaseConfirmed property.
     * 
     * @param value
     *     allowed object is
     *     {@link GenericConfirmedType }
     *     
     */
    public void setReleaseConfirmed(GenericConfirmedType value) {
        this.releaseConfirmed = value;
    }

    /**
     * Gets the value of the terminateConfirmed property.
     * 
     * @return
     *     possible object is
     *     {@link GenericConfirmedType }
     *     
     */
    public GenericConfirmedType getTerminateConfirmed() {
        return terminateConfirmed;
    }

    /**
     * Sets the value of the terminateConfirmed property.
     * 
     * @param value
     *     allowed object is
     *     {@link GenericConfirmedType }
     *     
     */
    public void setTerminateConfirmed(GenericConfirmedType value) {
        this.terminateConfirmed = value;
    }

    /**
     * Gets the value of the error property.
     * 
     * @return
     *     possible object is
     *     {@link GenericErrorType }
     *     
     */
    public GenericErrorType getError() {
        return error;
    }

    /**
     * Sets the value of the error property.
     * 
     * @param value
     *     allowed object is
     *     {@link GenericErrorType }
     *     
     */
    public void setError(GenericErrorType value) {
        this.error = value;
    }

}
