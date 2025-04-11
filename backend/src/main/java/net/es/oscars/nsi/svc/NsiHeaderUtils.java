package net.es.oscars.nsi.svc;

import jakarta.xml.ws.Holder;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.lib.soap.gen.nsi_2_0.framework.headers.CommonHeaderType;
import net.es.oscars.app.exc.NsiInternalException;
import net.es.oscars.app.exc.NsiValidationException;
import net.es.oscars.nsi.beans.NsiErrors;
import net.es.oscars.nsi.db.NsiRequesterNSARepository;
import net.es.oscars.nsi.ent.NsiRequesterNSA;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class NsiHeaderUtils {
    @Value("#{'${nsi.allowed-requesters}'.split(',')}")
    private List<String> allowedRequesters;

    @Value("${nsi.provider-nsa}")
    private String providerNsa;

    final public static String DEFAULT_PROTOCOL_VERSION = "application/vdn.ogf.nsi.cs.v2.provider+soap";

    private final NsiRequesterNSARepository requesterNsaRepo;

    public NsiHeaderUtils(NsiRequesterNSARepository requesterNsaRepo) {
        this.requesterNsaRepo = requesterNsaRepo;
    }

    /* header processing */

    public void processHeader(CommonHeaderType inHeader) throws NsiValidationException, NsiInternalException {
        String error = "";
        boolean hasError = false;
        if (!inHeader.getProviderNSA().equals(providerNsa)) {
            hasError = true;
            error += "provider nsa does not match\n";
        }
        boolean isAllowed = false;
        for (String allowed : allowedRequesters) {
            if (allowed.equals(inHeader.getRequesterNSA())) {
                isAllowed = true;
                break;
            }
        }
        if (!isAllowed) {
            hasError = true;
            error += "requester nsa not in allowed list\n";
        }
        if (hasError) {
            throw new NsiValidationException(error, NsiErrors.SEC_ERROR);
        }
        this.updateRequester(inHeader.getReplyTo(), inHeader.getRequesterNSA());
    }

    public void updateRequester(String replyTo, String nsaId) throws NsiInternalException {
        List<NsiRequesterNSA> requesters = requesterNsaRepo.findByNsaId(nsaId);
        if (requesters.isEmpty()) {
            log.info("saving new requester nsa: " + nsaId);
            NsiRequesterNSA requesterNSA = NsiRequesterNSA.builder()
                    .callbackUrl(replyTo)
                    .nsaId(nsaId)
                    .build();
            requesterNsaRepo.save(requesterNSA);
        } else if (requesters.size() >= 2) {
            throw new NsiInternalException("multiple requester entries for " + nsaId, NsiErrors.NRM_ERROR);
        } else {
            NsiRequesterNSA requesterNSA = requesters.getFirst();
            if (!requesterNSA.getCallbackUrl().equals(replyTo)) {
                log.info("updating callbackUrl for " + nsaId);
                requesterNSA.setCallbackUrl(replyTo);
                requesterNsaRepo.save(requesterNSA);
            }
        }
    }

    public NsiRequesterNSA getRequesterNsa(String nsaId) throws NsiInternalException {
        List<NsiRequesterNSA> requesters = requesterNsaRepo.findByNsaId(nsaId);
        if (requesters.isEmpty()) {
            throw new NsiInternalException("Unknown requester nsa id " + nsaId, NsiErrors.SEC_ERROR);
        } else if (requesters.size() >= 2) {
            throw new NsiInternalException("multiple requester entries for " + nsaId, NsiErrors.NRM_ERROR);
        } else {
            return requesters.getFirst();
        }
    }

    public void makeResponseHeader(CommonHeaderType inHeader) {
        inHeader.getAny().clear();
        inHeader.setReplyTo("");
    }

    public Holder<CommonHeaderType> makeClientHeader(String requesterNsaId, String correlationId) {
        CommonHeaderType hd = new CommonHeaderType();
        hd.setRequesterNSA(requesterNsaId);
        hd.setProviderNSA(providerNsa);
        hd.setProtocolVersion(DEFAULT_PROTOCOL_VERSION);
        hd.setCorrelationId(correlationId);
        Holder<CommonHeaderType> header = new Holder<>();
        header.value = hd;

        return header;
    }

    public String newCorrelationId() {
        return "urn:uuid:" + UUID.randomUUID();
    }

}
