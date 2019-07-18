package net.es.oscars.pss.svc;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.dto.pss.cmd.Command;
import net.es.oscars.dto.pss.cmd.CommandStatus;
import net.es.oscars.dto.pss.st.*;
import net.es.oscars.dto.topo.DeviceModel;
import net.es.oscars.pss.beans.*;
import net.es.oscars.pss.rancid.RancidArguments;
import net.es.oscars.pss.rancid.RancidResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class CommandRunner {
    private RouterConfigBuilder builder;
    private RancidRunner rancidRunner;
    private HealthService healthService;

    @Autowired
    public CommandRunner(RancidRunner rancidRunner, RouterConfigBuilder builder, HealthService healthService) {
        this.rancidRunner = rancidRunner;
        this.builder = builder;
        this.healthService = healthService;
    }

    public void run(CommandStatus status, Command command) {
        ConfigResult confRes;
        RancidArguments args;
        try {

            switch (command.getType()) {
                case CONFIG_STATUS:
                    break;
                case OPERATIONAL_STATUS:
                    break;
                case CONTROL_PLANE_STATUS:
                    ControlPlaneResult res = cplStatus(command.getDevice(), command.getModel(), command.getProfile());
                    status.setControlPlaneStatus(res.getStatus());
                    break;
                case BUILD:
                    status.setConfigStatus(ConfigStatus.NONE);
                    args = builder.build(command);
                    confRes = configure(args, command.getProfile());
                    status.setOutput(confRes.getOutput());
                    status.setConfigStatus(confRes.getStatus());
                    status.setCommands(confRes.getCommands());
                    break;
                case DISMANTLE:
                    status.setConfigStatus(ConfigStatus.NONE);
                    args = builder.dismantle(command);
                    confRes = configure(args, command.getProfile());
                    status.setConfigStatus(confRes.getStatus());
                    status.setOutput(confRes.getOutput());
                    status.setCommands(confRes.getCommands());
                    break;

            }
        } catch (UrnMappingException | ConfigException ex) {
            log.error("error", ex);
            status.setControlPlaneStatus(ControlPlaneStatus.ERROR);
        }
    }

    private ConfigResult configure(RancidArguments args, String profile) {

        ConfigResult result = ConfigResult.builder().build();

        try {
            RancidResult rr = rancidRunner.runRancid(args, profile);
            result.setStatus(ConfigStatus.OK);
            result.setOutput(rr.getOutput());
            result.setCommands(args.getRouterConfig());


        } catch (IOException | InterruptedException | TimeoutException | ControlPlaneException ex) {
            log.error("Rancid error", ex);
            result.setStatus(ConfigStatus.ERROR);

        }
        return result;
    }

    private ControlPlaneResult cplStatus(String device, DeviceModel model, String profile)
            throws UrnMappingException {

        ControlPlaneResult result = ControlPlaneResult.builder().build();

        try {
            RancidArguments args = builder.controlPlaneCheck(device, model, profile);
            rancidRunner.runRancid(args, profile);
            healthService.getHealth().getDeviceStatus().put(device, ControlPlaneStatus.OK);
            result.setStatus(ControlPlaneStatus.OK);

        } catch (IOException | InterruptedException | TimeoutException | ControlPlaneException | ConfigException ex) {
            log.error("Rancid error", ex);
            healthService.getHealth().getDeviceStatus().put(device, ControlPlaneStatus.ERROR);
            result.setStatus(ControlPlaneStatus.ERROR);
        }
        return result;

    }


}
