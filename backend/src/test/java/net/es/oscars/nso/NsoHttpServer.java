
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
import net.es.topo.common.dto.nso.FromNsoCheckSync;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.NsoVPLS;
import net.es.topo.common.dto.nso.YangPatchWrapper;
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

    public static FromNsoCheckSync nsoCheckSyncResult;

    public static int statusCode;

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
                "/restconf/data/tailf-ncs:devices/*",
                "/restconf/data/",

                "/esdb_api/graphql",
                "/esdb_api/v1/*"
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
                } else if (uri.startsWith("/esdb_api/graphql")) {
                    loadEsdbGraphqlMockData(req, resp);
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

        private void loadEsdbGraphqlMockData(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            log.info("ESNet GraphQL mock URI requested (" + req.getMethod() + "): " + req.getRequestURI());
            EsdbGraphqlResponseSpec[] esdbGraphqlResponseSpecs = new ObjectMapper()
                .readValue(
                    new ClassPathResource("http/response-specs/esdb.graphql.response-specs.json").getFile(),
                    EsdbGraphqlResponseSpec[].class
                );
            for (EsdbGraphqlResponseSpec spec : esdbGraphqlResponseSpecs) {
                if (spec.method.equals(req.getMethod())) {
                    InputStream stream = new ClassPathResource(spec.data).getInputStream();
                    String body = StreamUtils.copyToString(stream, Charset.defaultCharset());
                    resp.getWriter().write(body);
                    resp.setContentType("application/json");
                    resp.setStatus(spec.status);
                    resp.getWriter().flush();
                    log.info("ESNet GraphQL mock response: " + body);
                    return;
                }
            }
            // No response found? HTTP 404
            log.info("ESNet GraphQL mock response could not be found in response specs.");
        }

        private void loadEsnetVplsMockData(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // No request payload required.
            // Set the expected response content type
            // NOTE: We MUST also be careful that the mock data provided is actually formatted as the set Content Type!
            // Otherwise, we will write the data string, but RestTemplate.getForObject() will throw an exception anyway.

            NsoEsnetVplsResponseSpec[] vplsResponseSpecs = new ObjectMapper()
                .readValue(
                        new ClassPathResource("http/nso/vpls/nso.esnet-vpls.response-specs.json").getFile(),
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
                        new ClassPathResource("http/response-specs/nso.esnet-lsp.response-specs.json").getFile(),
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
//            log.info("patch request received. request payload is:\n" + payload);
            YangPatchWrapper patch = new ObjectMapper().readValue(payload.toString(), YangPatchWrapper.class);

            response.setContentType("application/yang-data+json");

            if (
                patch.getPatch().getPatchId().startsWith("delete VPLS")
                || patch.getPatch().getPatchId().startsWith("redeploy VPLS")
            ) {
                NsoEsnetVplsYangPatchDeleteResponseSpec[] vplsResponseSpecs = new ObjectMapper()
                    .readValue(
                        new ClassPathResource("http/response-specs/nso.esnet-vpls.sync.delete.response-specs.json").getFile(),
                        NsoEsnetVplsYangPatchDeleteResponseSpec[].class
                    );

                for( NsoEsnetVplsYangPatchDeleteResponseSpec responseSpec : vplsResponseSpecs) {
                    log.info("Comparing mock HTTP Patch response entry for VPLS Patch ID " + responseSpec.patchId());
                    if (responseSpec.patchId().equals( patch.getPatch().getPatchId() )) {
                        // read in the body from the file found in the path in the responseSpec...
                        InputStream bodyInputStream = new ClassPathResource(responseSpec.data).getInputStream();
                        String body = StreamUtils.copyToString(bodyInputStream, Charset.defaultCharset());
                        // ... then write it out as our response
                        response.getWriter().write(body);
                        response.setStatus(responseSpec.status);
                        log.info("Found mock HTTP Patch response entry for VPLS Patch ID " + responseSpec.patchId());
                        return;
                    }
                }
                log.warn("HTTP PATCH request received, but no mock HTTP PATCH response found for patch ID '{}' in http/nso.esnet-vpls.sync.delete.response-specs.json", patch.getPatch().getPatchId());
            } else if (
                patch.getPatch().getPatchId().startsWith("delete LSP")
                || patch.getPatch().getPatchId().startsWith("replace LSP")
            ) {
                NsoEsnetLspYangPatchDeleteResponseSpec[] lspResponseSpecs = new ObjectMapper()
                    .readValue(
                        new ClassPathResource("http/response-specs/nso.esnet-lsp.sync.delete.response-specs.json").getFile(),
                        NsoEsnetLspYangPatchDeleteResponseSpec[].class
                    );
                for ( NsoEsnetLspYangPatchDeleteResponseSpec responseSpec : lspResponseSpecs) {
                    log.info("Comparing mock HTTP Patch response entry for LSP Patch ID " + responseSpec.patchId());
                    if (responseSpec.patchId().equals( patch.getPatch().getPatchId() )) {
                        InputStream bodyInputStream = new ClassPathResource(responseSpec.data).getInputStream();
                        String body = StreamUtils.copyToString(bodyInputStream, Charset.defaultCharset());
                        // ... then write it out as our response
                        response.getWriter().write(body);
                        response.setStatus(responseSpec.status);
                        log.info("Found mock HTTP Patch response entry for LSP Patch ID " + responseSpec.patchId());
                        return;
                    }
                }
                log.warn("HTTP PATCH request received, but no mock HTTP PATCH response found for patch ID '{}' in http/nso.esnet-lsp.sync.delete.response-specs.json", patch.getPatch().getPatchId());
            }

            // @TODO return 404 if we get this far

            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("404 Not Found");
            response.getWriter().flush();
        }

        @Override
        protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            log.info("HTTP DELETE request received, mocking " + req.getRequestURI());
            resp.setStatus(HttpServletResponse.SC_OK);
//            resp.getWriter().write("OK");
            resp.getWriter().flush();
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            String uri = request.getRequestURI();
            log.info("POST request received, mocking " + uri + " with query: " + request.getQueryString());
            if (uri.startsWith("/restconf/data/esnet-status:esnet-status/nokia-show")) {
                mockPostNokiaShow(request, response);
            } else if (uri.startsWith("/restconf/data/tailf-ncs:services")) {
                mockPostTailfNcs(request, response);
            } else if (uri.startsWith("/esdb_api/graphql")) {
                loadEsdbGraphqlMockData(request, response);

            } else if (uri.startsWith("/restconf/data/tailf-ncs:devices/device")) {
                if (uri.endsWith("check-sync")) {
                    log.info("status code: "+NsoHttpServer.statusCode);
                    response.setStatus(NsoHttpServer.statusCode);
                    response.setContentType("application/yang-data+json");

                    if (NsoHttpServer.nsoCheckSyncResult != null) {
                        log.info("writing body");
                        response.getWriter().write(new ObjectMapper().writeValueAsString(NsoHttpServer.nsoCheckSyncResult));
                    }
                    response.getWriter().write("");
                    response.getWriter().flush();
                }
            } else {
                // Unknown.
                log.info("POST request not handled yet " + uri + " with query: " + request.getQueryString());
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
            log.debug("HTTP POST request payload\n" + payload);
            // consume the incoming NsoServiceWrapper
            NsoServicesWrapper nsoServicesWrapper = new ObjectMapper().readValue(payload.toString(), NsoServicesWrapper.class);

            response.setContentType("application/yang-data+json");

            // Is this for VPLS or LSP ?
            if (nsoServicesWrapper.getVplsInstances() != null && !nsoServicesWrapper.getVplsInstances().isEmpty()) {
                NsoEsnetVplsYangPatchResponseSpec[] responseSpecs = new ObjectMapper()
                        .readValue(new ClassPathResource("http/response-specs/nso.esnet-vpls.sync.response-specs.json").getFile(), NsoEsnetVplsYangPatchResponseSpec[].class);

                for (NsoEsnetVplsYangPatchResponseSpec responseSpec : responseSpecs) {
                    // check by connectionId (VPLS name) and vc-id
                    int vcId = responseSpec.vcId;
                    String connectionId = responseSpec.connectionId;

                    for (NsoVPLS vpls : nsoServicesWrapper.getVplsInstances()) {
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
                log.warn("Did not find a corresponding mock POST response (VPLS endpoint) in http/nso.esnet-vpls.sync.response-specs.json");
            } else if (nsoServicesWrapper.getLspInstances() != null && !nsoServicesWrapper.getLspInstances().isEmpty()) {
                NsoEsnetLspYangPatchResponseSpec[] responseSpecs = new ObjectMapper()
                        .readValue(new ClassPathResource("http/response-specs/nso.esnet-lsp.post.response-specs.json").getFile(), NsoEsnetLspYangPatchResponseSpec[].class);


                for (NsoLSP lsp : nsoServicesWrapper.getLspInstances()) {
                    for (NsoEsnetLspYangPatchResponseSpec responseSpec : responseSpecs) {
                        // check by connectionId (VPLS name)
                        String lspName = responseSpec.lspName;
                        String lspDevice = responseSpec.lspDevice;
                        log.info("Checking mock POST response LSP with name " + lspName + " and device " + lspDevice);
                        // Currently only sending one VPLS per HTTP POST. :-/
                        // We only need to find the one VPLS in the list of VPLS instances
                        log.info("...Does it equal our request LSP with name " + lsp.getName() + " and device " + lsp.getDevice() + "?");
                        if (lsp.getName().equals(lspName) && lsp.getDevice().equals(lspDevice)) {
                            // read in the body from the file found in the path in the responseSpec...
                            InputStream bodyInputStream = new ClassPathResource(responseSpec.data).getInputStream();
                            String body = StreamUtils.copyToString(bodyInputStream, Charset.defaultCharset());
                            // ... then write it out as our response
                            response.getWriter().write(body);
                            response.setStatus(responseSpec.status);
                            log.info("Yes. Found mock POST response for LSP " + lspName + " and device " + lspDevice);
                            return;
                        }
                    }
                    log.warn("Did not find a corresponding mock POST response for LSP instance key {} (LSP endpoint) in http/nso.esnet-lsp.post.response-specs.json", lsp.instanceKey());
                }
            }

            // If we got this far, it means the requested mock payload was "404 Not found"... (we don't have it listed)
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//            response.getWriter().write("404 Not Found");
            response.getWriter().flush();
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
                    .readValue(new ClassPathResource("http/nso/status/response-specs.json").getFile(), ResponseSpec[].class);

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
    public record NsoEsnetLspYangPatchResponseSpec(String lspName, String lspDevice, String data, Integer status) {}
    public record NsoEsnetLspYangPatchDeleteResponseSpec(String patchId, String data, Integer status) {}

    public record EsdbGraphqlResponseSpec(String method, String data, Integer status) {}
}