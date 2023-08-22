package net.es.oscars.nso.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;


@Slf4j
public class NsoResponseErrorHandler implements ResponseErrorHandler {
    @Override
    public boolean hasError(ClientHttpResponse httpResponse)
            throws IOException {

        return (httpResponse.getStatusCode().is4xxClientError()  || httpResponse.getStatusCode().is5xxServerError());
    }

    @Override
    public void handleError(ClientHttpResponse httpResponse) throws IOException {
        // just log stuff
        if (httpResponse.getStatusCode().is5xxServerError()) {
            log.error("server error: " +httpResponse.getStatusText());
        } else if (httpResponse.getStatusCode().is4xxClientError()) {
            log.error("client error: " +httpResponse.getStatusText());
        }
    }
}
