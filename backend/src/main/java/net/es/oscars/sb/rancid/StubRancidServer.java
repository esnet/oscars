package net.es.oscars.sb.rancid;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.dto.pss.cmd.*;
import net.es.oscars.dto.pss.st.ConfigStatus;
import net.es.oscars.dto.pss.st.LifecycleStatus;
import net.es.oscars.dto.pss.st.OperationalStatus;
import net.es.oscars.sb.db.RouterCommandsRepository;
import net.es.oscars.sb.ent.RouterCommands;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;


@Component
@Slf4j
@ConditionalOnExpression("${pss.server-type:stub}")
public class StubRancidServer implements RancidProxy {
    private Map<String, CommandStatus> statusMap = new HashMap<>();
    private Hashids hashids = new Hashids("ESnet salt");

    @Autowired
    private RouterCommandsRepository rcRepo;

    public CommandResponse submitCommand(Command cmd) {
        Integer id = new Random().nextInt(0, 1000000);

        String commandsString = "";
        for (RouterCommands rc : rcRepo.findByConnectionIdAndDeviceUrn(cmd.getConnectionId(), cmd.getDevice())) {
            if (rc.getType().equals(cmd.getType())) {
                commandsString = rc.getContents();
            }
        }

        String commandId = hashids.encode(id);
        CommandStatus cs = CommandStatus.builder()
                .type(cmd.getType())
                .device(cmd.getDevice())
                .profile(cmd.getProfile())
                .connectionId(cmd.getConnectionId())
                .output("stub output - commands were: \n"+commandsString)
                .commands(commandsString)
                .configStatus(ConfigStatus.OK)
                .lifecycleStatus(LifecycleStatus.DONE)
                .operationalStatus(OperationalStatus.OK)
                .build();
        statusMap.put(commandId, cs);
        return CommandResponse.builder()
                .commandId(commandId)
                .device(cmd.getDevice())
                .connectionId(cmd.getConnectionId())
                .build();
    }

    public DeviceConfigResponse getConfig(DeviceConfigRequest request) {
        return DeviceConfigResponse.builder().build();
    }


    public CommandStatus status(String commandId) {
        return this.statusMap.get(commandId);
    }

}