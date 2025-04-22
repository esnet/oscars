
package net.es.oscars.nso;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.sb.nso.rest.LiveStatusRequest;
import net.es.oscars.sb.nso.rest.NsoServicesWrapper;
import net.es.topo.common.dto.nso.NsoVPLS;
import net.es.topo.common.dto.nso.YangPatch;
import net.es.topo.common.dto.nso.YangPatchWrapper;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

@Component
@Slf4j
public class NsoHttpServer {

    @Value(value = "${nso.mockPort}")
    private int port;

    @Bean
    public ServletRegistrationBean<Servlet> servletRegistrationBean(){

        NsoServlet servlet = new NsoServlet();

        return new ServletRegistrationBean<>(
                servlet,
                "/restconf/data/esnet-status:esnet-status/nokia-show",
                "/restconf/data/tailf-ncs:services/esnet-vpls:vpls",
                "/restconf/data/tailf-ncs:services/esnet-lsp:lsp",
                "/restconf/data/tailf-ncs:services",
                "/restconf/data/"
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
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String method = req.getMethod();
            log.info("NsoServlet HTTP method: " + method);
            if (method.equals("PATCH")) {
                this.customDoPatch(req, resp);
            } else {
                super.service(req, resp);
            }
        }
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Is this for an nso.esnet-vpls mock request?
            String uri = req.getRequestURI();
            try {
                if (uri.startsWith("/restconf/data/tailf-ncs:services/esnet-vpls:vpls")) {
                    loadEsnetVplsMockData(req, resp);
                } else if (uri.startsWith("/restconf/data/tailf-ncs:services/esnet-lsp:lsp")) {
                    loadEsnetLspMockData(req, resp);
                } else {
                    // Unknown.
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("404 Not Found");
                    resp.getWriter().flush();
                }
            } catch (Exception ex) {
                log.error("Failed to load ESNet mock data", ex);
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

        private void loadEsnetLspMockData(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            NsoEsnetLspResponseSpec[] lspResponseSpecs = new ObjectMapper()
                .readValue(
                        new ClassPathResource("http/nso.esnet-lsp.response-specs.json").getFile(),
                        NsoEsnetLspResponseSpec[].class
                );

            for (NsoEsnetLspResponseSpec responseSpec : lspResponseSpecs ) {
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

        protected void customDoPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String uri = req.getRequestURI();
            log.info("PATCH request received, mocking " + uri);
            if (uri.startsWith("/restconf/data/")) {
                mockPatch(req, resp);
            } else {
                // Unknown.
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("404 Not Found");
                resp.getWriter().flush();
            }
        }

        protected void mockPatch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            StringBuilder payload = new StringBuilder();

            try(BufferedReader reader = request.getReader()){
                String line;
                while ((line = reader.readLine()) != null){
                    payload.append(line);
                }
            }
            YangPatchWrapper patch = new ObjectMapper().readValue(payload.toString(), YangPatchWrapper.class);

            NsoEsnetVplsYangPatchDeleteResponseSpec[] vplsResponseSpecs = new ObjectMapper()
                    .readValue(
                            new ClassPathResource("http/nso.esnet-vpls.sync.delete.response-specs.json").getFile(),
                            NsoEsnetVplsYangPatchDeleteResponseSpec[].class
                    );


            log.info("patch request received, with payload:\n" + patch.toString());
            response.setContentType("application/yang-data+json");

            for( NsoEsnetVplsYangPatchDeleteResponseSpec responseSpec : vplsResponseSpecs) {
                if (responseSpec.patchId().equals( patch.getPatch().getPatchId() )) {
                    // read in the body from the file found in the path in the responseSpec...
                    InputStream bodyInputStream = new ClassPathResource(responseSpec.data).getInputStream();
                    String body = StreamUtils.copyToString(bodyInputStream, Charset.defaultCharset());
                    // ... then write it out as our response
                    response.getWriter().write(body);
                    response.setStatus(HttpServletResponse.SC_OK);

                    return;
                }
            }

            // @TODO return 404 if we get this far
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            String uri = request.getRequestURI();
            log.info("POST request received, mocking " + uri);
            if (uri.startsWith("/restconf/data/esnet-status:esnet-status/nokia-show")) {
                mockPostNokiaShow(request, response);
            } else if (uri.startsWith("/restconf/data/tailf-ncs:services")) {
                mockPostTailfNcs(request, response);
            } else {
                // Unknown.
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("404 Not Found");
                response.getWriter().flush();
            }
        }

        protected void mockPostTailfNcs(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            StringBuilder payload = new StringBuilder();

            try(BufferedReader reader = request.getReader()){
                String line;
                while ((line = reader.readLine()) != null){
                    payload.append(line);
                }
            }
            // consume the incoming NsoServiceWrapper
            NsoServicesWrapper nsoServicesWrapper = new ObjectMapper().readValue(payload.toString(), NsoServicesWrapper.class);

            response.setContentType("application/yang-data+json");
            NsoEsnetVplsYangPatchResponseSpec[] responseSpecs = new ObjectMapper()
                .readValue(new ClassPathResource("http/nso.esnet-vpls.sync.response-specs.json").getFile(), NsoEsnetVplsYangPatchResponseSpec[].class);

            for (NsoEsnetVplsYangPatchResponseSpec responseSpec : responseSpecs) {
                // check by connectionId (VPLS name) and vc-id
                int vcId = responseSpec.vcId;
                String connectionId = responseSpec.connectionId;

                for (NsoVPLS vpls : nsoServicesWrapper.getVplsInstances())
                {
                    // Currently only sending one VPLS per HTTP POST. :-/
                    // We only need to find the one VPLS in the list of VPLS instances
                    if (vpls.getVcId() == vcId && vpls.getName().equals(connectionId)) {
                        // read in the body from the file found in the path in the responseSpec...
                        InputStream bodyInputStream = new ClassPathResource(responseSpec.data).getInputStream();
                        String body = StreamUtils.copyToString(bodyInputStream, Charset.defaultCharset());
                        // ... then write it out as our response
                        response.getWriter().write(body);
                        response.setStatus(responseSpec.status);
                        return;
                    }
                }
            }

            // If we got this far, it means the requested mock payload was "404 Not found"... (we don't have it listed)
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }

        protected void mockPostNokiaShow(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
                    break;
                }
            }
        }
    }

    public record ResponseSpec(String device, String args, String body, Integer status) {}
    public record NsoEsnetVplsResponseSpec(String data, Integer status) {}
    public record NsoEsnetVplsYangPatchResponseSpec(String connectionId, Integer vcId, String data, Integer status) {}
    public record NsoEsnetVplsYangPatchDeleteResponseSpec(String patchId, String data, Integer status) {}

    public record NsoEsnetLspResponseSpec(String data, Integer status) {}
}