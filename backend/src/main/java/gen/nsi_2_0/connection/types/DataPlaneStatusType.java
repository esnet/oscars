
package gen.nsi_2_0.connection.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 Models the current connection activation state within the
 *                 data plane.
 * 
 *                 Elements:
 *                 
 *                 active - True if the data plane is active.  For an aggregator,
 *                 this flag is true when data plane is activated in all
 *                 participating children.
 *                 
 *                 version - Version of the connection reservation this entry is
 *                 modelling.
 *                 
 *                 versionConsistent - Always true for uPA. For an aggregator,
 *                 if version numbers of all children are the same. This flag is
 *                 true. This field is valid when Active is true.
 *             
 * 
 * <p>Java class for DataPlaneStatusType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DataPlaneStatusType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="active" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="version" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="versionConsistent" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DataPlaneStatusType", propOrder = {
    "active",
    "version",
    "versionConsistent"
})
public class DataPlaneStatusType {

    protected boolean active;
    protected int version;
    protected boolean versionConsistent;

    /**
     * Gets the value of the active property.
     * 
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets the value of the active property.
     * 
     */
    public void setActive(boolean value) {
        this.active = value;
    }

    /**
     * Gets the value of the version property.
     * 
     */
    public int getVersion() {
        return version;
    }

    /**
     * Sets the value of the version property.
     * 
     */
    public void setVersion(int value) {
        this.version = value;
    }

    /**
     * Gets the value of the versionConsistent property.
     * 
     */
    public boolean isVersionConsistent() {
        return versionConsistent;
    }

    /**
     * Sets the value of the versionConsistent property.
     * 
     */
    public void setVersionConsistent(boolean value) {
        this.versionConsistent = value;
    }

}
