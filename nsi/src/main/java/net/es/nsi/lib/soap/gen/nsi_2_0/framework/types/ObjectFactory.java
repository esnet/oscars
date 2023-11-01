
package net.es.nsi.lib.soap.gen.nsi_2_0.framework.types;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the net.es.nsi.lib.soap.gen.nsi_2_0.framework.types package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _ServiceException_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/framework/types", "serviceException");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: net.es.nsi.lib.soap.gen.nsi_2_0.framework.types
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ServiceExceptionType }
     * 
     */
    public ServiceExceptionType createServiceExceptionType() {
        return new ServiceExceptionType();
    }

    /**
     * Create an instance of {@link VariablesType }
     * 
     */
    public VariablesType createVariablesType() {
        return new VariablesType();
    }

    /**
     * Create an instance of {@link TypeValuePairListType }
     * 
     */
    public TypeValuePairListType createTypeValuePairListType() {
        return new TypeValuePairListType();
    }

    /**
     * Create an instance of {@link TypeValuePairType }
     * 
     */
    public TypeValuePairType createTypeValuePairType() {
        return new TypeValuePairType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ServiceExceptionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ServiceExceptionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/framework/types", name = "serviceException")
    public JAXBElement<ServiceExceptionType> createServiceException(ServiceExceptionType value) {
        return new JAXBElement<ServiceExceptionType>(_ServiceException_QNAME, ServiceExceptionType.class, null, value);
    }

}
