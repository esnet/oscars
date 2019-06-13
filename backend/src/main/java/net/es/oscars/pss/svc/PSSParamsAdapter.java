package net.es.oscars.pss.svc;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.PSSException;
import net.es.oscars.app.props.PssProperties;
import net.es.oscars.dto.pss.cmd.Command;
import net.es.oscars.dto.pss.cmd.CommandType;
import net.es.oscars.pss.params.alu.AluParams;
import net.es.oscars.pss.params.mx.MxParams;
import net.es.oscars.pss.ent.RouterCommands;
import net.es.oscars.pss.equip.AluParamsAdapter;
import net.es.oscars.pss.equip.MxParamsAdapter;
import net.es.oscars.resv.ent.*;
import net.es.oscars.topo.beans.TopoUrn;
import net.es.oscars.topo.enums.UrnType;
import net.es.oscars.topo.svc.TopoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class PSSParamsAdapter {

    private TopoService topoService;
    private PssProperties pssProperties;
    private AluParamsAdapter aluParamsAdapter;
    private MxParamsAdapter mxParamsAdapter;

    @Autowired
    public PSSParamsAdapter(TopoService topoService,
                            MxParamsAdapter mxParamsAdapter,
                            AluParamsAdapter aluParamsAdapter,
                            PssProperties pssProperties) {
        this.aluParamsAdapter = aluParamsAdapter;
        this.mxParamsAdapter = mxParamsAdapter;
        this.topoService = topoService;
        this.pssProperties = pssProperties;
    }

    public Command command(CommandType type, Connection c, VlanJunction j, RouterCommands existing) throws PSSException {
        log.info("making command for "+j.getDeviceUrn());

        Command cmd = makeCmd(c.getConnectionId(), type, j.getDeviceUrn());
        if (existing != null) {
            return cmd;
        }

        switch (cmd.getModel()) {
            case ALCATEL_SR7750:
                AluParams aluParams = aluParamsAdapter.params(c, j);
                cmd.setAlu(aluParams);
                break;
            case JUNIPER_EX:
                break;
            case JUNIPER_MX:
                MxParams mxParams = mxParamsAdapter.params(c, j);
                cmd.setMx(mxParams);
                break;
        }
        return cmd;
    }

    private Command makeCmd(String connId, CommandType type, String device) throws PSSException {
        TopoUrn devUrn = topoService.getTopoUrnMap().get(device);
        if (devUrn == null) {
            throw new PSSException("could not locate topo URN for "+device);

        }
        if (!devUrn.getUrnType().equals(UrnType.DEVICE)) {
            throw new PSSException("bad urn type");
        }

        return Command.builder()
                .connectionId(connId)
                .type(type)
                .model(devUrn.getDevice().getModel())
                .device(devUrn.getUrn())
                .profile(pssProperties.getProfile())
                .build();
    }

}