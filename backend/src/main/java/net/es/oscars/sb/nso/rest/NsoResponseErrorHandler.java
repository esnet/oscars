package net.es.oscars.sb.nso.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.nio.charset.Charset;


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
            log.error("server error status text: {}", httpResponse.getStatusText());
        } else if (httpResponse.getStatusCode().is4xxClientError()) {
            log.error("client error status text: {}", httpResponse.getStatusText());
        }
        // if we actually read the error body out the InputStream closes, so don't do the next line
        // String body = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
        // log.error("error body: \n" +body);

    }
}
