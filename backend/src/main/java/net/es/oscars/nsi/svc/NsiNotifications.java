package net.es.oscars.nsi.svc;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.lib.soap.gen.nsi_2_0.connection.types.*;
import net.es.oscars.app.exc.NsiInternalException;
import net.es.oscars.nsi.beans.NsiNotification;
import net.es.oscars.nsi.db.NsiNotificationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
            // create XML unmarshallers for all the classes
            JAXBContext eetCtx = JAXBContext.newInstance(ErrorEventType.class);
            Unmarshaller eetUnm = eetCtx.createUnmarshaller();

            JAXBContext rtrtCtx = JAXBContext.newInstance(ReserveTimeoutRequestType.class);
            Unmarshaller rtrtUnm = rtrtCtx.createUnmarshaller();

            JAXBContext dpscrtCtx = JAXBContext.newInstance(DataPlaneStateChangeRequestType.class);
            Unmarshaller dpscrtUnm = dpscrtCtx.createUnmarshaller();

            // unmarshal each saved item into its class and add it to the result
            for (NsiNotification n : nsiNotifications) {
                switch (n.getType()) {
                    case ERROR_EVENT -> {
                        ErrorEventType eet = (ErrorEventType) eetUnm.unmarshal(new StringReader(n.getXml()));
                        qnct.getErrorEventOrReserveTimeoutOrDataPlaneStateChange().add(eet);

                    }
                    case RESERVE_TIMEOUT -> {
                        ReserveTimeoutRequestType rtrt = (ReserveTimeoutRequestType) rtrtUnm.unmarshal(new StringReader(n.getXml()));
                        qnct.getErrorEventOrReserveTimeoutOrDataPlaneStateChange().add(rtrt);

                    }
                    case DATAPLANE_STATE_CHANGE -> {
                        DataPlaneStateChangeRequestType dpscrt = (DataPlaneStateChangeRequestType) dpscrtUnm.unmarshal(new StringReader(n.getXml()));
                        qnct.getErrorEventOrReserveTimeoutOrDataPlaneStateChange().add(dpscrt);
                    }
                }
            }
        } catch (JAXBException e) {
            log.error("unmarshall error", e);
        }
        return qnct;


    }

}
