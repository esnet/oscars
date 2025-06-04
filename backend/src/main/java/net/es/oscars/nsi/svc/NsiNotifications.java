package net.es.oscars.nsi.svc;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.*;
import net.es.oscars.app.exc.NsiInternalException;
import net.es.oscars.nsi.beans.NsiNotification;
import net.es.oscars.nsi.db.NsiNotificationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.*;

@Component
@Slf4j
public class NsiNotifications {

    private final NsiNotificationRepository nsiNtfRepo;

    public NsiNotifications(NsiNotificationRepository nsiNtfRepo) {
        this.nsiNtfRepo = nsiNtfRepo;
    }

    @Transactional
    public void save(NsiNotification nsiNotification) throws NsiInternalException {
        this.nsiNtfRepo.save(nsiNotification);
    }

    @Transactional
    public QueryNotificationConfirmedType queryNotificationSync(QueryNotificationType queryNotificationSync) {
        String connId = queryNotificationSync.getConnectionId();
        QueryNotificationConfirmedType qnct = new QueryNotificationConfirmedType();
        // should never happen
        if (connId.isEmpty()) {
            return qnct;
        }
        Long startNotificationId = queryNotificationSync.getStartNotificationId();
        Long endNotificationId = queryNotificationSync.getEndNotificationId();

        List<NsiNotification> nsiNotifications = nsiNtfRepo.findByConnectionId(connId);
        if (startNotificationId != null) {
            nsiNotifications = nsiNotifications.stream().filter(ntf -> ntf.getNotificationId().compareTo(startNotificationId) >= 0).toList();
        }
        if (endNotificationId != null) {
            nsiNotifications = nsiNotifications.stream().filter(ntf -> ntf.getNotificationId().compareTo(endNotificationId) <= 0).toList();
        }

        try {

            JAXBContext context = JAXBContext.newInstance(NotificationBaseType.class);
            Unmarshaller unm = context.createUnmarshaller();

            // unmarshal each saved item into its class and add it to the result

            for (NsiNotification n : nsiNotifications) {
                XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(n.getXml()));
                switch (n.getType()) {
                    case ERROR_EVENT -> {
                        log.info("unmarshalling error event");
                        JAXBElement<ErrorEventType> jaxbElement = unm.unmarshal(reader, ErrorEventType.class);
                        qnct.getErrorEventOrReserveTimeoutOrDataPlaneStateChange().add(jaxbElement.getValue());
                    }
                    case RESERVE_TIMEOUT -> {
                        log.info("unmarshalling reserve timeout");
                        JAXBElement<ReserveTimeoutRequestType> jaxbElement = unm.unmarshal(reader, ReserveTimeoutRequestType.class);
                        qnct.getErrorEventOrReserveTimeoutOrDataPlaneStateChange().add(jaxbElement.getValue());
                    }
                    case DATAPLANE_STATE_CHANGE -> {
                        log.info("unmarshalling dataplane state change");
                        JAXBElement<DataPlaneStateChangeRequestType> jaxbElement = unm.unmarshal(reader, DataPlaneStateChangeRequestType.class);
                        qnct.getErrorEventOrReserveTimeoutOrDataPlaneStateChange().add(jaxbElement.getValue());
                    }
                }
            }
        } catch (JAXBException e) {
            log.error("unmarshall error", e);
        } catch (XMLStreamException e) {
            log.error("XMLStreamException error", e);
        }
        return qnct;


    }

}
