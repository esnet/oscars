
/**
 * Please modify this class to meet your needs
 * This class is not complete
 */

package gen.nsi_2_0.connection.provider;

import gen.nsi_2_0.connection.ifce.Error;
import gen.nsi_2_0.connection.ifce.ServiceException;
import gen.nsi_2_0.connection.types.*;
import gen.nsi_2_0.framework.headers.CommonHeaderType;

import java.util.logging.Logger;

/**
 * This class was generated by Apache CXF 3.2.0
 * 2017-09-28T14:21:43.524-07:00
 * Generated source version: 3.2.0
 * 
 */

@javax.jws.WebService(
                      serviceName = "ConnectionServiceProvider",
                      portName = "ConnectionServiceProviderPort",
                      targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/provider",
                      wsdlLocation = "file:/Users/haniotak/ij/oscars-newtech/nsi/src/main/resources/schema/ogf_nsi_connection_provider_v2_0.wsdl",
                      endpointInterface = "net.es.nsi.lib.soap.gen.nsi_2_0.connection.provider.ConnectionProviderPort")
                      
public class ConnectionServiceProviderPortImpl implements ConnectionProviderPort {

    private static final Logger LOG = Logger.getLogger(ConnectionServiceProviderPortImpl.class.getName());

    /* (non-Javadoc)
     * @see net.es.nsi.lib.soap.gen.nsi_2_0.connection.provider.ConnectionProviderPort#provision(net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericRequestType provision, net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType header)*
     */
    public GenericAcknowledgmentType provision(GenericRequestType provision, javax.xml.ws.Holder<CommonHeaderType> header) throws ServiceException {
        LOG.info("Executing operation provision");
        System.out.println(provision);
        System.out.println(header.value);
        try {
            GenericAcknowledgmentType _return = null;
            return _return;
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException("serviceException...");
    }

    /* (non-Javadoc)
     * @see net.es.nsi.lib.soap.gen.nsi_2_0.connection.provider.ConnectionProviderPort#querySummarySync(net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QueryType querySummarySync, net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType header)*
     */
    public QuerySummaryConfirmedType querySummarySync(QueryType querySummarySync, javax.xml.ws.Holder<CommonHeaderType> header) throws Error {
        LOG.info("Executing operation querySummarySync");
        System.out.println(querySummarySync);
        System.out.println(header.value);
        try {
            QuerySummaryConfirmedType _return = null;
            return _return;
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.Error("error...");
    }

    /* (non-Javadoc)
     * @see net.es.nsi.lib.soap.gen.nsi_2_0.connection.provider.ConnectionProviderPort#queryRecursive(net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QueryType queryRecursive, net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType header)*
     */
    public GenericAcknowledgmentType queryRecursive(QueryType queryRecursive, javax.xml.ws.Holder<CommonHeaderType> header) throws ServiceException {
        LOG.info("Executing operation queryRecursive");
        System.out.println(queryRecursive);
        System.out.println(header.value);
        try {
            GenericAcknowledgmentType _return = null;
            return _return;
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException("serviceException...");
    }

    /* (non-Javadoc)
     * @see net.es.nsi.lib.soap.gen.nsi_2_0.connection.provider.ConnectionProviderPort#reserveCommit(net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericRequestType reserveCommit, net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType header)*
     */
    public GenericAcknowledgmentType reserveCommit(GenericRequestType reserveCommit, javax.xml.ws.Holder<CommonHeaderType> header) throws ServiceException {
        LOG.info("Executing operation reserveCommit");
        System.out.println(reserveCommit);
        System.out.println(header.value);
        try {
            GenericAcknowledgmentType _return = null;
            return _return;
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException("serviceException...");
    }

    /* (non-Javadoc)
     * @see net.es.nsi.lib.soap.gen.nsi_2_0.connection.provider.ConnectionProviderPort#queryNotification(net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QueryNotificationType queryNotification, net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType header)*
     */
    public GenericAcknowledgmentType queryNotification(QueryNotificationType queryNotification, javax.xml.ws.Holder<CommonHeaderType> header) throws ServiceException {
        LOG.info("Executing operation queryNotification");
        System.out.println(queryNotification);
        System.out.println(header.value);
        try {
            GenericAcknowledgmentType _return = null;
            return _return;
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException("serviceException...");
    }

    /* (non-Javadoc)
     * @see net.es.nsi.lib.soap.gen.nsi_2_0.connection.provider.ConnectionProviderPort#terminate(net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericRequestType parameters, net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType header)*
     */
    public GenericAcknowledgmentType terminate(GenericRequestType parameters, javax.xml.ws.Holder<CommonHeaderType> header) throws ServiceException {
        LOG.info("Executing operation terminate");
        System.out.println(parameters);
        System.out.println(header.value);
        try {
            GenericAcknowledgmentType _return = null;
            return _return;
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException("serviceException...");
    }

    /* (non-Javadoc)
     * @see net.es.nsi.lib.soap.gen.nsi_2_0.connection.provider.ConnectionProviderPort#reserve(net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.ReserveType reserve, net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType header)*
     */
    public ReserveResponseType reserve(ReserveType reserve, javax.xml.ws.Holder<CommonHeaderType> header) throws ServiceException {
        LOG.info("Executing operation reserve");
        System.out.println(reserve);
        System.out.println(header.value);
        try {
            ReserveResponseType _return = null;
            return _return;
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException("serviceException...");
    }

    /* (non-Javadoc)
     * @see net.es.nsi.lib.soap.gen.nsi_2_0.connection.provider.ConnectionProviderPort#queryResultSync(net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QueryResultType queryResultSync, net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType header)*
     */
    public QueryResultConfirmedType queryResultSync(QueryResultType queryResultSync, javax.xml.ws.Holder<CommonHeaderType> header) throws Error {
        LOG.info("Executing operation queryResultSync");
        System.out.println(queryResultSync);
        System.out.println(header.value);
        try {
            QueryResultConfirmedType _return = null;
            return _return;
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.Error("error...");
    }

    /* (non-Javadoc)
     * @see net.es.nsi.lib.soap.gen.nsi_2_0.connection.provider.ConnectionProviderPort#release(net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericRequestType release, net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType header)*
     */
    public GenericAcknowledgmentType release(GenericRequestType release, javax.xml.ws.Holder<CommonHeaderType> header) throws ServiceException {
        LOG.info("Executing operation release");
        System.out.println(release);
        System.out.println(header.value);
        try {
            GenericAcknowledgmentType _return = null;
            return _return;
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException("serviceException...");
    }

    /* (non-Javadoc)
     * @see net.es.nsi.lib.soap.gen.nsi_2_0.connection.provider.ConnectionProviderPort#reserveAbort(net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.GenericRequestType reserveAbort, net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType header)*
     */
    public GenericAcknowledgmentType reserveAbort(GenericRequestType reserveAbort, javax.xml.ws.Holder<CommonHeaderType> header) throws ServiceException {
        LOG.info("Executing operation reserveAbort");
        System.out.println(reserveAbort);
        System.out.println(header.value);
        try {
            GenericAcknowledgmentType _return = null;
            return _return;
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException("serviceException...");
    }

    /* (non-Javadoc)
     * @see net.es.nsi.lib.soap.gen.nsi_2_0.connection.provider.ConnectionProviderPort#querySummary(net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QueryType querySummary, net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType header)*
     */
    public GenericAcknowledgmentType querySummary(QueryType querySummary, javax.xml.ws.Holder<CommonHeaderType> header) throws ServiceException {
        LOG.info("Executing operation querySummary");
        System.out.println(querySummary);
        System.out.println(header.value);
        try {
            GenericAcknowledgmentType _return = null;
            return _return;
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException("serviceException...");
    }

    /* (non-Javadoc)
     * @see net.es.nsi.lib.soap.gen.nsi_2_0.connection.provider.ConnectionProviderPort#queryResult(net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QueryResultType queryResult, net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType header)*
     */
    public GenericAcknowledgmentType queryResult(QueryResultType queryResult, javax.xml.ws.Holder<CommonHeaderType> header) throws ServiceException {
        LOG.info("Executing operation queryResult");
        System.out.println(queryResult);
        System.out.println(header.value);
        try {
            GenericAcknowledgmentType _return = null;
            return _return;
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.ServiceException("serviceException...");
    }

    /* (non-Javadoc)
     * @see net.es.nsi.lib.soap.gen.nsi_2_0.connection.provider.ConnectionProviderPort#queryNotificationSync(net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.QueryNotificationType queryNotificationSync, net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType header)*
     */
    public QueryNotificationConfirmedType queryNotificationSync(QueryNotificationType queryNotificationSync, javax.xml.ws.Holder<CommonHeaderType> header) throws Error {
        LOG.info("Executing operation queryNotificationSync");
        System.out.println(queryNotificationSync);
        System.out.println(header.value);
        try {
            QueryNotificationConfirmedType _return = null;
            return _return;
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new net.es.nsi.lib.soap.gen.nsi_2_0.connection.ifce.Error("error...");
    }

}