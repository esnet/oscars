package net.es.oscars.sb.rancid;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.PssProperties;
import net.es.oscars.dto.pss.cmd.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.RestTemplateBuilder;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
@ConditionalOnProperty(name="pss.server-type", havingValue = "rest")
public class RestRancidServer implements RancidProxy {
    private final PssProperties props;
    private RestTemplate restTemplate;
    @Autowired
    public RestRancidServer(PssProperties props, RestTemplateBuilder restTemplateBuilder, SslBundles sslBundles) {

        this.props = props;
        try {
            this.restTemplate = restTemplateBuilder
                    .setSslBundle(sslBundles.getBundle("pss"))
                    .basicAuthentication(props.getUsername(), props.getPassword())
                    .build();

        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        log.info("PSS server URL: " + props.getUrl());
    }


    public CommandResponse submitCommand(Command cmd) {
        if (cmd.getType().equals(CommandType.CONTROL_PLANE_STATUS)) {
            log.info("submit command - device " + cmd.getDevice());

        } else {
            log.info("submit command - conn id: " + cmd.getConnectionId() + " , dev: " + cmd.getDevice());

        }
        String pssUrl = props.getUrl();
        String submitUrl = "/command";
        String restPath = pssUrl + submitUrl;
        return restTemplate.postForObject(restPath, cmd, CommandResponse.class);

    }

    public DeviceConfigResponse getConfig(DeviceConfigRequest request) {
        log.info("getConfig - device " + request.getDevice());
        String pssUrl = props.getUrl();
        String submitUrl = "/getConfig";
        String restPath = pssUrl + submitUrl;
        return restTemplate.postForObject(restPath, request, DeviceConfigResponse.class);
    }


    public CommandStatus status(String commandId) {
        log.info("status - cmd id " + commandId);
        String pssUrl = props.getUrl();
        String submitUrl = "/status/" + commandId;
        String restPath = pssUrl + submitUrl;
        return restTemplate.getForObject(restPath, CommandStatus.class);
    }

}