
package gen.nsi_2_0.services.point2point;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;
import gen.nsi_2_0.services.types.TypeValueType;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the net.es.nsi.lib.soap.gen.nsi_2_0.services.point2point package. 
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

    private final static QName _P2Ps_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/services/point2point", "p2ps");
    private final static QName _Capacity_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/services/point2point", "capacity");
    private final static QName _Parameter_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/services/point2point", "parameter");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: net.es.nsi.lib.soap.gen.nsi_2_0.services.point2point
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link P2PServiceBaseType }
     * 
     */
    public P2PServiceBaseType createP2PServiceBaseType() {
        return new P2PServiceBaseType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link P2PServiceBaseType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/services/point2point", name = "p2ps")
    public JAXBElement<P2PServiceBaseType> createP2Ps(P2PServiceBaseType value) {
        return new JAXBElement<P2PServiceBaseType>(_P2Ps_QNAME, P2PServiceBaseType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Long }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/services/point2point", name = "capacity")
    public JAXBElement<Long> createCapacity(Long value) {
        return new JAXBElement<Long>(_Capacity_QNAME, Long.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TypeValueType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.ogf.org/nsi/2013/12/services/point2point", name = "parameter")
    public JAXBElement<TypeValueType> createParameter(TypeValueType value) {
        return new JAXBElement<TypeValueType>(_Parameter_QNAME, TypeValueType.class, null, value);
    }

}
