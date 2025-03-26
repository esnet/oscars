
package net.es.oscars.nso;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.sb.nso.dto.NsoVplsResponse;
import net.es.oscars.sb.nso.rest.LiveStatusRequest;
import net.es.topo.common.dto.nso.FromNsoServiceConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

@Component
@Slf4j
public class NsoHttpServer {

    @Value("${nso.mockPort}")
    private int port;

    @Bean
    public ServletRegistrationBean<Servlet> servletRegistrationBean(){
        return new ServletRegistrationBean<>(
                new NsoServlet(),
                "/restconf/data/esnet-status:esnet-status/nokia-show",
                "/data/tailf-ncs:services/esnet-vpls:vpls",
                "/data/tail-ncs:services/esnet-lsp:lsp"
        );
    }

    @Bean
    public JettyServletWebServerFactory jettyCustomizer() {
        JettyServletWebServerFactory jetty = new JettyServletWebServerFactory();
        jetty.addServerCustomizers(new JettyCustomizer(port));
        return jetty;
    }

    static class JettyCustomizer implements JettyServerCustomizer {

        private final int port;

        public JettyCustomizer(int port) {
            this.port = port;
        }

        @Override
        public void customize(Server server) {
            ServerConnector connector = new ServerConnector(server);
            connector.setPort(port);
            server.addConnector(connector);
            try {
                connector.start();
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to start Jetty connector", ex);
            }
        }
    }

    public static class NsoServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Is this for an nso.esnet-vpls mock request?
            String uri = req.getRequestURI();
            try {
                if (uri.startsWith("/data/tailf-ncs:services/esnet-vpls:vpls")) {
                    loadEsnetVplsMockData(req, resp);
                }
            } catch (Exception ex) {
                log.error("Failed to load esnet vpls mock data", ex);
            }
        }

        private void loadEsnetVplsMockData(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // No request payload required.
            // Set the expected response content type
            // NOTE: We MUST also be careful that the mock data provided is actually formatted as the set Content Type!
            // Otherwise, we will write the data string, but RestTemplate.getForObject() will throw an exception anyway.

            NsoEsnetVplsResponseSpec[] vplsResponseSpecs = new ObjectMapper()
                .readValue(
                        new ClassPathResource("http/nso.esnet-vpls.response-specs.json").getFile(),
                        NsoEsnetVplsResponseSpec[].class
                );

            for (NsoEsnetVplsResponseSpec responseSpec : vplsResponseSpecs) {
                // Just load all of them
                InputStream bodyInputStream = new ClassPathResource(responseSpec.data).getInputStream();
                // Read the mock data from the file found in the responseSpec.body path
                String body = StreamUtils.copyToString(bodyInputStream, Charset.defaultCharset());
                // Write the mock data to our response
                resp.getWriter().write(body);

                // Set the mock HTTP status
                resp.setStatus(responseSpec.status);
                resp.setContentType("application/yang-data+json");
                resp.getWriter().flush();

                // We found our entry, break the loop
                break;
            }
        }
        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {

            StringBuilder payload = new StringBuilder();
            try(BufferedReader reader = request.getReader()){
                String line;
                while ((line = reader.readLine()) != null){
                    payload.append(line);
                }
            }
            // consume the incoming LiveStatusRequest
            LiveStatusRequest lsr = new ObjectMapper().readValue(payload.toString(), LiveStatusRequest.class);

            response.setContentType("application/yang-data+json");

            // read in the JSON that specifies how we respond to a particular (device, args) tuple
            ResponseSpec[] responseSpecs = new ObjectMapper()
                    .readValue(new ClassPathResource("http/response-specs.json").getFile(), ResponseSpec[].class);

            // walk through the response specifications
            for (ResponseSpec responseSpec : responseSpecs) {
                // if the spec matches the request...
                if (responseSpec.device.equals(lsr.getDevice()) && responseSpec.args.equals(lsr.getArgs())) {
                    // read in the body from the file found in the path in the responseSpec...
                    InputStream bodyInputStream = new ClassPathResource(responseSpec.body).getInputStream();
                    String body = StreamUtils.copyToString(bodyInputStream, Charset.defaultCharset());
                    // ... then write it out as our response
                    response.getWriter().write(body);

                    response.setStatus(responseSpec.status);
                    return;
                }
            }

        }
    }

    public record ResponseSpec(String device, String args, String body, Integer status) {}
    public record NsoEsnetVplsResponseSpec(String data, Integer status) {}
}