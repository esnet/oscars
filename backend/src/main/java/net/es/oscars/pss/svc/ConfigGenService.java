package net.es.oscars.pss.svc;

import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PSSException;
import net.es.oscars.app.props.PssProperties;
import net.es.oscars.dto.pss.cmd.*;
import net.es.oscars.pss.beans.TemplateOutput;
import net.es.oscars.pss.beans.TemplateVersionReport;
import net.es.oscars.pss.params.alu.AluParams;
import net.es.oscars.pss.params.mx.MxParams;
import net.es.oscars.pss.db.RouterCommandsRepository;
import net.es.oscars.pss.ent.RouterCommands;
import net.es.oscars.pss.equip.*;
import net.es.oscars.pss.tpl.Stringifier;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.ent.VlanJunction;
import net.es.oscars.topo.ent.Device;
import net.es.oscars.topo.svc.TopoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
@Slf4j
public class ConfigGenService {
    @Autowired
    private RouterCommandsRepository rcr;
    @Autowired
    private AluCommandGenerator acg;
    @Autowired
    private AluParamsAdapter apa;
    @Autowired
    private MxCommandGenerator mcg;
    @Autowired
    private MxParamsAdapter mpa;

    @Autowired
    private ExCommandGenerator ecg;

    @Autowired
    private TopoService topoSvc;

    @Autowired
    private Stringifier stringifier;

    public void generateConfig(Connection conn) throws PSSException {
        /*
        try {
            log.info("sleeping a bit to simulate slow config gen");
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        */

        log.info("generating config for " + conn.getConnectionId());
        for (VlanJunction j : conn.getReserved().getCmp().getJunctions()) {
            Device d = topoSvc.getDeviceRepo().findByUrn(j.getDeviceUrn()).orElseThrow(PSSException::new);
            String build = null;
            String dismantle = null;
            String opStatus = null;
            String templateVersion = null;
            switch (d.getModel()) {
                case ALCATEL_SR7750:
                    AluParams aluParams = apa.params(conn, j);
                    build = acg.build(aluParams);
                    dismantle = acg.dismantle(aluParams);
                    opStatus = acg.show(aluParams);
                    templateVersion = this.consistentVersion(acg.getTemplateFilenames());
                    break;
                case JUNIPER_MX:
                    MxParams mxParams = mpa.params(conn, j);
                    build = mcg.build(mxParams);
                    dismantle = mcg.dismantle(mxParams);
                    opStatus = mcg.show(mxParams);
                    templateVersion = this.consistentVersion(mcg.getTemplateFilenames());
                    break;
                case JUNIPER_EX:
                    break;
            }
            RouterCommands rcb = RouterCommands.builder()
                    .connectionId(conn.getConnectionId())
                    .deviceUrn(j.getDeviceUrn())
                    .contents(build)
                    .templateVersion(templateVersion)
                    .type(CommandType.BUILD)
                    .build();
            rcr.save(rcb);
            RouterCommands rcd = RouterCommands.builder()
                    .connectionId(conn.getConnectionId())
                    .deviceUrn(j.getDeviceUrn())
                    .contents(dismantle)
                    .templateVersion(templateVersion)
                    .type(CommandType.DISMANTLE)
                    .build();
            rcr.save(rcd);
            RouterCommands rcs = RouterCommands.builder()
                    .connectionId(conn.getConnectionId())
                    .deviceUrn(j.getDeviceUrn())
                    .contents(opStatus)
                    .templateVersion(templateVersion)
                    .type(CommandType.OPERATIONAL_STATUS)
                    .build();
            rcr.save(rcs);
        }

        log.info("generated config for " + conn.getConnectionId());
    }

    public String consistentVersion(List<String> templateFilenames) throws PSSException {
        List<TemplateVersionReport> tvrs = this.versionReport(templateFilenames);
        boolean allHaveVersions = true;
        boolean versionsConsistent = true;
        String consistentVersion = null;
        for (TemplateVersionReport tvr : tvrs) {
            if (!tvr.getHasVersion()) {
                allHaveVersions = false;
            } else {
                if (consistentVersion == null) {
                    consistentVersion = tvr.getTemplateVersion();
                } else if (!consistentVersion.equals(tvr.getTemplateVersion())) {
                    versionsConsistent = false;
                }
            }
        }
        if (!allHaveVersions) {
            throw new PSSException("Not all templates have @version tags");
        } else if (!versionsConsistent) {
            throw new PSSException("Template @version tags are inconsistent");
        }
        return consistentVersion;
    }

    public List<TemplateVersionReport> versionReport(List<String> templateFilenames) throws PSSException {
        List<TemplateVersionReport> result = new ArrayList<>();
        for (String tfn : templateFilenames) {
            try {
                TemplateOutput to = this.stringifier.stringify(null, tfn);
                TemplateVersionReport tvr = TemplateVersionReport.builder()
                        .hasVersion(to.getHasVersion())
                        .templateFilename(tfn)
                        .templateVersion(to.getTemplateVersion())
                        .build();
                result.add(tvr);

            } catch (IOException | TemplateException ex) {
                log.error(ex.getMessage(), ex);
                throw new PSSException("template handling exception!");
            }
        }
        return result;
    }


}