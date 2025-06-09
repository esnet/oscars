package net.es.oscars.web.graphql;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.svc.ConnService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class ConnListController {
    private final ConnService connSvc;

    public ConnListController(ConnService connSvc) {
        this.connSvc = connSvc;
    }

    @SchemaMapping(typeName="OscarsQuery")
    public Connection ConnectionById(@Argument String connectionId) {
        return connSvc.findConnection(connectionId).orElseThrow();
    }

}
