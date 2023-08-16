package net.es.oscars.pss.nso;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.nso.db.NsoServiceDAO;
import net.es.oscars.nso.ent.NsoService;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.NsoVPLS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Component
@Slf4j
public class NsoTicker {
    @Autowired
    private NsoServiceDAO nsoDao;

//    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void insertAService() {
        NsoVPLS vpls = NsoVPLS.builder()
                .description("something")
                .build();

        NsoLSP nsoLSP = NsoLSP.builder()
                .device("a device")
                .build();
        List<NsoLSP> lsps = new ArrayList<>();
        lsps.add(nsoLSP);

        NsoService nsoService = NsoService.builder()
                .connectionId("ABCD")
                .vpls(vpls)
                .lsp(lsps)
                .updated(Instant.now())
                .build();
        nsoDao.save(nsoService);


    }
}
