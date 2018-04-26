
package net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.2.0
 * 2017-09-28T14:21:44.536-07:00
 * Generated source version: 3.2.0
 */

@WebFault(name = "serviceException", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/types")
public class ServiceException extends Exception {
    
    private net.es.nsi.lib.soap.gen.nsi_2_0.framework.types.ServiceExceptionType serviceException;

    public ServiceException() {
        super();
    }
    
    public ServiceException(String message) {
        super(message);
    }
    
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceException(String message, net.es.nsi.lib.soap.gen.nsi_2_0.framework.types.ServiceExceptionType serviceException) {
        super(message);
        this.serviceException = serviceException;
    }

    public ServiceException(String message, net.es.nsi.lib.soap.gen.nsi_2_0.framework.types.ServiceExceptionType serviceException, Throwable cause) {
        super(message, cause);
        this.serviceException = serviceException;
    }

    public net.es.nsi.lib.soap.gen.nsi_2_0.framework.types.ServiceExceptionType getFaultInfo() {
        return this.serviceException;
    }
}